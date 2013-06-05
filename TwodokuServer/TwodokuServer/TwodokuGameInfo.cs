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

        public int Status = 0;
        public int Turn = 0;

        public string InitialBoard = null;
        public string PlayerBoard = null;
        public string Multipliers = null;

        public string HandSystem = null;
        public int HandSize = 0;
        public string ScoringSystem = null;
        public string MultiplierSystem = null;
        public string BonusSystem = null;

        public string LastMove = null;
        public string Hand = null;

        public static TwodokuGameInfo FromHttpPost(int gameId, Hashtable dataEntries)
        {
            TwodokuGameInfo ret = new TwodokuGameInfo();

            ret.GameId = gameId;

            ret.Player1 = (string)dataEntries["Player1"];
            int.TryParse((string)dataEntries["Player1Score"], out ret.Player1Score);
            ret.Player2 = (string)dataEntries["Player2"];
            int.TryParse((string)dataEntries["Player2Score"], out ret.Player2Score);
            int.TryParse((string)dataEntries["Status"], out ret.Status);
            int.TryParse((string)dataEntries["Turn"], out ret.Turn);
            ret.LastMove = (string)dataEntries["LastMove"];
            ret.PlayerBoard = (string)dataEntries["PlayerBoard"];
            ret.HandSystem = (string)dataEntries["HandSystem"];
            int.TryParse((string)dataEntries["HandSize"], out ret.HandSize);
            ret.ScoringSystem = (string)dataEntries["ScoringSystem"];
            ret.MultiplierSystem = (string)dataEntries["MultiplierSystem"];
            ret.BonusSystem = (string)dataEntries["BonusSystem"];
            ret.InitialBoard = (string)dataEntries["StartingBoard"];
            ret.Multipliers = (string)dataEntries["Multipliers"];

            ret.PlayDate = DateTime.Now;

            return ret;
        }

        /// <summary>
        /// This is used for retrieving a game from the database
        /// </summary>
        /// <param name="reader"></param>
        /// <returns></returns>
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

            gameInfo.Status = (int)reader[DBHelper.ColumnStatus];
            gameInfo.Turn = (int)reader[DBHelper.ColumnTurn];

            gameInfo.HandSystem = (string)reader[DBHelper.ColumnHandSystem];
            gameInfo.HandSize = (int)reader[DBHelper.ColumnHandSize];
            gameInfo.ScoringSystem = (string)reader[DBHelper.ColumnScoringSystem];

            gameInfo.MultiplierSystem = (string)reader[DBHelper.ColumnMultiplierSystem];
            gameInfo.BonusSystem = (string)reader[DBHelper.ColumnBonusSystem];

            gameInfo.LastMove = (string)reader[DBHelper.ColumnLastMove];
            gameInfo.Hand = (string)reader[DBHelper.ColumnHand];
            
            gameInfo.InitialBoard = (string)reader[DBHelper.ColumnStartingBoard];
            gameInfo.PlayerBoard = (string)reader[DBHelper.ColumnPlayerBoard];
            gameInfo.Multipliers = (string)reader[DBHelper.ColumnMultipliers];

            return gameInfo;
        }

        /// <summary>
        /// This is used for importing the database from a text file
        /// </summary>
        /// <param name="input"></param>
        /// <returns></returns>
        public static TwodokuGameInfo FromString(string input)
        {
            string[] parts = input.Split(',');

            TwodokuGameInfo ret = new TwodokuGameInfo();

            int.TryParse(parts[0], out ret.GameId);
            ret.Player1 = parts[1];
            int.TryParse(parts[2], out ret.Player1Score);
            ret.Player2 = parts[3];
            int.TryParse(parts[4], out ret.Player2Score);

            ret.StartDate = DateStrings.FromString(parts[5]);
            ret.PlayDate = DateStrings.FromString(parts[6]);

            int.TryParse(parts[7], out ret.Status);
            int.TryParse(parts[8], out ret.Turn);

            ret.HandSystem = parts[9];
            int.TryParse(parts[10], out ret.HandSize);
            ret.ScoringSystem = parts[11];
            ret.MultiplierSystem = parts[12];
            ret.BonusSystem = parts[13];

            ret.LastMove = parts[14];
            ret.Hand = parts[15];

            ret.InitialBoard = parts[16];
            ret.PlayerBoard = parts[17];
            ret.Multipliers = parts[18];
            
            return ret;
        }

        /// <summary>
        /// This is sent to the client
        /// </summary>
        /// <param name="infoOnly"></param>
        /// <returns></returns>
        public string ToPacket(bool infoOnly)
        {
            string ret = "";

            DateTime timestamp = StartDate;
            string startDate = string.Format("{0}:{1}:{2}:{3}:{4}:{5}", timestamp.Year, timestamp.Month, timestamp.Day, timestamp.Hour, timestamp.Minute, timestamp.Second);
            timestamp = PlayDate;
            string playDate = string.Format("{0}:{1}:{2}:{3}:{4}:{5}", timestamp.Year, timestamp.Month, timestamp.Day, timestamp.Hour, timestamp.Minute, timestamp.Second);

            ret = string.Format("{0},{1},{2},{3},{4},{5},{6},{7},{8}",
                    GameId, Player1, Player1Score, Player2, Player2Score,
                    startDate, playDate, Status, Turn);

            if (!infoOnly)
            {
                ret += string.Format(",{0},{1},{2},{3},{4},{5},{6},{7},{8},{9}",
                    HandSystem, HandSize, ScoringSystem, MultiplierSystem, BonusSystem, LastMove, Hand, InitialBoard, PlayerBoard, Multipliers);
            }

            return ret;
        }

        /// <summary>
        /// This is used to export the database to a text file
        /// </summary>
        /// <returns></returns>
        public override string ToString()
        {
            string ret =  string.Format("{0},{1},{2},{3},{4},{5},{6},{7},{8},{9},{10},{11},{12},{13},{14},{15},{16},{17},{18}",
                GameId, Player1, Player1Score, Player2, Player2Score, DateStrings.ToString(StartDate), DateStrings.ToString(PlayDate), Status, Turn,
                HandSystem, HandSize, ScoringSystem, MultiplierSystem, BonusSystem, LastMove, Hand, InitialBoard, PlayerBoard, Multipliers);

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
