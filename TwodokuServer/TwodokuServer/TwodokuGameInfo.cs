using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Collections;

namespace TwodokuServer
{
    public class TwodokuGameInfo
    {
        public int GameId = -1;

        public string Player1 = null;
        public int Player1Score = 0;
        public string Player2 = null;
        public int Player2Score = 0;

        public int Active = 0;
        public int Turn = 0;

        public string InitialBoard = null;
        public string PlayerBoard = null;
        public string Multipliers = null;

        public string HandSystem = null;
        public int HandSize = 0;
        public string ScoringSystem = null;

        public static TwodokuGameInfo FromHttpPost(int gameId, Hashtable dataEntries)
        {
            TwodokuGameInfo ret = new TwodokuGameInfo();

            ret.Player1 = (string)dataEntries["Player1"];
            int.TryParse((string)dataEntries["Player1Score"], out ret.Player1Score);
            ret.Player2 = (string)dataEntries["Player2"];
            int.TryParse((string)dataEntries["Player2Score"], out ret.Player2Score);
            int.TryParse((string)dataEntries["Active"], out ret.Active);
            int.TryParse((string)dataEntries["Turn"], out ret.Turn);
            ret.PlayerBoard = (string)dataEntries["PlayerBoard"];
            ret.HandSystem = (string)dataEntries["HandSystem"];
            int.TryParse((string)dataEntries["HandSize"], out ret.HandSize);
            ret.ScoringSystem = (string)dataEntries["ScoringSystem"];
            ret.InitialBoard = (string)dataEntries["StartingBoard"];
            ret.Multipliers = (string)dataEntries["Multipliers"];

            return ret;
        }
    }
}
