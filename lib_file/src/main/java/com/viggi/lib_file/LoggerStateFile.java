package com.viggi.lib_file;

import android.util.Log;

import com.viggi.lib_datestring.DateStrings;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Created by Dave on 2/22/2015.
 */
public class LoggerStateFile
{
    //Note: If you change this, you'll need to delete the Temp file before running the new app
    // Otherwise it will probably crash, or at least act strangely
    public static final int RecentTotalsHistoryLength = 8;
    public static final int RecentEntriesHistoryLength = 3;

    public String TempFilename = null;
    public Calendar ActiveDate = null;

    public boolean Safe = false;

    public String[] EventNames = null;
    public int[][] EventRecentCounts = null;
    public double[] EventAllTimeAverages = null;
    public double[] EventRecentAverages = null;
    public Calendar[][] EventRecentHistories = null;

    public String[] ToggleNames = null;
    public boolean[] ToggleStates = null;
    public Calendar[] ToggleLastDates = null;

    private LoggerStateFile(String tempFilename)
    {
        TempFilename = tempFilename;
    }

    public static LoggerStateFile FromFile(String filename, LoggerConfig config) throws Exception
    {
        Log.i("LoggerState", "Loading from file");
        LoggerStateFile ret = new LoggerStateFile(filename);

        BufferedReader br = new BufferedReader(new FileReader(ret.TempFilename));

        int numButtons = config.Buttons.size();
        int numToggles = config.Toggles.size();

        ret.EventNames = new String[numButtons];

        ret.EventRecentCounts = new int[numButtons][];
        ret.EventRecentHistories = new Calendar[numButtons][];
        for (int i = 0; i < numButtons; i++)
        {
            ret.EventRecentCounts[i] = new int[RecentTotalsHistoryLength];
            ret.EventRecentHistories[i] = new Calendar[RecentEntriesHistoryLength];
        }

        ret.EventAllTimeAverages = new double[numButtons];
        ret.EventRecentAverages = new double[numButtons];

        //Toggle setup
        ret.ToggleNames = new String[numToggles];
        ret.ToggleStates = new boolean[numToggles];
        ret.ToggleLastDates = new Calendar[numToggles];

        while (true)
        {
            String line = br.readLine();
            if (line == null)
                break;

            String[] parts = line.split(" = ");
            if (parts.length == 0)
                continue;

            if (parts[0].equals("ActiveDate"))
                ret.ActiveDate = DateStrings.ParseDateTimeString(parts[1]);
            if (parts[0].equals("Event"))
            {
                //Break event into parts and find button index
                String[] subparts = parts[1].split(", ");
                int index = config.Buttons.indexOf(subparts[0]);
                if (index < 0)
                    continue;

                //Assign event name and today count
                ret.EventNames[index] = subparts[0];

                //DEFAULT: Name, daily total, prev1, prev2, prev3
                double allTimeAverage = -1;
                double recentAverage = -1;
                if (subparts.length > 5)
                {
                    //Name, (recent totals), all-time average, recent average, (recent dates)

                    //Parse recent daily totals
                    int offset = 1;
                    for (int i = 0; i < RecentTotalsHistoryLength; i++)
                    {
                        ret.EventRecentCounts[index][i] = Integer.valueOf(subparts[offset]);
                        offset++;
                    }

                    //Parse all-time and recent averages
                    allTimeAverage = Double.valueOf(subparts[offset]);
                    offset++;
                    recentAverage = Double.valueOf(subparts[offset]);
                    offset++;

                    //Parse recent entry dates
                    for (int i = 0; i < RecentEntriesHistoryLength; i++)
                    {
                        ret.EventRecentHistories[index][i] = DateStrings.ParseDateTimeString(subparts[offset].trim());
                        offset++;
                    }
                }
                else
                {
                    //THE OLD WAY (for legacy support): Name, daily total, prev1, prev2, prev3
                    ret.EventRecentCounts[index][0] = Integer.valueOf(subparts[1]);
                    for (int i = 0; i < 3; i++)
                        ret.EventRecentHistories[index][i] = DateStrings.ParseDateTimeString(subparts[i + 2].trim());
                }

                ret.EventAllTimeAverages[index] = allTimeAverage;
                ret.EventRecentAverages[index] = recentAverage;
            }
            if (parts[0].equals("Toggle"))
            {
                String[] subparts = parts[1].split(", ");
                int index = config.Toggles.indexOf(subparts[0]);
                if (index < 0)
                    continue;

                ret.ToggleNames[index] = subparts[0];
                ret.ToggleStates[index] = subparts[1].trim().equals("on");
                ret.ToggleLastDates[index] = DateStrings.ParseDateTimeString(subparts[2].trim());
            }
            if (parts[0].equals("Safe"))
            {
                ret.Safe = parts[1].trim().equals("on");
                if (!ret.Safe)
                    ret.Safe = parts[1].trim().equals("true");
            }
        }
        br.close();

        Log.i("LoggerState", String.format("%d buttons, %d toggles", numButtons, numToggles));

        if (!DateStrings.SameDay(Calendar.getInstance(), ret.ActiveDate, config.MidnightHour))
        {
            Log.i("LoggerState", "Starting new day");
            ret.StartNewActiveDay(config);
            ret.Save(config);
        }

        return ret;
    }

