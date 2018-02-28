package com.viggi.lib_file;

import java.util.Calendar;

/**
 * Created by dave on 8/28/17.
 */

public class LoggerStateItem
{
    public static final int RecentTotalsHistoryLength = 8;
    public static final int RecentEntriesHistoryLength = 3;

    public String Name = null;

    public boolean IsToggle = false;
    public boolean ToggleState = false;

    public double[] RecentCounts = new double[RecentTotalsHistoryLength];
    public double AllTimeAverage = 0;
    public double RecentAverage = 0;
    public Calendar[] RecentHistory = new Calendar[RecentEntriesHistoryLength];


}
