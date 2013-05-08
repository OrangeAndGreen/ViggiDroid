using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Collections;
using System.Data.SqlClient;

namespace TwodokuServer
{
    public class TwodokuGameInfo
    {
        public int GameId = -1;

        public string Player1 = null;
        public int Player1Score = 0;
        public string Player2 = null;
        public int Player2Score = 0;

        public DateTime StartDate;
        public DateTime PlayDate;

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

            ret.GameId = gameId;

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

            ret.PlayDate = DateTime.Now;

            return ret;
        }

        public static TwodokuGameInfo FromSqlReader(SqlDataReader reader)
        {
            TwodokuGameInfo gameInfo = new TwodokuGameInfo();

            gameInfo.GameId = (int)reader[DBHelper.ColumnGameId];
            gameInfo.Player1 = (string)reader[DBHelper.ColumnPlayer1];
            gameInfo.Player1Score = (int)reader[DBHelper.ColumnPlayer1Score];
            gameInfo.Player2 = (string)reader[DBHelper.ColumnPlayer2];
            gameInfo.Player2Score = (int)reader[DBHelper.ColumnPlayer2Score];
            
            gameInfo.StartDate = (DateTime)reader[DBHelper.ColumnStartDate];
            gameInfo.PlayDate = (DateTime)reader[DBHelper.ColumnPlayDate];

            gameInfo.Active = (int)reader[DBHelper.ColumnActive];
            gameInfo.Turn = (int)reader[DBHelper.ColumnTurn];

            gameInfo.InitialBoard = (string)reader[DBHelper.ColumnStartingBoard];
            gameInfo.PlayerBoard = (string)reader[DBHelper.ColumnPlayerBoard];
            gameInfo.Multipliers = (string)reader[DBHelper.ColumnMultipliers];

            gameInfo.HandSystem = (string)reader[DBHelper.ColumnHandSystem];
            gameInfo.HandSize = (int)reader[DBHelper.ColumnHandSize];
            gameInfo.ScoringSystem = (string)reader[DBHelper.ColumnScoringSystem];

            return gameInfo;
        }

        public string ToPacket(bool infoOnly)
        {
            string ret = "";

            DateTime timestamp = StartDate;
            string startDate = string.Format("{0}:{1}:{2}:{3}:{4}:{5}", timestamp.Year, timestamp.Month, timestamp.Day, timestamp.Hour, timestamp.Minute, timestamp.Second);
            timestamp = PlayDate;
            string playDate = string.Format("{0}:{1}:{2}:{3}:{4}:{5}", timestamp.Year, timestamp.Month, timestamp.Day, timestamp.Hour, timestamp.Minute, timestamp.Second);

            ret = string.Format("{0},{1},{2},{3},{4},{5},{6},{7},{8}",
                    GameId, Player1, Player1Score, Player2, Player2Score,
                    startDate, playDate, Turn, Active);

            if (!infoOnly)
            {
                ret += string.Format(",{0},{1},{2},{3},{4},{5}",
                    HandSystem, HandSize, ScoringSystem, InitialBoard, PlayerBoard, Multipliers);
            }

            return ret;
        }

        public bool IsSameGame(TwodokuGameInfo gameInfo)
        {
            if (GameId != gameInfo.GameId)
                return false;
            if (Player1 != gameInfo.Player1)
                return false;
            if (Player2 != gameInfo.Player2)
                return false;
            //if (StartDate != gameInfo.StartDate)
            //    return false;
            //if (HandSystem != gameInfo.HandSystem)
            //    return false;
            //if (HandSize != gameInfo.HandSize)
            //    return false;
            //if (ScoringSystem != gameInfo.ScoringSystem)
            //    return false;

            return true;
        }
    }
}
