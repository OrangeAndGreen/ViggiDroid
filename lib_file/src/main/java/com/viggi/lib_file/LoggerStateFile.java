package com.viggi.lib_file;

import android.util.Log;

import com.viggi.lib_datestring.DateStrings;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * Created by Dave on 2/22/2015.
 * Holds the temporary state information used to make the Logger load faster and quickly display stats
 */
public class LoggerStateFile
{
    //Note: If you change this, you'll need to delete the Temp file before running the new app
    // Otherwise it will probably crash, or at least act strangely
    public static final int RecentTotalsHistoryLength = 8;
    private static final int RecentEntriesHistoryLength = 3;

    private String TempFilename = null;
    private Calendar ActiveDate = null;
    public Calendar LastPhoneCall = null;

    public boolean Safe = false;

    private List<LoggerStateItem> Items = new ArrayList<>();

    private LoggerStateFile(String tempFilename)
    {
        TempFilename = tempFilename;
    }

    public static LoggerStateFile FromFile(String filename, LoggerConfig config) throws Exception
    {
        Log.i("LoggerState", "Loading from file");
        LoggerStateFile ret = new LoggerStateFile(filename);

        BufferedReader br = new BufferedReader(new FileReader(ret.TempFilename));

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
            if (parts[0].equals("LastPhoneCall"))
                ret.LastPhoneCall = DateStrings.ParseDateTimeString(parts[1]);
            if (parts[0].equals("Event"))
            {
                //Break event into parts and find button index
                String[] subparts = parts[1].split(", ");
                LogItem configItem = config.GetEntryByName(subparts[0]);
                if(configItem == null)
                    continue;

                LoggerStateItem stateItem = new LoggerStateItem();
                stateItem.Name = subparts[0];

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
                        stateItem.RecentCounts[i] = Integer.valueOf(subparts[offset]);
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
                        stateItem.RecentHistory[i] = DateStrings.ParseDateTimeString(subparts[offset].trim());
                        offset++;
                    }
                }
                else
                {
                    //THE OLD WAY (for legacy support): Name, daily total, prev1, prev2, prev3
                    stateItem.RecentCounts[0] = Integer.valueOf(subparts[1]);
                    for (int i = 0; i < 3; i++)
                    {
                        stateItem.RecentHistory[i] = DateStrings.ParseDateTimeString(subparts[i + 2].trim());
                    }
                }

                stateItem.AllTimeAverage = allTimeAverage;
                stateItem.RecentAverage = recentAverage;

