using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Data.SqlClient;

namespace TwodokuServer
{
    public class TwodokuPlayer
    {
        public int PlayerId = 0;
        public string Name = null;
        public string Password = null;
        public string GcmId = null;
        public int Wins = 0;
        public int Losses = 0;
        public int Streak = 0;

        public TwodokuPlayer()
        {
        }

        public TwodokuPlayer(int playerId, string name, string password, string gcmId)
        {
            PlayerId = playerId;
            Name = name;
            Password = password;
            GcmId = gcmId;
        }

        /// <summary>
        /// This is used for retrieving a player from the database
        /// </summary>
        /// <param name="reader"></param>
        /// <returns></returns>
        public static TwodokuPlayer FromSqlReader(SqlDataReader reader)
        {
            TwodokuPlayer player = new TwodokuPlayer();

            player.PlayerId = (int)reader[DBHelper.ColumnPlayerId];
            player.Name = (string)reader[DBHelper.ColumnName];
            player.Password = (string)reader[DBHelper.ColumnPassword];
            player.GcmId = (string)reader[DBHelper.ColumnGcmId];
            player.Wins = (int)reader[DBHelper.ColumnWins];
            player.Losses = (int)reader[DBHelper.ColumnLosses];
            player.Streak = (int)reader[DBHelper.ColumnStreak];

            return player;
        }

        /// <summary>
        /// This is used for importing the database from a text file
        /// </summary>
        /// <param name="input"></param>
        /// <returns></returns>
        public static TwodokuPlayer FromString(string input)
        {
            string[] parts = input.Split(',');

            TwodokuPlayer ret = new TwodokuPlayer();

            int.TryParse(parts[0], out ret.PlayerId);
            ret.Name = parts[1];
            ret.Password = parts[2];
            ret.GcmId = parts[3];
            int.TryParse(parts[4], out ret.Wins);
            int.TryParse(parts[5], out ret.Losses);
            int.TryParse(parts[6], out ret.Streak);

            return ret;
        }

        /// <summary>
        /// This is used to export the database to a text file
        /// </summary>
        /// <returns></returns>
        public override string ToString()
        {
            string ret = string.Format("{0},{1},{2},{3},{4},{5},{6}",
                PlayerId, Name, Password, GcmId, Wins, Losses, Streak);

            return ret;
        }
    }
}
