using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.IO;
using System.Net.Sockets;
using System.Collections;
using System.Threading;
using System.Net;
using System.Data.SqlClient;

/*
Steps for modifying the DB:
	-Add column to DB definition
	-Add to TwodokuGameInfo.FromHttpPost
	-Add column name to DBHelper constants
	-Add to TwodokuGameInfo.FromSqlReader but comment out
	-Add to TwodokuGameInfo.FromString
	-Add to TwodokuGameInfo.ToPacket
	-Add to TwodokuGameInfo.ToString
	-Add temporary override to set new field to "" in ToString
	-Add to DBHelper.AddGame
	-Recompile executable
	-Run TwodokuServer.exe export
	-Recreate DB
	-Run TwodokuServer.exe import
	-Java: Add new field to SudokuGameTwoPlayer
	-Java: Add to HttpClient.UpdateGameTask
	-Java: Add to SudokuGameTwoPlayer.FromString
	-Java: Add to TwodokuGameTwoPlayer constructor
 */

namespace TwodokuServer
{
    public class TwodokuServer
    {
        private DBHelper mDB = null;
        protected int mPort = 33133;
        bool mActive = true;

        public TwodokuServer(string[] args)
        {
            bool startServer = true;
            
            //Create the database helper and open the database
            mDB = new DBHelper(@".\SQLEXPRESS", @"Twodoku");
            Console.WriteLine(mDB.Open());
            
            //Parse the command line arguments
            if (args.Length > 1)
                Console.WriteLine("Confused by command line arguments, starting up with defaults (too many args)");
            else if (args.Length > 0)
            {
                string[] parts = args[0].Split(':');
                if(parts.Length > 2)
                    Console.WriteLine("Confused by command line arguments, starting up with defaults (too many parts)");
                else if (parts[0].ToLower().Equals("export"))
                {
                    startServer = false;
                    ExportDB();
                }
                else if (parts[0].ToLower().Equals("import"))
                {
                    startServer = false;
                    ImportDB();
                }
                else if(parts[0].ToLower().Equals("port"))
                {
                    int.TryParse(parts[1], out mPort);
                }
                else
                    Console.WriteLine("Confused by command line arguments, starting up with defaults (unknown command)");
            }

            if (startServer)
            {
                Thread thread = new Thread(new ThreadStart(listen));
                thread.Start();
            }
        }

        private void listen()
        {
            Console.WriteLine(string.Format("Server is listening on port {0}", mPort));
            TcpListener listener = new TcpListener(IPAddress.Any, mPort);
            listener.Start();
            while (mActive)
            {
                TcpClient s = listener.AcceptTcpClient();
                HttpProcessor processor = new HttpProcessor(s, mDB);
                Thread thread = new Thread(new ThreadStart(processor.Process));
                thread.Start();
                Thread.Sleep(1);
            }
        }

        private void ExportDB()
        {
            string filename = "games.txt";
            StreamWriter writer = File.CreateText(filename);

            Console.WriteLine("Exporting Games database to " + filename);
            string query = String.Format("select {0} from {1} order by {2}", "*", DBHelper.TableGames, "STARTDATE");
            SqlDataReader reader = mDB.Query(query);

            if (reader != null)
            {
                while (reader.Read())
                {
                    TwodokuGameInfo gameInfo = TwodokuGameInfo.FromSqlReader(reader);
                    string output = gameInfo.ToString();
                    Console.WriteLine("Exporting " + output);
                    writer.WriteLine(output);
                    //outputStream.WriteLine(gameInfo.ToPacket(true));
                }
                reader.Close();
            }
            else
                Console.WriteLine("No games");

            Console.WriteLine("Finished Games");
            writer.Close();

            //Now export the Player table
            filename = "players.txt";
            writer = File.CreateText(filename);

            Console.WriteLine("Exporting Players database to " + filename);
            query = String.Format("select {0} from {1} order by {2}", "*", DBHelper.TablePlayers, "PLAYERID");
            reader = mDB.Query(query);

            if (reader != null)
            {
                while (reader.Read())
                {
                    TwodokuPlayer player = TwodokuPlayer.FromSqlReader(reader);
                    string output = player.ToString();
                    Console.WriteLine("Exporting " + output);
                    writer.WriteLine(output);
                    //outputStream.WriteLine(gameInfo.ToPacket(true));
                }
                reader.Close();
            }
            else
                Console.WriteLine("No players");

            Console.WriteLine("Finished Players");
            writer.Close();
        }

        private void ImportDB()
        {
            string filename = "games.txt";
            StreamReader reader = File.OpenText(filename);
            Console.WriteLine("Importing Games database from " + filename);
            mDB.Reset("ERASE IT ALL");

            string line = reader.ReadLine();
            while (line != null)
            {
                Console.WriteLine("Importing " + line);
                TwodokuGameInfo gameInfo = TwodokuGameInfo.FromString(line);
                mDB.AddGame(gameInfo, false);
                line = reader.ReadLine();
            }

            Console.WriteLine("Finished Games");
            reader.Close();

            //Now import the Players table
            filename = "players.txt";
            reader = File.OpenText(filename);
            Console.WriteLine("Importing Players database from " + filename);

            line = reader.ReadLine();
            while (line != null)
            {
                Console.WriteLine("Importing " + line);
                TwodokuPlayer player = TwodokuPlayer.FromString(line);
                mDB.AddPlayer(player);
                line = reader.ReadLine();
            }

            Console.WriteLine("Finished Players");
            reader.Close();
        }
    }
}
