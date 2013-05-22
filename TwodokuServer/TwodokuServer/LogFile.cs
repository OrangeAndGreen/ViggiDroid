using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.IO;

namespace TwodokuServer
{
    public class LogFile
    {
        public static string GetLocalName(string filename)
        {
            int index = filename.LastIndexOf('\\');
            string ret = filename.Substring(index + 1);

            index = ret.LastIndexOf('.');
            ret = ret.Substring(0, index);

            return ret;
        }

        public static void LogError(string error)
        {
            string filename = "Errors.txt";
            string entry = DateTime.Now.ToString() + ": " + error;

            Console.WriteLine(entry);
            
            TextWriter writer = null;
            writer = System.IO.File.AppendText(filename);
            writer.Write(entry + "\n");
            writer.Close();
        }
    }
}
