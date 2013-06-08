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
            string entry = DateStrings.ToString(DateTime.Now) + ": " + error;

            Console.WriteLine(entry);

            if (!File.Exists(filename))
                File.Create(filename).Close();

            TextWriter writer = File.AppendText(filename);
            writer.Write(entry + "\n");
            writer.Close();
        }

        public static void CreateGameLog(string filename, TwodokuGameInfo game)
        {
            TextWriter writer = File.CreateText(filename);

            writer.WriteLine(string.Format("Initial Board: {0}", game.InitialBoard));
            writer.WriteLine(string.Format("Multipliers: {0}", game.Multipliers));
            writer.WriteLine(string.Format("Hand System: {0} ({1})", game.HandSystem, game.HandSize));
            writer.WriteLine(string.Format("Scoring System: {0}", game.ScoringSystem));
            writer.WriteLine(string.Format("Multiplier System: {0}", game.MultiplierSystem));
            writer.WriteLine(string.Format("Bonus System: {0}", game.BonusSystem));

            writer.Close();
        }

        public static void LogGameMove(TwodokuGameInfo game)
        {
            string filename = string.Format("{0} - {1} v {2}.txt", game.GameId, game.Player1, game.Player2);

            string entry = string.Format("{0}: {1}, {2}-{3}", DateStrings.ToString(DateTime.Now), game.LastMove, game.Player1Score, game.Player2Score);

            if (!File.Exists(filename))
                CreateGameLog(filename, game);

            TextWriter writer = File.AppendText(filename);
            writer.Write(entry + "\n");
            writer.Close();
        }
    }
}
