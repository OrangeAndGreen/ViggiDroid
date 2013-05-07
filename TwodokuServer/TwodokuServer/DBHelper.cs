using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Data.SqlClient;

namespace TwodokuServer
{
    public class DBHelper
    {
        public const string TableGames = "Games";

        public const string ColumnGameId = "GAMEID";
        public const string ColumnPlayer1 = "PLAYER1";
        public const string ColumnPlayer1Score = "PLAYER1SCORE";
        public const string ColumnPlayer2 = "PLAYER2";
        public const string ColumnPlayer2Score = "PLAYER2SCORE";
        public const string ColumnStartDate = "STARTDATE";
        public const string ColumnPlayDate = "PLAYDATE";
        public const string ColumnActive = "ACTIVE";
        public const string ColumnTurn = "TURN";
        public const string ColumnHandSystem = "HANDSYSTEM";
        public const string ColumnHandSize = "HANDSIZE";
        public const string ColumnScoringSystem = "SCORINGSYSTEM";
        public const string ColumnStartingBoard = "STARTINGBOARD";
        public const string ColumnPlayerBoard = "PLAYERBOARD";
        public const string ColumnMultipliers = "MULTIPLIERS";

        public string Server = null;
        public string Database = null;

        private SqlConnection mSQL = null;

        public DBHelper(string server, string database)
        {
            Server = server;
            Database = database;
        }

        public string Open()
        {
            string connectionString = String.Format("Data Source={0};Database={1};Integrated Security=True;Connect Timeout=15;User Instance=False;", Server, Database);
            mSQL = new SqlConnection(connectionString);

            try
            {
                mSQL.Open();
                return "Opened DB\r\n\r\n";
            }
            catch (Exception e)
            {
                return "Failed to connect to DB: " + e.ToString() + "\r\n\r\n";
            }
        }

        public string Close()
        {
            if (mSQL != null)
            {
                mSQL.Close();
                mSQL = null;
                return "Closed DB\r\n";
            }

            return "DB not open\r\n";
        }

        public void Reset(string pass)
        {
            if (pass != null && pass.Equals("ERASE IT ALL"))
            {
                Query(String.Format("truncate table {0}", TableGames));
            }
        }

        public bool Insert(string table, string values, string name = null)
        {
            bool check = false;
            string command = String.Format("INSERT into {0} values ({1})", table, values);
            try
            {
                SqlCommand insertCommand = new SqlCommand(command, mSQL);
                insertCommand.ExecuteNonQuery();
                check = true;
            }
            catch (Exception e)
            {
                LogFile.LogError(String.Format("DB Insert failed: {0}\r\n{1}", command, e));
            }

            //string success = check ? "passed" : "failed";
            //string label = name != null ? name : values;
            //return String.Format("Adding to DB {0} {1}: {2}\r\n", table, success, label);
            return check;
        }

        public string Delete(string table, string column, string value, string name = null)
        {
            bool check = false;
            string command = String.Format("DELETE from {0} where {1}={2}", table, column, value);
            try
            {
                SqlCommand deleteCommand = new SqlCommand(command, mSQL);
                deleteCommand.ExecuteNonQuery();
                check = true;
            }
            catch (Exception e)
            {
                LogFile.LogError(String.Format("DB Delete failed: {0}\r\n{1}", command, e));
            }

            string success = check ? "passed" : "failed";
            string label = name != null ? name : value;
            return String.Format("Deleting from DB '{0}' {1}: {2}\r\n", table, success, label);
        }

        public string Update(string table, string update, string qualifier)
        {
            string ret = null;
            string command = String.Format("UPDATE {0} SET {1} WHERE {2}", table, update, qualifier);
            try
            {
                SqlCommand updateCommand = new SqlCommand(command, mSQL);
                updateCommand.ExecuteNonQuery();
                ret = "Updated DB";
            }
            catch
            {
                ret = "Error updating DB";
                LogFile.LogError(String.Format("Error updating database: {0}", command));
            }
            return ret;
        }

        public bool Exists(string table, string colID, string query, string colID2 = null, int query2 = 0)
        {
            bool exists = false;
            try
            {
                string searcher = String.Format("select * from {0} where {1}='{2}'", table, colID, query);
                if (colID2 != null && query2 > 0)
                {
                    searcher += String.Format(" and {0}={1}", colID2, query2);
                }

                SqlDataReader reader = Query(searcher);
                if (reader != null)
                {
                    if (reader.Read())
                    {
                        //string stat = String.Format("Entry already exists in {0} DB for {1}", table, query);
                        //if (colID2 != null && query2 > 0)
                        //{
                        //    stat += String.Format(" - {0}", query2);
                        //}
                        //stat += "\r\n";

                        //AppendLabelText(stat);

                        exists = true;
                    }
                    reader.Close();
                }
            }
            catch (Exception e)
            {
                LogFile.LogError(String.Format("ERROR: DB Exists(5), {0}", e));
            }

            return exists;
        }

