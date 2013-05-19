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
        /// This is used for retrieving a game from the database
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

            return player;
        }
    }
}