                ret.Items.add(stateItem);
            }
            if (parts[0].equals("Toggle"))
            {
                String[] subparts = parts[1].split(", ");
                LogItem configItem = config.GetEntryByName(subparts[0]);
                if(configItem == null)
                    continue;

                LoggerStateItem stateItem = new LoggerStateItem();
                stateItem.Name = subparts[0];
                stateItem.IsToggle = configItem.IsToggle;
                stateItem.ToggleState = subparts[1].trim().equals("on");

                double allTimeAverage;
                double recentAverage;
                if (subparts.length > 3)
                {
                    //NEW: Name, State, (recent totals), all-time average, recent average, (recent dates)
                    //Name, (recent totals), all-time average, recent average, (recent dates)

                    //Parse recent daily totals
                    int offset = 2;
                    for (int i = 0; i < RecentTotalsHistoryLength; i++)
                    {
                        stateItem.RecentCounts[i] = Double.valueOf(subparts[offset]);
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
                        stateItem.RecentHistory[i] = DateStrings.ParseDateTimeString(subparts[offset].trim());
                        offset++;
                    }

                    stateItem.AllTimeAverage = allTimeAverage;
                    stateItem.RecentAverage = recentAverage;
                }
                else
                {
                    //THE OLD WAY
                    stateItem.RecentHistory[0] = DateStrings.ParseDateTimeString(subparts[2].trim());
                }

                ret.Items.add(stateItem);
            }
            if (parts[0].equals("Safe"))
            {
                ret.Safe = parts[1].trim().equals("on");
                if (!ret.Safe)
                    ret.Safe = parts[1].trim().equals("true");
            }
        }
        br.close();

        Log.i("LoggerState", String.format("%d items", ret.Items.size()));

        if (!DateStrings.SameDay(Calendar.getInstance(), ret.ActiveDate, config.MidnightHour))
        {
            Log.i("LoggerState", "Starting new day");
            ret.StartNewActiveDay(config);
            ret.Save();
        }

        return ret;
    }

    public static LoggerStateFile Create(String filename, List<LogEntry> entries, LoggerConfig config)
    {
        int numEntries = 0;
        if(entries != null)
            numEntries = entries.size();
        Log.i("LoggerState", String.format("Creating new: %d entries", numEntries));

        LoggerStateFile ret = new LoggerStateFile(filename);

        ret.ActiveDate = DateStrings.GetActiveDate(Calendar.getInstance(), config.MidnightHour);
        ret.LastPhoneCall = Calendar.getInstance();

        for(LogItem configItem : config.Items)
        {
            LoggerStateItem stateItem = new LoggerStateItem();

            stateItem.Name = configItem.Name;
            stateItem.IsToggle = configItem.IsToggle;

            if(entries != null && entries.size() > 0)
            {
                float[] dailyTotals;
                if(configItem.IsToggle)
                {
                    dailyTotals = LogEntry.ExtractDailyToggleTotals(entries, stateItem.Name, entries.get(0).GetDate(), config.MidnightHour, null, new ArrayList<Boolean>());
                }
                else
                {
                    dailyTotals = LogEntry.ExtractDailyEventTotals(entries, stateItem.Name, entries.get(0).GetDate(), config.MidnightHour, null, new ArrayList<Boolean>());
                }

                float total = 0;
                for(float dailyTotal : dailyTotals)
                {
                    total += dailyTotal;
                }

                stateItem.AllTimeAverage = total / dailyTotals.length;
                for (int j = 0; j < RecentTotalsHistoryLength; j++) {
                    stateItem.RecentCounts[j] = dailyTotals[dailyTotals.length - 1 - j];
                }
            }

            ret.Items.add(stateItem);
        }

        for(int i=0; i<numEntries; i++)
        {
            LogEntry curEntry = entries.get(i);
            LoggerStateItem item = ret.GetEntryByName(curEntry.GetType());

            if(item != null)
            {
                if(item.IsToggle)
                {
                    item.ToggleState = curEntry.GetToggleState().trim().equals("on");
                }

                ret.UpdateItemHistory(curEntry.GetDate(), item);
            }
        }
        ret.Save();

        return ret;
    }

    public int NumItems()
    {
        return Items.size();
    }

    private void StartNewActiveDay(LoggerConfig config)
    {
        ActiveDate = DateStrings.GetActiveDate(Calendar.getInstance(), config.MidnightHour);
        Safe = false;
        Calendar curDate = Calendar.getInstance();

        for(LoggerStateItem item : Items)
        {
            //For overnight toggles, add any remaining "on" time
            if(item.IsToggle && item.ToggleState)
            {
                item.RecentCounts[0] += (curDate.getTimeInMillis() - item.RecentHistory[0].getTimeInMillis()) / (float)3600000;
            }

            //Shift the RecentCounts (0 is most recent)
            for (int j = 0; j < RecentTotalsHistoryLength - 1; j++)
            {
                item.RecentCounts[RecentTotalsHistoryLength - 1 - j] = item.RecentCounts[RecentTotalsHistoryLength - 2 - j];
            }

            item.RecentCounts[0] = 0;
        }
    }

    //Updates the list of 3 most recent entries
    private void UpdateItemHistory(Calendar curDate, LoggerStateItem item)
    {
        //Rotate the dates, i.e 1->2, 0->1
        for(int i=0; i<RecentEntriesHistoryLength - 1; i++)
        {
            item.RecentHistory[RecentEntriesHistoryLength - i - 1] =  item.RecentHistory[RecentEntriesHistoryLength - i - 2];
        }

        //Assign the new 0 date
        item.RecentHistory[0] = curDate;
    }

    public void UpdateItem(LoggerStateItem item, Calendar date, int midnightHour)
    {
        if(item.IsToggle)
        {
            if(!item.ToggleState)
            {
                Calendar compareDate = item.RecentHistory[0];
                if(DateStrings.GetActiveDiffInDays(date, item.RecentHistory[0], midnightHour) != 0)
                {
                    //Toggle was on overnight
                    compareDate = (Calendar)date.clone();
                    compareDate.set(Calendar.HOUR_OF_DAY, midnightHour);
                    compareDate.set(Calendar.MINUTE, 0);
                    compareDate.set(Calendar.SECOND, 0);
                }

                item.RecentCounts[0] += (date.getTimeInMillis() - compareDate.getTimeInMillis()) / (float)3600000;
            }
        }
        else
        {
            item.RecentCounts[0]++;
        }

        UpdateItemHistory(date, item);
        Save();
    }

    public void Save()
    {
        Log.i("LoggerState", "Saving LoggerState");

        try
        {
            FileWriter fw = new FileWriter(TempFilename, false);

            fw.write(String.format("ActiveDate = %s\n", DateStrings.GetDateTimeString(ActiveDate)));
            fw.write(String.format("LastPhoneCall = %s\n", DateStrings.GetDateTimeString(LastPhoneCall)));
            fw.write(String.format("Safe = %s\n", Safe));

            for(LoggerStateItem item : Items)
            {
                //TODO LOGITEM: What can be reduced from the cases below?
                if(item.IsToggle)
                {
                    String toggleState = "off";
                    if(item.ToggleState)
                        toggleState = "on";

                    String totalsString = "";
                    for(int j=0; j<RecentTotalsHistoryLength; j++)
                    {
                        if(j > 0)
                            totalsString += ", ";

                        totalsString += String.format(Locale.getDefault(), "%f", item.RecentCounts[j]);
                    }

                    String prevDatesString = "";
                    for(int j=0; j<RecentEntriesHistoryLength; j++)
                    {
                        if(j > 0)
                            prevDatesString += ", ";

                        prevDatesString += DateStrings.GetDateTimeString(item.RecentHistory[j]);
                    }

                    //Name, daily total, yesterday total, all-time average, recent average, prev1, prev2, prev3
                    fw.write(String.format(Locale.getDefault(), "Toggle = %s, %s, %s, %f, %f, %s\n",
                            item.Name,
                            toggleState,
                            totalsString,
                            item.AllTimeAverage,
                            item.RecentAverage,
                            prevDatesString
                    ));
                }
                else
                {
                    String totalsString = "";
                    for(int j=0; j<RecentTotalsHistoryLength; j++)
                    {
                        if(j > 0)
                            totalsString += ", ";

                        totalsString += String.format(Locale.getDefault(), "%d", (int)item.RecentCounts[j]);
                    }

                    String prevDatesString = "";
                    for(int j=0; j<RecentEntriesHistoryLength; j++)
                    {
                        if(j > 0)
                            prevDatesString += ", ";

                        prevDatesString += DateStrings.GetDateTimeString(item.RecentHistory[j]);
                    }

                    //Name, daily total, yesterday total, all-time average, recent average, prev1, prev2, prev3
                    fw.write(String.format(Locale.getDefault(), "Event = %s, %s, %f, %f, %s\n",
                            item.Name,
                            totalsString,
                            item.AllTimeAverage,
                            item.RecentAverage,
                            prevDatesString
                    ));
                }
            }

            fw.close();
        }
        catch(Exception e)
        {
            Log.e("LoggerState", "Error saving LoggerState");
        }
    }

    public LoggerStateItem GetEntryByName(String name)
    {
        for(LoggerStateItem item : Items)
        {
            if(item.Name.equals(name))
            {
                return item;
            }
        }

        return null;
    }
}