        public bool Exists(string table, string colID, int query)
        {
            bool exists = false;

            try
            {
                string searcher = String.Format("select * from {0} where {1}={2}", table, colID, query);

                SqlDataReader reader = Query(searcher);
                if (reader != null)
                {
                    if (reader.Read())
                    {
                        exists = true;
                    }
                    reader.Close();
                }
            }
            catch (Exception e)
            {
                LogFile.LogError(String.Format("ERROR: DB Exists(3), {0}", e));
            }

            return exists;
        }

        public List<string> FindUniqueStrings(string table, string column, string project = null)
        {
            List<string> ret = new List<string>();

            string query = null;
            if (project != null)
            {
                string[] projectParts = project.Split('.');
                project = projectParts[0];
                string[] subprojects = null;
                if (projectParts.Length > 1)
                {
                    subprojects = new string[projectParts.Length - 1];
                    for (int i = 0; i < projectParts.Length - 1; i++)
                        subprojects[i] = projectParts[i + 1];
                }

                query = String.Format("select distinct {0} from {1} where PROJECT='{2}'", column, table, project);

                if (subprojects != null)
                {
                    query += " and ";
                    for (int i = 0; i < subprojects.Length; i++)
                    {
                        if (i > 0)
                            query += " or ";
                        query += String.Format("{0}='{1}'", "SUBPROJECT", subprojects[i]);
                    }
                }
            }
            else
                query = String.Format("select distinct {0} from {1} order by {2}", column, table, column);


            SqlDataReader reader = Query(query);
            if (reader != null)
            {
                while (reader.Read())
                {
                    string val = ((string)reader[0]).Trim();
                    if (!ret.Contains(val))
                        ret.Add(val);
                }
                reader.Close();
            }

            return ret;
        }

        public List<int> FindUniqueInts(string table, string column, string project = null)
        {
            List<int> ret = new List<int>();

            string query = null;
            if (project != null)
            {
                string[] projectParts = project.Split('.');
                project = projectParts[0];
                string[] subprojects = null;
                if (projectParts.Length > 1)
                {
                    subprojects = new string[projectParts.Length - 1];
                    for (int i = 0; i < projectParts.Length - 1; i++)
                        subprojects[i] = projectParts[i + 1];
                }

                query = String.Format("select distinct {0} from {1} where PROJECT='{2}'", column, table, project);

                if (subprojects != null)
                {
                    query += " and ";
                    for (int i = 0; i < subprojects.Length; i++)
                    {
                        if (i > 0)
                            query += " or ";
                        query += String.Format("{0}='{1}'", "SUBPROJECT", subprojects[i]);
                    }
                }
            }
            else
                query = String.Format("select distinct {0} from {1} order by {2}", column, table, column);

            SqlDataReader reader = Query(query);
            if (reader != null)
            {
                while (reader.Read())
                {
                    ret.Add((int)reader[0]);
                }
                reader.Close();
            }

            return ret;
        }

        public List<float> FindUniqueFloats(string table, string column, string project = null)
        {
            List<float> ret = new List<float>();

            string query = null;
            if (project != null)
            {
                string[] projectParts = project.Split('.');
                project = projectParts[0];
                string[] subprojects = null;
                if (projectParts.Length > 1)
                {
                    subprojects = new string[projectParts.Length - 1];
                    for (int i = 0; i < projectParts.Length - 1; i++)
                        subprojects[i] = projectParts[i + 1];
                }

                query = String.Format("select distinct {0} from {1} where PROJECT='{2}'", column, table, project);

                if (subprojects != null)
                {
                    query += " and ";
                    for (int i = 0; i < subprojects.Length; i++)
                    {
                        if (i > 0)
                            query += " or ";
                        query += String.Format("{0}='{1}'", "SUBPROJECT", subprojects[i]);
                    }
                }
            }
            else
                query = String.Format("select distinct {0} from {1} order by {2}", column, table, column);

            SqlDataReader reader = Query(query);
            if (reader != null)
            {
                while (reader.Read())
                {
                    ret.Add((float)(double)reader[0]);
                }
                reader.Close();
            }

            return ret;
        }

        public SqlDataReader Query(string table, string colID, string query)
        {
            return Query(String.Format("select * from {0} where {1}='{2}'", table, colID, query));
        }

        public SqlDataReader Query(string query)
        {
            SqlDataReader rdr = null;

            try
            {
                SqlCommand cmd = new SqlCommand(query, mSQL);
                rdr = cmd.ExecuteReader();
            }
            catch (Exception e)
            {
                LogFile.LogError(String.Format("ERROR: DB Query(1), {0}", e));
            }

            return rdr;
        }


        public int GetNextKey(string table, string keyRow)
        {
            int lastID = 0;
            List<int> ids = FindUniqueInts(table, keyRow);
            foreach (int id in ids)
            {
                if (id > lastID)
                    lastID = id;
            }
            return lastID + 1;
        }

        public void AddGame(TwodokuGameInfo gameInfo)
        {
        }

        public bool UpdateGame(TwodokuGameInfo gameInfo)
        {
            return false;
        }
    }
}