    public static LoggerStateFile Create(String filename, List<LogEntry> entries, LoggerConfig config)
    {
        int numButtons = config.Buttons.size();
        int numToggles = config.Toggles.size();

        int numEntries = 0;
        if(entries != null)
            numEntries = entries.size();
        Log.i("LoggerState", String.format("Creating new: %d buttons, %d toggles (%d entries)", numButtons, numToggles, numEntries));

        LoggerStateFile ret = new LoggerStateFile(filename);

        ret.ActiveDate = DateStrings.GetActiveDate(Calendar.getInstance(), config.MidnightHour);

        ret.EventNames = new String[numButtons];
        ret.EventRecentCounts = new int[numButtons][];
        ret.EventAllTimeAverages = new double[numButtons];
        ret.EventRecentAverages = new double[numButtons];
        ret.EventRecentHistories = new Calendar[numButtons][];

        ret.ToggleNames = new String[numToggles];
        ret.ToggleStates = new boolean[numToggles];
        ret.ToggleLastDates = new Calendar[numToggles];

        for(int i=0; i<numButtons; i++)
        {
            ret.EventNames[i] = config.Buttons.get(i);
            ret.EventRecentCounts[i] = new int[RecentTotalsHistoryLength];
            ret.EventRecentHistories[i] = new Calendar[RecentEntriesHistoryLength];

            if(entries != null && entries.size() > 0)
            {

                float[] dailyTotals = LogEntry.ExtractDailyEventTotals(entries, i, entries.get(0).GetDate(), config, null, new ArrayList<Boolean>());
                float total = 0;
                for (int j = 0; j < dailyTotals.length; j++) {
                    total += dailyTotals[j];
                }

                ret.EventAllTimeAverages[i] = total / dailyTotals.length;
                for (int j = 0; j < RecentTotalsHistoryLength; j++) {
                    ret.EventRecentCounts[i][j] = (int) dailyTotals[dailyTotals.length - 1 - j];
                }
            }
        }
        for(int i=0; i<numToggles; i++)
        {
            ret.ToggleNames[i] = config.Toggles.get(i);
        }

        for(int i=0; i<numEntries; i++)
        {
            LogEntry curEntry = entries.get(i);
            int index = curEntry.GetId(config);
            if(index >=0)
            {
                //Update event logger
                ret.UpdateEventHistory(curEntry.GetDate(), index);
            }
            else
            {
                //Update toggle
                index = curEntry.GetToggleId(config);
                if(index >= 0)
                {
                    boolean checked = curEntry.GetToggleState().trim().equals("on");
                    ret.ToggleStates[index] = checked;
                    ret.ToggleLastDates[index] = curEntry.GetDate();
                }
            }
        }
        ret.Save(config);

        return ret;
    }


    public void StartNewActiveDay(LoggerConfig config)
    {
        ActiveDate = DateStrings.GetActiveDate(Calendar.getInstance(), config.MidnightHour);
        Safe = false;

        for(int i=0; i < EventRecentCounts.length; i++)
        {
            for (int j = 0; j < RecentTotalsHistoryLength - 1; j++)
            {
                EventRecentCounts[i][RecentTotalsHistoryLength - 1 - j] = EventRecentCounts[i][RecentTotalsHistoryLength - 2 - j];
            }
            EventRecentCounts[i][0] = 0;
        }
    }

    //Updates the list of 3 most recent entries
    private void UpdateEventHistory(Calendar curDate, int index)
    {
        //Rotate the dates, i.e 1->2, 0->1
        for(int i=0; i<RecentEntriesHistoryLength - 1; i++)
        {
            EventRecentHistories[index][RecentEntriesHistoryLength - i - 1] =  EventRecentHistories[index][RecentEntriesHistoryLength - i - 2];
        }

        //Assign the new 0 date
        EventRecentHistories[index][0] = curDate;
    }

    public void UpdateEvent(int index, Calendar date, LoggerConfig config)
    {
        EventRecentCounts[index][0]++;
        UpdateEventHistory(date, index);
        Save(config);
    }

    public void UpdateToggle(int index, Calendar date, boolean state, LoggerConfig config)
    {
        ToggleStates[index] = state;
        ToggleLastDates[index] = date;
        Save(config);
    }

    public void Save(LoggerConfig config)
    {
        Log.i("LoggerState", "Saving LoggerState");

        try
        {
            FileWriter fw = new FileWriter(TempFilename, false);

            fw.write(String.format("ActiveDate = %s\n", DateStrings.GetDateTimeString(ActiveDate)));
            fw.write(String.format("Safe = %s\n", Safe));

            for(int i=0; i<config.Buttons.size(); i++)
            {
                String totalsString = "";
                for(int j=0; j<RecentTotalsHistoryLength; j++)
                {
                    if(j > 0)
                        totalsString += ", ";

                    totalsString += String.format("%d", EventRecentCounts[i][j]);
                }

                String prevDatesString = "";
                for(int j=0; j<RecentEntriesHistoryLength; j++)
                {
                    if(j > 0)
                        prevDatesString += ", ";

                    prevDatesString += DateStrings.GetDateTimeString(EventRecentHistories[i][j]);
                }

                //Name, daily total, yesterday total, all-time average, recent average, prev1, prev2, prev3
                fw.write(String.format("Event = %s, %s, %f, %f, %s\n",
                        config.Buttons.get(i),
                        totalsString,
                        EventAllTimeAverages[i],
                        EventRecentAverages[i],
                        prevDatesString
                ));
            }

            for(int i=0; i<config.Toggles.size(); i++)
            {
                String toggleState = "off";
                if(ToggleStates[i])
                    toggleState = "on";
                fw.write(String.format("Toggle = %s, %s, %s\n",
                        config.Toggles.get(i),
                        toggleState,
                        DateStrings.GetDateTimeString(ToggleLastDates[i])
                ));
            }

            fw.close();
        }
        catch(Exception e)
        {
            Log.e("LoggerState", "Error saving LoggerState");
        }
    }
}
