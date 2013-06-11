using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Data.SqlClient;

namespace TwodokuServer
{
    public class DBHelper
    {
        public const string TableGames = "Games";
        public const string TablePlayers = "Players";

        public const string ColumnGameId = "GAMEID";
        public const string ColumnPlayer1 = "PLAYER1";
        public const string ColumnPlayer1Score = "PLAYER1SCORE";
        public const string ColumnPlayer2 = "PLAYER2";
        public const string ColumnPlayer2Score = "PLAYER2SCORE";
        public const string ColumnStartDate = "STARTDATE";
        public const string ColumnPlayDate = "PLAYDATE";
        
        public const string ColumnStatus = "STATUS";
        public const string ColumnTurn = "TURN";
        public const string ColumnHandSystem = "HANDSYSTEM";
        public const string ColumnHandSize = "HANDSIZE";
        public const string ColumnScoringSystem = "SCORINGSYSTEM";
        public const string ColumnMultiplierSystem = "MULTIPLIERSYSTEM";
        public const string ColumnBonusSystem = "BONUSSYSTEM";
        public const string ColumnStartingBoard = "STARTINGBOARD";
        public const string ColumnPlayerBoard = "PLAYERBOARD";
        public const string ColumnMultipliers = "MULTIPLIERS";
        public const string ColumnLastMove = "LASTMOVE";
        public const string ColumnHand = "HAND";

        public const string ColumnPlayerId = "PLAYERID";
        public const string ColumnName = "NAME";
        public const string ColumnPassword = "PASSWORD";
        public const string ColumnGcmId = "GCMID";
        public const string ColumnWins = "WINS";
        public const string ColumnLosses = "LOSSES";
        public const string ColumnStreak = "STREAK";

        public string Server = null;
        public string Database = null;

        private SqlConnection mSQL = null;
        private Random mRandom = new Random();

        public DBHelper(string server, string database)
        {
            Server = server;
            Database = database;
        }

        public string Open()
        {
            string connectionString = String.Format("Data Source={0};Database={1};Integrated Security=True;Connect Timeout=15;User Instance=False;", Server, Database);
            mSQL = new SqlConnection(connectionString);

            try
            {
                mSQL.Open();
                return "Opened DB\r\n\r\n";
            }
            catch (Exception e)
            {
                return "Failed to connect to DB: " + e.ToString() + "\r\n\r\n";
            }
        }

        public string Close()
        {
            if (mSQL != null)
            {
                mSQL.Close();
                mSQL = null;
                return "Closed DB\r\n";
            }

            return "DB not open\r\n";
        }

        public void Reset(string pass)
        {
            if (pass != null && pass.Equals("ERASE IT ALL"))
            {
                SqlDataReader reader = Query(String.Format("truncate table {0}", TableGames));
                reader.Close();
            }
        }

        public bool Insert(string table, string values, string name = null)
        {
            bool check = false;
            string command = String.Format("INSERT into {0} values ({1})", table, values);
            try
            {
                SqlCommand insertCommand = new SqlCommand(command, mSQL);
                insertCommand.ExecuteNonQuery();
                check = true;
            }
            catch (Exception e)
            {
                LogFile.LogError(String.Format("DB Insert failed: {0}\r\n{1}", command, e));
            }

            //string success = check ? "passed" : "failed";
            //string label = name != null ? name : values;
            //return String.Format("Adding to DB {0} {1}: {2}\r\n", table, success, label);
            return check;
        }

        public string Delete(string table, string column, string value, string name = null)
        {
            bool check = false;
            string command = String.Format("DELETE from {0} where {1}={2}", table, column, value);
            try
            {
                SqlCommand deleteCommand = new SqlCommand(command, mSQL);
                deleteCommand.ExecuteNonQuery();
                check = true;
            }
            catch (Exception e)
            {
                LogFile.LogError(String.Format("DB Delete failed: {0}\r\n{1}", command, e));
            }

            string success = check ? "passed" : "failed";
            string label = name != null ? name : value;
            return String.Format("Deleting from DB '{0}' {1}: {2}\r\n", table, success, label);
        }

        public bool Update(string table, string update, string qualifier)
        {
            bool ret = false;
            string command = String.Format("UPDATE {0} SET {1} WHERE {2}", table, update, qualifier);
            try
            {
                SqlCommand updateCommand = new SqlCommand(command, mSQL);
                updateCommand.ExecuteNonQuery();
                ret = true;
            }
            catch
            {
                ret = false;
                LogFile.LogError(String.Format("Error updating database: {0}", command));
            }
            return ret;
        }

