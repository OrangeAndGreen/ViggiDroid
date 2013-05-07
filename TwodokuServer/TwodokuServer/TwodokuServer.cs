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

namespace TwodokuServer
{
    public class TwodokuServer
    {
        private DBHelper mDB = null;
        protected int mPort;
        bool mActive = true;

        public TwodokuServer(int port)
        {
            mPort = port;
            mDB = new DBHelper(@".\SQLEXPRESS", @"Twodoku");
            Console.WriteLine(mDB.Open());
        }

        public void listen()
        {
            TcpListener listener = new TcpListener(IPAddress.Loopback, mPort);
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
    }



    public class HttpProcessor
    {
        private TcpClient mSocket;
        private DBHelper mDB = null;

        //TODO: Do these all need to be globals?
        public String HttpMethod;
        public String HttpUrl;
        public String HttpProtocolVersionString;
        public Hashtable HttpHeaders = new Hashtable();

        private static int MAX_POST_SIZE = 10 * 1024 * 1024; // 10MB

        public HttpProcessor(TcpClient s, DBHelper db)
        {
            mSocket = s;
            mDB = db;
        }

        private string StreamReadLine(Stream inputStream)
        {
            string data = "";
            while (true)
            {
                int next_char = inputStream.ReadByte();
                if (next_char == '\n')
                    break;
                if (next_char == '\r')
                    continue;
                if (next_char == -1)
                {
                    Thread.Sleep(1);
                    continue;
                }
                data += Convert.ToChar(next_char);
            }
            return data;
        }

        public void Process()
        {
            // we can't use a StreamReader for input, because it buffers up extra data on us inside it's
            // "processed" view of the world, and we want the data raw after the headers
            Stream inputStream = new BufferedStream(mSocket.GetStream());

            // we probably shouldn't be using a streamwriter for all output from handlers either
            StreamWriter outputStream = new StreamWriter(new BufferedStream(mSocket.GetStream()));
            try
            {
                //Parse the request
                String request = StreamReadLine(inputStream);
                string[] tokens = request.Split(' ');
                if (tokens.Length != 3)
                {
                    throw new Exception("invalid http request line");
                }
                HttpMethod = tokens[0].ToUpper();
                HttpUrl = tokens[1];
                HttpProtocolVersionString = tokens[2];

                Console.WriteLine("starting: " + request);


                ReadHeaders(inputStream);
                if (HttpMethod.Equals("GET"))
                {
                    HandleGETRequest(outputStream);
                }
                else if (HttpMethod.Equals("POST"))
                {
                    HandlePOSTRequest(inputStream, outputStream);
                }
            }
            catch (Exception e)
            {
                Console.WriteLine("Exception: " + e.ToString());

                //Write the failure response
                outputStream.WriteLine("HTTP/1.0 404 File not found");
                outputStream.WriteLine("Connection: close");
                outputStream.WriteLine("");
            }
            outputStream.Flush();
            outputStream.Close();
            inputStream = null;
            outputStream = null;
            mSocket.Close();
        }

        public void ReadHeaders(Stream inputStream)
        {
            Console.WriteLine("readHeaders()");
            String line;
            while ((line = StreamReadLine(inputStream)) != null)
            {
                if (line.Equals(""))
                {
                    Console.WriteLine("got headers");
                    return;
                }

                int separator = line.IndexOf(':');
                if (separator == -1)
                {
                    throw new Exception("invalid http header line: " + line);
                }
                String name = line.Substring(0, separator);
                int pos = separator + 1;
                while ((pos < line.Length) && (line[pos] == ' '))
                {
                    pos++; // strip any spaces
                }

                string value = line.Substring(pos, line.Length - pos);
                Console.WriteLine("header: {0}:{1}", name, value);
                HttpHeaders[name] = value;
            }
        }

