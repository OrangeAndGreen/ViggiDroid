using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace TwodokuServer
{
    public class DateStrings
    {
        public static string ToString(DateTime ts)
        {
            return string.Format("{0:0000}{1:00}{2:00}_{3:00}{4:00}{5:00}", ts.Year, ts.Month, ts.Day, ts.Hour, ts.Minute, ts.Second);
        }

        public static DateTime FromString(string ts)
        {
            int year = 0;
            int.TryParse(ts.Substring(0, 4), out year);

            int month = 0;
            int.TryParse(ts.Substring(4, 2), out month);

            int day = 0;
            int.TryParse(ts.Substring(6, 2), out day);

            int hour = 0;
            int.TryParse(ts.Substring(9, 2), out hour);

            int minute = 0;
            int.TryParse(ts.Substring(11, 2), out minute);

            int second = 0;
            int.TryParse(ts.Substring(13, 2), out second);

            return new DateTime(year, month, day, hour, minute, second);
        }
    }
}