        public bool Exists(string table, string colID, string query, string colID2 = null, int query2 = 0)
        {
            bool exists = false;
            try
            {
                string searcher = String.Format("select * from {0} where {1}='{2}'", table, colID, query);
                if (colID2 != null && query2 > 0)
                {
                    searcher += String.Format(" and {0}={1}", colID2, query2);
                }

                SqlDataReader reader = Query(searcher);
                if (reader != null)
                {
                    if (reader.Read())
                    {
                        //string stat = String.Format("Entry already exists in {0} DB for {1}", table, query);
                        //if (colID2 != null && query2 > 0)
                        //{
                        //    stat += String.Format(" - {0}", query2);
                        //}
                        //stat += "\r\n";

                        //AppendLabelText(stat);

                        exists = true;
                    }
                    reader.Close();
                }
            }
            catch (Exception e)
            {
                LogFile.LogError(String.Format("ERROR: DB Exists(5), {0}", e));
            }

            return exists;
        }

        public bool Exists(string table, string colID, int query)
        {
            bool exists = false;

            try
            {
                string searcher = String.Format("select * from {0} where {1}={2}", table, colID, query);

                SqlDataReader reader = Query(searcher);
                if (reader != null)
                {
                    if (reader.Read())
                    {
                        exists = true;
                    }
                    reader.Close();
                }
            }
            catch (Exception e)
            {
                LogFile.LogError(String.Format("ERROR: DB Exists(3), {0}", e));
            }

            return exists;
        }

        public List<string> FindUniqueStrings(string table, string column, string project = null)
        {
            List<string> ret = new List<string>();

            string query = null;
            if (project != null)
            {
                string[] projectParts = project.Split('.');
                project = projectParts[0];
                string[] subprojects = null;
                if (projectParts.Length > 1)
                {
                    subprojects = new string[projectParts.Length - 1];
                    for (int i = 0; i < projectParts.Length - 1; i++)
                        subprojects[i] = projectParts[i + 1];
                }

                query = String.Format("select distinct {0} from {1} where PROJECT='{2}'", column, table, project);

                if (subprojects != null)
                {
                    query += " and ";
                    for (int i = 0; i < subprojects.Length; i++)
                    {
                        if (i > 0)
                            query += " or ";
                        query += String.Format("{0}='{1}'", "SUBPROJECT", subprojects[i]);
                    }
                }
            }
            else
                query = String.Format("select distinct {0} from {1} order by {2}", column, table, column);


            SqlDataReader reader = Query(query);
            if (reader != null)
            {
                while (reader.Read())
                {
                    string val = ((string)reader[0]).Trim();
                    if (!ret.Contains(val))
                        ret.Add(val);
                }
                reader.Close();
            }

            return ret;
        }

        public List<int> FindUniqueInts(string table, string column, string project = null)
        {
            List<int> ret = new List<int>();

            string query = null;
            if (project != null)
            {
                string[] projectParts = project.Split('.');
                project = projectParts[0];
                string[] subprojects = null;
                if (projectParts.Length > 1)
                {
                    subprojects = new string[projectParts.Length - 1];
                    for (int i = 0; i < projectParts.Length - 1; i++)
                        subprojects[i] = projectParts[i + 1];
                }

                query = String.Format("select distinct {0} from {1} where PROJECT='{2}'", column, table, project);

                if (subprojects != null)
                {
                    query += " and ";
                    for (int i = 0; i < subprojects.Length; i++)
                    {
                        if (i > 0)
                            query += " or ";
                        query += String.Format("{0}='{1}'", "SUBPROJECT", subprojects[i]);
                    }
                }
            }
            else
                query = String.Format("select distinct {0} from {1} order by {2}", column, table, column);

            SqlDataReader reader = Query(query);
            if (reader != null)
            {
                while (reader.Read())
                {
                    ret.Add((int)reader[0]);
                }
                reader.Close();
            }

            return ret;
        }

