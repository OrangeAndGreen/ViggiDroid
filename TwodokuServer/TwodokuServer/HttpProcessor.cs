using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Net;
using System.IO;
using System.Collections;
using System.Data.SqlClient;
using System.Threading;
using System.Net.Sockets;

namespace TwodokuServer
{
    public class HttpProcessor
    {
        private TcpClient mSocket;
        private DBHelper mDB = null;

        public String HttpMethod;
        public String HttpUrl;
        private IPAddress mIPAddress = null;
        private String HttpProtocolVersionString;
        public Hashtable HttpHeaders = new Hashtable();

        private static int MAX_POST_SIZE = 10 * 1024 * 1024; // 10MB

        public HttpProcessor(TcpClient s, DBHelper db)
        {
            mSocket = s;
            mDB = db;

            mIPAddress = ((IPEndPoint)mSocket.Client.RemoteEndPoint).Address;
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
            string postPlayer = null;
            try
            {
                //Parse the request
                String request = StreamReadLine(inputStream);
                //Console.WriteLine(request);
                string[] tokens = request.Split(' ');
                if (tokens.Length != 3)
                {
                    throw new Exception("invalid http request line");
                }
                HttpMethod = tokens[0].ToUpper();
                HttpUrl = tokens[1];
                HttpProtocolVersionString = tokens[2];

                DateTime now = DateTime.Now;
                //Console.Write(string.Format("{0} {1}: ", now.ToShortDateString(), now.ToShortTimeString()));

                ReadHeaders(inputStream);
                if (HttpMethod.Equals("GET"))
                {
                    HandleGETRequest(outputStream);
                }
                else if (HttpMethod.Equals("POST"))
                {
                    postPlayer = HandlePOSTRequest(inputStream, outputStream);
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

            if (postPlayer != null)
            {
                //Send a test ping to the player
                string url = "https://android.googleapis.com/gcm/send";
                TwodokuPlayer player = mDB.GetPlayer(postPlayer);
                if (player != null)
                {
                    string data = "registration_id=" + player.GcmId;
                    //Console.WriteLine(string.Format("Pinging user {0}", player.Name));
                    string response = SendPost(url, data);
                    //Console.WriteLine(response);
                }
                else
                {
                    //Create the new player in the database
                    Console.WriteLine("Creating new player: " + postPlayer);
                    TwodokuPlayer newPlayer = new TwodokuPlayer(-1, postPlayer, "", "");
                    newPlayer.PlayerId = mDB.GetNextKey(DBHelper.TablePlayers, DBHelper.ColumnPlayerId);
                    mDB.AddPlayer(newPlayer);
                }
            }
        }

        public void ReadHeaders(Stream inputStream)
        {
            //Console.WriteLine("readHeaders()");
            String line;
            while ((line = StreamReadLine(inputStream)) != null)
            {
                if (line.Equals(""))
                {
                    //Console.WriteLine("got headers");
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
                //Console.WriteLine("header: {0}:{1}", name, value);
                HttpHeaders[name.ToLower()] = value;
            }
        }

        public TwodokuPlayer HandleGETRequest(StreamWriter outputStream)
        {
            //Console.WriteLine("request: {0}", HttpUrl);

            //Write the success response
            outputStream.WriteLine("HTTP/1.0 200 OK");
            outputStream.WriteLine("Content-Type: text/html");
            outputStream.WriteLine("Connection: close");
            outputStream.WriteLine("");

            string method = "";
            if (HttpHeaders.ContainsKey("method"))
                method = (string)HttpHeaders["method"];

            string name = (string)HttpHeaders["player"];
            string password = (string)HttpHeaders["password"];
            string gcmId = (string)HttpHeaders["gcmid"];

            //CODE TO CREATE A NEW PLAYER
            //TwodokuPlayer player = mDB.GetPlayer(name);
            //if (player == null)
            //{
            //    player = new TwodokuPlayer(-1, name, password, gcmId);
            //    player.PlayerId = mDB.GetNextKey(DBHelper.TablePlayers, DBHelper.ColumnPlayerId);
            //    mDB.AddPlayer(player);
            //}

            TwodokuPlayer player = mDB.GetPlayer(name);
            if (LoginUser(player, password))
            {
                if (gcmId != null)
                {
                    player.GcmId = gcmId;
                    mDB.UpdatePlayerGcmId(player);
                }

                if (method.Equals("Gamelist"))
                {
                    outputStream.Write(GenerateGameListResponse(name));
                }
                else if (method.Equals("Game"))
                {
                    int gameId = -1;
                    if (HttpHeaders.ContainsKey("gameid"))
                        int.TryParse((string)HttpHeaders["gameid"], out gameId);

                    outputStream.Write(GenerateGameResponse(name, gameId));
                }
            }
            else
            {
                outputStream.Write(GenerateFailedLoginResponse(name));
            }

            return player;
        }

        private bool LoginUser(TwodokuPlayer player, string password)
        {
            if (player == null || password == null)
                return false;

            return password.Equals(player.Password);
        }

        private String GenerateFailedLoginResponse(string name)
        {
            if (name == null || name.Equals(""))
                name = "(unknown)";
            Console.WriteLine(string.Format("{0}: Sending login failure to {1} ({2})", DateStrings.ToString(DateTime.Now), name, mIPAddress));

            StringBuilder ret = new StringBuilder();

            ret.AppendLine("HTTP/1.0 404 Login failed");
            ret.AppendLine("Connection: close");
            ret.AppendLine("");

            return ret.ToString();
        }

        private String GenerateGameListResponse(string name)
        {
            Console.WriteLine(string.Format("{0}: Sending game list to {1} ({2})", DateStrings.ToString(DateTime.Now), name, mIPAddress));
            
            string qualifier = DBHelper.ColumnPlayer1 + "='" + name + "' OR " + DBHelper.ColumnPlayer2 + "='" + name + "'";
            string query = String.Format("select {0} from {1} where {2} order by {3}", "*", DBHelper.TableGames, qualifier, "STARTDATE");
            SqlDataReader reader = mDB.Query(query);

            StringBuilder ret = new StringBuilder();
            ret.AppendLine("HTTP/1.0 200 OK");
            if (reader != null)
            {
                while (reader.Read())
                {
                    TwodokuGameInfo gameInfo = TwodokuGameInfo.FromSqlReader(reader);
                    ret.AppendLine(gameInfo.ToPacket(true));
                }
                reader.Close();
            }
            else
                ret.AppendLine("No games");

            return ret.ToString();
        }

        private String GenerateGameResponse(string name, int id)
        {
            Console.WriteLine(string.Format("{0}: Sending game {1} to {2} ({3})", DateStrings.ToString(DateTime.Now), id, name, mIPAddress));

            TwodokuGameInfo gameInfo = mDB.GetGame(id);

            StringBuilder ret = new StringBuilder();
            ret.AppendLine("HTTP/1.0 200 OK");
            ret.AppendLine(gameInfo.ToPacket(false));
            return ret.ToString();
        }

        public string HandlePOSTRequest(Stream inputStream, StreamWriter outputStream)
        {
            string data = GetPostData(inputStream);

            //Console.WriteLine("POST request: {0}", data);

            //Parse the data
            int gameId = 0;
            int.TryParse((string)HttpHeaders["gameid"], out gameId);

            String playerName = (string)HttpHeaders["player"];
            String password = (string)HttpHeaders["password"];

            TwodokuPlayer player = mDB.GetPlayer(playerName);
            if (LoginUser(player, password))
            {
                Hashtable dataEntries = new Hashtable();
                string[] lines = data.Split('&');
                foreach (string line in lines)
                {
                    string[] parts = line.Split('=');
                    dataEntries[parts[0]] = parts[1];
                }

                TwodokuGameInfo gameInfo = TwodokuGameInfo.FromHttpPost(gameId, dataEntries);

                //Update the database
                bool success = false;
                if (gameId <= 0)
                {
                    //Add new game
                    Console.WriteLine(string.Format("{0}: Adding new game for {1}", DateStrings.ToString(DateTime.Now), playerName));
                    gameInfo.GameId = mDB.GetNextKey(DBHelper.TableGames, DBHelper.ColumnGameId);
                    success = mDB.AddGame(gameInfo);
                }
                else
                {
                    //Update existing game
                    Console.WriteLine(string.Format("{0}: Updating game {1} for {2}", DateStrings.ToString(DateTime.Now), gameId, playerName));
                    success = mDB.UpdateGame(gameInfo);
                }

                //Respond with failure if there is a problem (i.e. gameId and players don't match)
                if (success)
                {
                    outputStream.WriteLine("HTTP/1.0 200 OK");
                    outputStream.WriteLine("Content-Type: text/html");
                    outputStream.WriteLine("Connection: close");
                    outputStream.WriteLine("");
                    //outputStream.WriteLine("<html><body><h1>test server</h1>");
                    //outputStream.WriteLine("<a href=/test>return</a><p>");
                    //outputStream.WriteLine("postbody: <pre>{0}</pre>", data);
                    
                    string otherPlayer = gameInfo.Player2;
                    if (playerName.Equals(otherPlayer))
                        otherPlayer = gameInfo.Player1;

                    return otherPlayer;
                }
                else
                {
                    outputStream.WriteLine("HTTP/1.0 404 Update failed");
                    outputStream.WriteLine("Connection: close");
                    outputStream.WriteLine("");

                    return null;
                }
            }
            else
            {
                outputStream.Write(GenerateFailedLoginResponse(playerName));
                return null;
            }
        }

        private const int BUF_SIZE = 4096;
        private string GetPostData(Stream inputStream)
        {
            // this post data processing just reads everything into a memory stream.
            // this is fine for smallish things, but for large stuff we should really
            // hand an input stream to the request processor. However, the input stream 
            // we hand him needs to let him see the "end of the stream" at this content 
            // length, because otherwise he won't know when he's seen it all! 

            //Console.WriteLine("get post data start");
            int content_len = 0;
            MemoryStream ms = new MemoryStream();
            if (HttpHeaders.ContainsKey("content-length"))
            {
                content_len = Convert.ToInt32(HttpHeaders["content-length"]);
                if (content_len > MAX_POST_SIZE)
                {
                    throw new Exception(String.Format("POST Content-Length({0}) too big for this simple server", content_len));
                }
                byte[] buf = new byte[BUF_SIZE];
                int to_read = content_len;
                while (to_read > 0)
                {
                    //Console.WriteLine("starting Read, to_read={0}", to_read);

                    int numread = inputStream.Read(buf, 0, Math.Min(BUF_SIZE, to_read));
                    //Console.WriteLine("read finished, numread={0}", numread);
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
            //Console.WriteLine(string.Format("Received {0} bytes", content_len));

            StreamReader sr = new StreamReader(ms);

            return sr.ReadToEnd();
        }

        public string SendPost(string url, string postData)
        {
            string webpageContent = string.Empty;

            try
            {
                byte[] byteArray = Encoding.UTF8.GetBytes(postData);

                HttpWebRequest webRequest = (HttpWebRequest)WebRequest.Create(url);
                webRequest.Method = "POST";
                webRequest.ContentType = "application/x-www-form-urlencoded";
                webRequest.ContentLength = byteArray.Length;

                string regId = "AIzaSyDZwlIo0uQ90PvRac272_vAyllnW9eok38";
                webRequest.Headers.Add("Authorization", "key=" + regId);

                using (Stream webpageStream = webRequest.GetRequestStream())
                {
                    webpageStream.Write(byteArray, 0, byteArray.Length);
                }

                using (HttpWebResponse webResponse = (HttpWebResponse)webRequest.GetResponse())
                {
                    using (StreamReader reader = new StreamReader(webResponse.GetResponseStream()))
                    {
                        webpageContent = reader.ReadToEnd();
                    }
                }
            }
            catch (Exception ex)
            {
                Console.WriteLine(ex.Message);
            }

            return webpageContent;
        }
    }
}