        public void HandleGETRequest(StreamWriter outputStream)
        {
            Console.WriteLine("request: {0}", HttpUrl);

            //Write the success response
            outputStream.WriteLine("HTTP/1.0 200 OK");
            outputStream.WriteLine("Content-Type: text/html");
            outputStream.WriteLine("Connection: close");
            outputStream.WriteLine("");


            string method = "";
            if (HttpHeaders.ContainsKey("method"))
                method = (string)HttpHeaders["method"];

            string player1 = "";
            if (HttpHeaders.ContainsKey("player1"))
                player1 = (string)HttpHeaders["player1"];
            string player2 = "";
            if (HttpHeaders.ContainsKey("player2"))
                player2 = (string)HttpHeaders["player2"];

            if (method.Equals("Gamelist"))
            {
                string qualifier = DBHelper.ColumnPlayer1 + "='" + player1 + "' OR " + DBHelper.ColumnPlayer2 + "='" + player1 + "'";
                string query = String.Format("select {0} from {1} where {2} order by {3}", "*", DBHelper.TableGames, qualifier, "STARTDATE");
                SqlDataReader reader = mDB.Query(query);

                if (reader != null)
                {
                    while (reader.Read())
                    {
                        DateTime timestamp = (DateTime)reader[DBHelper.ColumnStartDate];
                        string startDate = string.Format("{0}:{1}:{2}:{3}:{4}:{5}", timestamp.Year, timestamp.Month, timestamp.Day, timestamp.Hour, timestamp.Minute, timestamp.Second);
                        timestamp = (DateTime)reader[DBHelper.ColumnPlayDate];
                        string playDate = string.Format("{0}:{1}:{2}:{3}:{4}:{5}", timestamp.Year, timestamp.Month, timestamp.Day, timestamp.Hour, timestamp.Minute, timestamp.Second);

                        outputStream.WriteLine(string.Format("{0},{1},{2},{3},{4},{5},{6},{7}",
                                (int)reader[DBHelper.ColumnGameId],
                                (string)reader[DBHelper.ColumnPlayer1], (int)reader[DBHelper.ColumnPlayer1Score],
                                (string)reader[DBHelper.ColumnPlayer2], (int)reader[DBHelper.ColumnPlayer2Score],
                                startDate, playDate, (int)reader[DBHelper.ColumnTurn]));
                    }
                    reader.Close();
                }
                else
                    outputStream.WriteLine("No games");
            }
            else
            {
                //Sample code for default response
                outputStream.WriteLine("<html><body><h1>test server</h1>");
                outputStream.WriteLine("Current Time: " + DateTime.Now.ToString());
                outputStream.WriteLine("url : {0}", HttpUrl);

                outputStream.WriteLine("<form method=post action=/form>");
                outputStream.WriteLine("<input type=text name=foo value=foovalue>");
                outputStream.WriteLine("<input type=submit name=bar value=barvalue>");
                outputStream.WriteLine("</form>");
            }
        }

        private const int BUF_SIZE = 4096;
        public void HandlePOSTRequest(Stream inputStream, StreamWriter outputStream)
        {
            // this post data processing just reads everything into a memory stream.
            // this is fine for smallish things, but for large stuff we should really
            // hand an input stream to the request processor. However, the input stream 
            // we hand him needs to let him see the "end of the stream" at this content 
            // length, because otherwise he won't know when he's seen it all! 

            Console.WriteLine("get post data start");
            int content_len = 0;
            MemoryStream ms = new MemoryStream();
            if (HttpHeaders.ContainsKey("content-length"))
            {
                content_len = Convert.ToInt32(HttpHeaders["content-length"]);
                if (content_len > MAX_POST_SIZE)
                {
                    throw new Exception(
                        String.Format("POST Content-Length({0}) too big for this simple server",
                          content_len));
                }
                byte[] buf = new byte[BUF_SIZE];
                int to_read = content_len;
                while (to_read > 0)
                {
                    Console.WriteLine("starting Read, to_read={0}", to_read);

                    int numread = inputStream.Read(buf, 0, Math.Min(BUF_SIZE, to_read));
                    Console.WriteLine("read finished, numread={0}", numread);
                    if (numread == 0)
                    {
                        if (to_read == 0)
                        {
                            break;
                        }
                        else
                        {
                            throw new Exception("client disconnected during post");
                        }
                    }
                    to_read -= numread;
                    ms.Write(buf, 0, numread);
                }
                ms.Seek(0, SeekOrigin.Begin);
            }
            Console.WriteLine("get post data end");

            StreamReader sr = new StreamReader(ms);
            
            string data = sr.ReadToEnd();

            Console.WriteLine("POST request: {0}", data);
            
            //Parse the data
            int gameId = 0;
            int.TryParse((string)HttpHeaders["gameid"], out gameId);

            Hashtable dataEntries = new Hashtable();
            string[] lines = data.Split('&');
            foreach (string line in lines)
            {
                string[] parts = line.Split('=');
                dataEntries[parts[0]] = parts[1];
            }

            TwodokuGameInfo gameInfo = TwodokuGameInfo.FromHttpPost(gameId, dataEntries);

            //Update the database
            bool success = true;
            if (gameId <= 0)
            {
                //Add new game
                gameInfo.GameId = mDB.GetNextKey(DBHelper.TableGames, DBHelper.ColumnGameId);
                mDB.AddGame(gameInfo);
            }
            else
            {
                //Update existing game
                success = mDB.UpdateGame(gameInfo);
            }
            
            //Respond with failure if there is a problem (i.e. gameId and players don't match)
            if (success)
            {
                outputStream.WriteLine("HTTP/1.0 200 OK");
                outputStream.WriteLine("Content-Type: text/html");
                outputStream.WriteLine("Connection: close");
                outputStream.WriteLine("");
                outputStream.WriteLine("<html><body><h1>test server</h1>");
                outputStream.WriteLine("<a href=/test>return</a><p>");
                outputStream.WriteLine("postbody: <pre>{0}</pre>", data);
            }
            else
            {
                outputStream.WriteLine("HTTP/1.0 404 File not found");
                outputStream.WriteLine("Connection: close");
                outputStream.WriteLine("");
            }
        }
    }
}