        public List<float> FindUniqueFloats(string table, string column, string project = null)
        {
            List<float> ret = new List<float>();

            string query = null;
            if (project != null)
            {
                string[] projectParts = project.Split('.');
                project = projectParts[0];
                string[] subprojects = null;
                if (projectParts.Length > 1)
                {
                    subprojects = new string[projectParts.Length - 1];
                    for (int i = 0; i < projectParts.Length - 1; i++)
                        subprojects[i] = projectParts[i + 1];
                }

                query = String.Format("select distinct {0} from {1} where PROJECT='{2}'", column, table, project);

                if (subprojects != null)
                {
                    query += " and ";
                    for (int i = 0; i < subprojects.Length; i++)
                    {
                        if (i > 0)
                            query += " or ";
                        query += String.Format("{0}='{1}'", "SUBPROJECT", subprojects[i]);
                    }
                }
            }
            else
                query = String.Format("select distinct {0} from {1} order by {2}", column, table, column);

            SqlDataReader reader = Query(query);
            if (reader != null)
            {
                while (reader.Read())
                {
                    ret.Add((float)(double)reader[0]);
                }
                reader.Close();
            }

            return ret;
        }

        public SqlDataReader Query(string table, string colID, string query)
        {
            return Query(String.Format("select * from {0} where {1}='{2}'", table, colID, query));
        }

        public SqlDataReader Query(string query)
        {
            SqlDataReader rdr = null;

            try
            {
                SqlCommand cmd = new SqlCommand(query, mSQL);
                rdr = cmd.ExecuteReader();
            }
            catch (Exception e)
            {
                LogFile.LogError(String.Format("ERROR: DB Query(1), {0}", e));
            }

            return rdr;
        }


        public int GetNextKey(string table, string keyRow)
        {
            int lastID = 0;
            List<int> ids = FindUniqueInts(table, keyRow);
            foreach (int id in ids)
            {
                if (id > lastID)
                    lastID = id;
            }
            return lastID + 1;
        }



        /////////////////////     Game Helpers       ////////////////////////////////


        public bool AddGame(TwodokuGameInfo gameInfo, Boolean useNow)
        {
            string values = "";
            values += "'" + gameInfo.GameId + "',";
            values += "'" + gameInfo.Player1 + "',";
            values += "'" + gameInfo.Player1Score + "',";
            values += "'" + gameInfo.Player2 + "',";
            values += "'" + gameInfo.Player2Score + "',";

            if (useNow)
            {
                gameInfo.StartDate = DateTime.Now;
                gameInfo.PlayDate = DateTime.Now;
            }
            values += "'" + gameInfo.StartDate + "',";
            values += "'" + gameInfo.PlayDate + "',";
            values += "'" + gameInfo.Status + "',";
            values += "'" + gameInfo.Turn + "',";
            values += "'" + gameInfo.HandSystem + "',";
            values += "'" + gameInfo.HandSize + "',";
            values += "'" + gameInfo.ScoringSystem + "',";
            values += "'" + gameInfo.MultiplierSystem + "',";
            values += "'" + gameInfo.BonusSystem + "',";
            values += "'" + gameInfo.LastMove + "',";
            values += "'" + GetHand() + "',";
            values += "'" + gameInfo.InitialBoard + "',";
            values += "'" + gameInfo.PlayerBoard + "',";
            values += "'" + gameInfo.Multipliers + "'";

            LogFile.LogGameMove(gameInfo);

            return Insert(TableGames, values);
        }

        public bool UpdateGame(TwodokuGameInfo gameInfo)
        {
            //Retrieve the game and make sure everything matches (players, etc.)
            TwodokuGameInfo existingGame = GetGame(gameInfo.GameId);
            if (existingGame == null || !existingGame.IsSameGame(gameInfo))
                return false;

            LogFile.LogGameMove(gameInfo);

            gameInfo.Player1 = existingGame.Player1;
            gameInfo.Player2 = existingGame.Player2;

            //Update: Scores, PlayDate, Status, Turn, PlayerBoard, LastMove
            string update = "";
            update += string.Format("{0}='{1}',", ColumnPlayer1Score, gameInfo.Player1Score);
            update += string.Format("{0}='{1}',", ColumnPlayer2Score, gameInfo.Player2Score);
            update += string.Format("{0}='{1}',", ColumnPlayDate, gameInfo.PlayDate);
            update += string.Format("{0}='{1}',", ColumnStatus, gameInfo.Status);
            update += string.Format("{0}='{1}',", ColumnTurn, gameInfo.Turn);
            update += string.Format("{0}='{1}',", ColumnPlayerBoard, gameInfo.PlayerBoard);
            update += string.Format("{0}='{1}',", ColumnLastMove, gameInfo.LastMove);

            //Create a new hand for the next turn
            string hand = GetHand();
            //Console.WriteLine("Hand: " + hand);
            update += string.Format("{0}='{1}'", ColumnHand, hand);

            string qualifier = string.Format("{0}='{1}'", ColumnGameId, gameInfo.GameId);
            return Update(TableGames, update, qualifier);
        }

