using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading;

namespace TwodokuServer
{
    class Program
    {
        static void Main(string[] args)
        {
            TwodokuServer httpServer;
            if (args.GetLength(0) > 0)
            {
                httpServer = new TwodokuServer(Convert.ToInt16(args[0]));
            }
            else
            {
                httpServer = new TwodokuServer(8080);
            }
            Thread thread = new Thread(new ThreadStart(httpServer.listen));
            thread.Start();
        }
    }
}
