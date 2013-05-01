using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.IO;
using System.Net.Sockets;
using System.Collections;
using System.Threading;
using System.Net;

namespace TwodokuServer
{
    public class TwodokuServer
    {
        protected int mPort;
        TcpListener mListener;
        bool mActive = true;

        public TwodokuServer(int port)
        {
            mPort = port;
        }

        public void listen()
        {
            mListener = new TcpListener(IPAddress.Loopback, mPort);
            mListener.Start();
            while (mActive)
            {
                TcpClient s = mListener.AcceptTcpClient();
                HttpProcessor processor = new HttpProcessor(s, this);
                Thread thread = new Thread(new ThreadStart(processor.process));
                thread.Start();
                Thread.Sleep(1);
            }
        }
    }



    public class HttpProcessor
    {
        public TcpClient mSocket;
        public TwodokuServer mServer;

        private Stream mInputStream;
        public StreamWriter OutputStream;

        public String HttpMethod;
        public String HttpUrl;
        public String HttpProtocolVersionString;
        public Hashtable HttpHeaders = new Hashtable();


        private static int MAX_POST_SIZE = 10 * 1024 * 1024; // 10MB

        public HttpProcessor(TcpClient s, TwodokuServer srv)
        {
            mSocket = s;
            mServer = srv;
        }

        private string streamReadLine(Stream inputStream)
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

        public void process()
        {
            // we can't use a StreamReader for input, because it buffers up extra data on us inside it's
            // "processed" view of the world, and we want the data raw after the headers
            mInputStream = new BufferedStream(mSocket.GetStream());

            // we probably shouldn't be using a streamwriter for all output from handlers either
            OutputStream = new StreamWriter(new BufferedStream(mSocket.GetStream()));
            try
            {
                //Parse the request
                String request = streamReadLine(mInputStream);
                string[] tokens = request.Split(' ');
                if (tokens.Length != 3)
                {
                    throw new Exception("invalid http request line");
                }
                HttpMethod = tokens[0].ToUpper();
                HttpUrl = tokens[1];
                HttpProtocolVersionString = tokens[2];

                Console.WriteLine("starting: " + request);


                readHeaders();
                if (HttpMethod.Equals("GET"))
                {
                    handleGETRequest();
                }
                else if (HttpMethod.Equals("POST"))
                {
                    handlePOSTRequest();
                }
            }
            catch (Exception e)
            {
                Console.WriteLine("Exception: " + e.ToString());
                writeFailure();
            }
            OutputStream.Flush();
            mInputStream = null;
            OutputStream = null;
            mSocket.Close();
        }

        public void readHeaders()
        {
            Console.WriteLine("readHeaders()");
            String line;
            while ((line = streamReadLine(mInputStream)) != null)
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

        public void handleGETRequest()
        {
            Console.WriteLine("request: {0}", HttpUrl);
            writeSuccess();

            string method = "";
            if (HttpHeaders.ContainsKey("method"))
            {
                method = (string)HttpHeaders["method"];
            }

            if (method.Equals("Gamelist"))
            {
                OutputStream.WriteLine("No games");
            }
            else
            {
                OutputStream.WriteLine("<html><body><h1>test server</h1>");
                OutputStream.WriteLine("Current Time: " + DateTime.Now.ToString());
                OutputStream.WriteLine("url : {0}", HttpUrl);

                OutputStream.WriteLine("<form method=post action=/form>");
                OutputStream.WriteLine("<input type=text name=foo value=foovalue>");
                OutputStream.WriteLine("<input type=submit name=bar value=barvalue>");
                OutputStream.WriteLine("</form>");
            }
        }

        private const int BUF_SIZE = 4096;
        public void handlePOSTRequest()
        {
            // this post data processing just reads everything into a memory stream.
            // this is fine for smallish things, but for large stuff we should really
            // hand an input stream to the request processor. However, the input stream 
            // we hand him needs to let him see the "end of the stream" at this content 
            // length, because otherwise he won't know when he's seen it all! 

            Console.WriteLine("get post data start");
            int content_len = 0;
            MemoryStream ms = new MemoryStream();
            if (this.HttpHeaders.ContainsKey("Content-Length"))
            {
                content_len = Convert.ToInt32(this.HttpHeaders["Content-Length"]);
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

                    int numread = this.mInputStream.Read(buf, 0, Math.Min(BUF_SIZE, to_read));
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
            Console.WriteLine("POST request: {0}", HttpUrl);
            string data = sr.ReadToEnd();

            OutputStream.WriteLine("<html><body><h1>test server</h1>");
            OutputStream.WriteLine("<a href=/test>return</a><p>");
            OutputStream.WriteLine("postbody: <pre>{0}</pre>", data);
        }

        public void writeSuccess(string content_type = "text/html")
        {
            OutputStream.WriteLine("HTTP/1.0 200 OK");
            OutputStream.WriteLine("Content-Type: " + content_type);
            OutputStream.WriteLine("Connection: close");
            OutputStream.WriteLine("");
        }

        public void writeFailure()
        {
            OutputStream.WriteLine("HTTP/1.0 404 File not found");
            OutputStream.WriteLine("Connection: close");
            OutputStream.WriteLine("");
        }
    }


}