        private string GetHand()
        {
            string ret = "";

            for(int i=0; i<50; i++)
            {
                int number = mRandom.Next(1, 10);
                ret += number.ToString();
            }

            return ret;
        }

        public TwodokuGameInfo GetGame(int gameId)
        {
            TwodokuGameInfo gameInfo = null;
            string qualifier = ColumnGameId + "='" + gameId + "'";
            string query = String.Format("select {0} from {1} where {2}", "*", TableGames, qualifier);
            SqlDataReader reader = Query(query);

            if (reader != null)
            {
                reader.Read();
                gameInfo = TwodokuGameInfo.FromSqlReader(reader);
                reader.Close();
            }
            return gameInfo;
        }



        /////////////////////     Game Helpers       ////////////////////////////////


        public bool AddPlayer(TwodokuPlayer player)
        {
            string values = "";
            values += "'" + player.PlayerId + "',";
            values += "'" + player.Name + "',";
            values += "'" + player.Password + "',";
            values += "'" + player.GcmId + "',";
            values += "'" + player.Wins + "',";
            values += "'" + player.Losses + "',";
            values += "'" + player.Streak + "'";

            return Insert(TablePlayers, values);
        }

        public bool UpdatePlayerGcmId(TwodokuPlayer player)
        {
            //Retrieve the player and make sure it exists
            TwodokuPlayer existingPlayer = GetPlayer(player.Name);
            if (existingPlayer == null)
                return false;

            //Update: Scores, PlayDate, Status, Turn, PlayerBoard, LastMove
            string update = "";
            update += string.Format("{0}='{1}'", ColumnGcmId, player.GcmId);

            string qualifier = string.Format("{0}='{1}'", ColumnName, player.Name);
            return Update(TablePlayers, update, qualifier);
        }

        public bool UpdatePlayerStats(string name, bool won)
        {
            TwodokuPlayer player = GetPlayer(name);
            if (player == null)
                return false;
            return UpdatePlayerStats(player, won);
        }

        public bool UpdatePlayerStats(TwodokuPlayer player, bool won)
        {
            if (won)
            {
                player.Wins++;
                if (player.Streak <= 0)
                    player.Streak = 1;
                else
                    player.Streak++;
            }
            else
            {
                player.Losses++;
                if (player.Streak >= 0)
                    player.Streak = -1;
                else
                    player.Streak--;
            }

            //Update: Wins, Losses, and Streak
            string update = "";
            update += string.Format("{0}='{1}',", ColumnWins, player.Wins);
            update += string.Format("{0}='{1}',", ColumnLosses, player.Losses);
            update += string.Format("{0}='{1}'", ColumnStreak, player.Streak);

            string qualifier = string.Format("{0}='{1}'", ColumnName, player.Name);
            return Update(TablePlayers, update, qualifier);
        }

        public bool UpdatePlayerPassword(string name, string newPassword)
        {
            TwodokuPlayer player = GetPlayer(name);
            if (player == null)
                return false;
            return UpdatePlayerPassword(player, newPassword);
        }

        public bool UpdatePlayerPassword(TwodokuPlayer player, string newPassword)
        {
            player.Password = newPassword;

            //Update: Password
            string update = "";
            update += string.Format("{0}='{1}'", ColumnPassword, player.Password);

            string qualifier = string.Format("{0}='{1}'", ColumnName, player.Name);
            return Update(TablePlayers, update, qualifier);
        }

        public TwodokuPlayer GetPlayer(string name)
        {
            TwodokuPlayer player = null;
            string qualifier = ColumnName + "='" + name + "'";
            string query = String.Format("select {0} from {1} where {2}", "*", TablePlayers, qualifier);
            SqlDataReader reader = Query(query);

            if (reader != null)
            {
                if(reader.Read())
                    player = TwodokuPlayer.FromSqlReader(reader);
                reader.Close();
            }
            return player;
        }
    }
}
