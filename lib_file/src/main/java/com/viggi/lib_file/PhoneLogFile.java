package com.viggi.lib_file;

import android.util.Log;

import com.viggi.lib_datestring.DateStrings;
import com.viggi.lib_math.ArrayMath;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * Created by dave on 11/15/17.
 */

public class PhoneLogFile
{
    public String LogFilename = null;
    private List<PhoneLogEntry> LogEntries = null;

    public PhoneLogFile(String filename)
    {
        this(filename, false);
    }

    public PhoneLogFile(String filename, boolean loadNow)
    {
        LogFilename = filename;

        if(loadNow)
        {
            Load();
        }
    }

    private void Load()
    {
        try
        {
            File f = new File(LogFilename);
            BufferedReader br = new BufferedReader(new FileReader(f));
            char[] buf = new char[(int)f.length()];
            br.read(buf, 0, (int)f.length());
            br.close();
            String log = String.copyValueOf(buf);
            String[] logEntries = log.split("\n");

            //Convert the String array to a List<PhoneLogEntry>
            LogEntries = new ArrayList<>();
            for(String logEntry : logEntries)
            {
                //Format: date, type: number (name) >> duration
                String[] parts = logEntry.split(" >> ");
                if(parts.length > 1)
                {
                    double duration = Double.parseDouble(parts[1]);
                    parts = parts[0].split(": ");
                    if(parts.length > 1)
                    {
                        String phoneNumber = parts[1];
                        String person = "?";
                        String dateAndType = parts[0];
                        parts = dateAndType.split(", ");
                        if(parts.length > 1)
                        {
                            String dateStr = parts[0];
                            String type = parts[1];

                            parts = phoneNumber.split(" ");
                            if(parts.length > 1)
                            {
                                phoneNumber = parts[0];
                                person = parts[1].substring(1, parts[1].length()-2);
                            }

                            LogEntries.add(new PhoneLogEntry(dateStr, type, phoneNumber, person, duration));
                        }
                    }
                }
            }
        }
        catch(Exception e)
        {
            Log.e("PhoneLogFile", "Failed to load log file");
        }
    }

    public List<PhoneLogEntry> GetLogEntries()
    {
        if(LogEntries == null)
            Load();

        if(LogEntries == null)
            LogEntries = new ArrayList<>();

        return LogEntries;
    }

    public void AddLogEntry(Calendar curDate, int type, String number, String name, String duration)
    {
        PhoneLogEntry entry = new PhoneLogEntry(DateStrings.GetDateTimeString(curDate), PhoneLogFile.callTypeToString(type), number, name, Double.parseDouble(duration));

        try
        {
            //Write entry to log file
            FileWriter fw = new FileWriter(LogFilename, true);
            fw.append(entry.GetEntryString());
            fw.close();
        }
        catch(IOException e)
        {
            Log.e("PhoneLogFile", "Failed to write phone log entry");
        }

        if(LogEntries != null)
            LogEntries.add(entry);
    }

    public static String callTypeToString(int type)
    {
        switch(type)
        {
            case 1:
                return "Incoming";
            case 2:
                return "Outgoing";
            case 3:
                return "Missed";
            case 4:
                return "Voicemail";
            case 5:
                return "Rejected";
            case 6:
                return "Blocked";
            case 7:
                return "External";
            default:
                return "?";
        }
    }

    public List<PhoneLogEntry> ExtractLog(String filter, List<Boolean> dayFilters)
    {
        int numDayFilters = 0;
        for (int i=0; i<dayFilters.size(); i++)
        {
            if(dayFilters.get(i))
            {
                numDayFilters++;
            }
        }
        boolean dayFilteringEnabled = numDayFilters > 0 && numDayFilters < 7;

        //Populate the output array
        List<PhoneLogEntry> output = new ArrayList<>();
        for(int i=0; i<GetLogEntries().size(); i++)
        {
            //Filter for desired searchIndex
            boolean addLine = true;//item != null && GetLogEntries().get(i).GetType().equals(item.Name);

            //Filter by day-of-week
            if(addLine && dayFilteringEnabled)
            {
                //1=Sunday, 7=Saturday
                int dayOfWeek = (GetLogEntries().get(i).GetDate().get(Calendar.DAY_OF_WEEK) + 5) % 7;

                if(!dayFilters.get(dayOfWeek))
                {
                    addLine = false;
                }
            }

            //Filter by comment ("on" entries only for toggles)
            if(addLine)
            {
                if(filter == null || (GetLogEntries().get(i).GetName() != null && GetLogEntries().get(i).GetName().contains(filter)))
                {
                    output.add(GetLogEntries().get(i));
                }
            }
        }

        return output;
    }

    public static float[] ExtractDailyTotals(List<PhoneLogEntry> allEntries, Calendar startDate, int midnightHour, String filter, List<Boolean> dayFilters, boolean timeTotal)
    {
//        if(item.IsToggle)
//        {
//            return LogEntry.ExtractDailyToggleTotals(allEntries, item.Name, startDate, midnightHour, filter, dayFilters);
//        }
//        else
//        {
            return PhoneLogEntry.ExtractDailyTotals(allEntries, startDate, midnightHour, filter, dayFilters, timeTotal);
//        }
    }

    public String GetStats(int midnightHour, String filter, List<Boolean> dayFilters)
    {
        String stats = "";

        String label = "";
        int multiplier = 1;
//        if(item.IsToggle)
//        {
//            label = " hours";
//            multiplier = 2;
//        }

        List<PhoneLogEntry> subset = ExtractLog(filter, dayFilters);
        if(subset == null)
            return stats;
        stats += "Phone calls:\n";
        int subsetSize = subset.size();
        if(subsetSize == 0)
            return stats + "0 logged\n";

        Calendar now = Calendar.getInstance();
        //First entry stats
        Calendar firstDate = subset.get(0).GetDate();
        String elapsed = DateStrings.GetElapsedTimeString(firstDate, now, 3);
        stats += String.format("First entry: %s (%s ago)\n", DateStrings.GetPrintableDateString(firstDate), elapsed);

        //Total entry stats
        float elapsedDays = ((float)(now.getTimeInMillis() - firstDate.getTimeInMillis())) / 1000 / 3600 / 24;
        float dailyAverage = subsetSize/elapsedDays;
        stats += String.format(Locale.getDefault(), "Total: %d entries (%.02f/day, %d/year)\n", subsetSize/multiplier, dailyAverage/multiplier, (int)(dailyAverage * 365/multiplier));

//        if(item.IsToggle)
//        {
//            //Total time stats
//            long totalTime = 0;
//            Calendar onDate = null;
//            for(int j=0; j<subsetSize; j++)
//            {
//                LogEntry curEntry = subset.get(j);
//                if(subset.get(j).GetToggleState().equals("on"))
//                {
//                    onDate = curEntry.GetDate();
//                }
//                else if(onDate != null)
//                {
//                    totalTime += curEntry.GetDate().getTimeInMillis() - onDate.getTimeInMillis();
//                    onDate = null;
//                }
//            }
//            if(onDate != null)
//                totalTime += now.getTimeInMillis() - onDate.getTimeInMillis();
//            float totalHours = (float)totalTime / 1000 / 3600;
//            stats += String.format(Locale.getDefault(), "Time: %s (%.02f hrs/day, %d hrs/yr)\n", DateStrings.GetElapsedTimeString(totalTime, 3), totalHours/elapsedDays, (int)(totalHours/elapsedDays * 365));
//        }

        //Calculate the data
        Calendar startDate = Calendar.getInstance();
        float[] dailyTotals = ExtractDailyTotals(GetLogEntries(), startDate, midnightHour, filter, dayFilters, true);
        float[] allAve = ArrayMath.GetAllTimeRunningAverageCurve(dailyTotals);
        float[] runningAve = ArrayMath.GetRunningAverageCurve(dailyTotals, 30);

        //Define the variables to track
        float minTotal = Float.MAX_VALUE;
        float maxTotal = Float.MIN_VALUE;
        Calendar minTotalDate = Calendar.getInstance();
        Calendar maxTotalDate = Calendar.getInstance();
        float minAve = Float.MAX_VALUE;
        float maxAve = Float.MIN_VALUE;
        Calendar minAveDate = Calendar.getInstance();
        Calendar maxAveDate = Calendar.getInstance();
        float minRunning = Float.MAX_VALUE;
        float maxRunning = Float.MIN_VALUE;
        Calendar minRunningDate = Calendar.getInstance();
        Calendar maxRunningDate = Calendar.getInstance();

        Calendar curDate = (Calendar)startDate.clone();
        for(int i=0; i<dailyTotals.length; i++)
        {
            float curTotal = dailyTotals[i];
            if(curTotal < minTotal)
            {
                minTotal = curTotal;
                minTotalDate = (Calendar)curDate.clone();
            }
            if(curTotal > maxTotal)
            {
                maxTotal = curTotal;
                maxTotalDate = (Calendar)curDate.clone();
            }

            float curAve = allAve[i];
            if(curAve < minAve)
            {
                minAve = curAve;
                minAveDate = (Calendar)curDate.clone();
            }
            if(curAve > maxAve)
            {
                maxAve = curAve;
                maxAveDate = (Calendar)curDate.clone();
            }

            float curRunning = runningAve[i];
            if(curRunning < minRunning)
            {
                minRunning = curRunning;
                minRunningDate = (Calendar)curDate.clone();
            }
            if(curRunning > maxRunning)
            {
                maxRunning = curRunning;
                maxRunningDate = (Calendar)curDate.clone();
            }

            curDate.add(Calendar.HOUR, 24);
        }

//        long minOff = Long.MAX_VALUE;
//        long maxOff = Long.MIN_VALUE;
//        Calendar minOffDate = Calendar.getInstance();
//        Calendar maxOffDate = Calendar.getInstance();
//        long minOn = Long.MAX_VALUE;
//        long maxOn = Long.MIN_VALUE;
//        Calendar minOnDate = Calendar.getInstance();
//        Calendar maxOnDate = Calendar.getInstance();

        long minInterval = Long.MAX_VALUE;
        long maxInterval = Long.MIN_VALUE;
        Calendar minIntervalDate = Calendar.getInstance();
        Calendar maxIntervalDate = Calendar.getInstance();

        Calendar lastDate = null;

        for(int i=0; i<subsetSize; i++)
        {
            curDate = subset.get(i).GetDate();

            if(lastDate != null)
            {
//                if(item.IsToggle)
//                {
//                    long interval = curDate.getTimeInMillis() - lastDate.getTimeInMillis();
//                    if(subset.get(i).ToggleState.equals("off"))
//                    {
//                        if(interval < minOn)
//                        {
//                            minOn = interval;
//                            minOnDate = lastDate;
//                        }
//                        if(interval > maxOn)
//                        {
//                            maxOn = interval;
//                            maxOnDate = lastDate;
//                        }
//                    }
//                    else
//                    {
//                        if(interval < minOff)
//                        {
//                            minOff = interval;
//                            minOffDate = lastDate;
//                        }
//                        if(interval > maxOff)
//                        {
//                            maxOff = interval;
//                            maxOffDate = lastDate;
//                        }
//                    }
//                }
//                else
//                {
                    long interval = curDate.getTimeInMillis() - lastDate.getTimeInMillis();
                    if(interval < minInterval)
                    {
                        minInterval = interval;
                        minIntervalDate = (Calendar)lastDate.clone();
                    }
                    if(interval > maxInterval)
                    {
                        maxInterval = interval;
                        maxIntervalDate = (Calendar)lastDate.clone();
                    }
                    //Use the following debug line when looking for out-of-order dates
                    //if(interval < 0)
                    //        Log.i("LogFile", "Adding interval " + interval + " between " + DateStrings.GetDateTimeString(lastDate) + " and " + DateStrings.GetDateTimeString(curDate));
//                }
            }

            lastDate = curDate;
        }

        stats += "\n";
        stats += String.format(Locale.getDefault(), "Least/day: %.02f%s (%s)\nMost/day: %.02f%s (%s)\n", minTotal, label, DateStrings.GetDateString(minTotalDate),
                maxTotal, label, DateStrings.GetDateString(maxTotalDate));
        stats += "\n";

//        if(item.IsToggle)
//        {
//            stats += String.format("Shortest off: %s (%s)\nLongest off: %s (%s)\n", DateStrings.GetElapsedTimeString(minOff, 3), DateStrings.GetDateTimeString(minOffDate),
//                    DateStrings.GetElapsedTimeString(maxOff, 3), DateStrings.GetDateTimeString(maxOffDate));
//            stats += String.format("Shortest on: %s (%s)\nLongest on: %s (%s)\n", DateStrings.GetElapsedTimeString(minOn, 3), DateStrings.GetDateTimeString(minOnDate),
//                    DateStrings.GetElapsedTimeString(maxOn, 3), DateStrings.GetDateTimeString(maxOnDate));
//        }
//        else
//        {
            stats += String.format("Shortest interval: %s (%s)\nLongest interval: %s (%s)\n",
                    DateStrings.GetElapsedTimeString(minInterval, 3), DateStrings.GetDateTimeString(minIntervalDate),
                    DateStrings.GetElapsedTimeString(maxInterval, 3), DateStrings.GetDateTimeString(maxIntervalDate));
//        }

        stats += "\n";
        stats += String.format(Locale.getDefault(), "All-time average:\nMin: %.02f%s/day (%s)\nNow: %.02f%s/day\nMax: %.02f%s/day (%s)\n", minAve, label, DateStrings.GetDateString(minAveDate), allAve[allAve.length - 1], label,
                maxAve, label, DateStrings.GetDateString(maxAveDate));
        stats += "\n";
        stats += String.format(Locale.getDefault(), "Running average:\nMin: %.02f%s/day (%s)\nNow: %.02f%s/day\nMax: %.02f%s/day (%s)\n", minRunning, label, DateStrings.GetDateString(minRunningDate), runningAve[runningAve.length - 1], label,
                maxRunning, label, DateStrings.GetDateString(maxRunningDate));

//        if(!item.IsToggle)
//        {
            List<PhoneLogEntry> entries = ExtractLog(filter, dayFilters);
            float[] values = new float[entries.size()];
            for(int i=0; i<entries.size(); i++)
            {
                try
                {
                    values[i] = Float.parseFloat(entries.get(i).GetName());
                }
                catch(Exception e)
                {
                    Log.e("LogFile", "Error parsing float while getting LogFile stats");
                }
            }

            allAve = ArrayMath.GetAllTimeRunningAverageCurve(values);
            runningAve = ArrayMath.GetRunningAverageCurve(values, 30);

            minTotal = Float.MAX_VALUE;
            maxTotal = Float.MIN_VALUE;
            minTotalDate = Calendar.getInstance();
            maxTotalDate = Calendar.getInstance();
            minAve = Float.MAX_VALUE;
            maxAve = Float.MIN_VALUE;
            minAveDate = Calendar.getInstance();
            maxAveDate = Calendar.getInstance();
            minRunning = Float.MAX_VALUE;
            maxRunning = Float.MIN_VALUE;
            minRunningDate = Calendar.getInstance();
            maxRunningDate = Calendar.getInstance();
            curDate = (Calendar)startDate.clone();
            for(int i=0; i<values.length; i++)
            {
                float curTotal = values[i];
                if(curTotal < minTotal)
                {
                    minTotal = curTotal;
                    minTotalDate = (Calendar)curDate.clone();
                }
                if(curTotal > maxTotal)
                {
                    maxTotal = curTotal;
                    maxTotalDate = (Calendar)curDate.clone();
                }

                float curAve = allAve[i];
                if(curAve < minAve)
                {
                    minAve = curAve;
                    minAveDate = (Calendar)curDate.clone();
                }
                if(curAve > maxAve)
                {
                    maxAve = curAve;
                    maxAveDate = (Calendar)curDate.clone();
                }

                float curRunning = runningAve[i];
                if(curRunning < minRunning)
                {
                    minRunning = curRunning;
                    minRunningDate = (Calendar)curDate.clone();
                }
                if(curRunning > maxRunning)
                {
                    maxRunning = curRunning;
                    maxRunningDate = (Calendar)curDate.clone();
                }
                curDate.add(Calendar.HOUR, 24);
            }

            stats += "\n";
            stats += "***** Value stats *****\n";
            stats += "\n";
            stats += String.format(Locale.getDefault(), "Least: %.02f (%s)\nMost: %.02f (%s)\n", minTotal, DateStrings.GetDateString(minTotalDate),
                    maxTotal, DateStrings.GetDateString(maxTotalDate));
            stats += "\n";
            stats += String.format(Locale.getDefault(), "All-time average:\nMin: %.02f (%s)\nNow: %.02f\nMax: %.02f (%s)\n", minAve, DateStrings.GetDateString(minAveDate), allAve[allAve.length - 1],
                    maxAve, DateStrings.GetDateString(maxAveDate));
            stats += "\n";
            stats += String.format(Locale.getDefault(), "Running average:\nMin: %.02f (%s)\nNow: %.02f\nMax: %.02f (%s)", minRunning, DateStrings.GetDateString(minRunningDate), runningAve[runningAve.length - 1],
                    maxRunning, DateStrings.GetDateString(maxRunningDate));
//        }

        return stats;
    }

    public String GetCommentSummary(String filter, List<Boolean> dayFilters)
    {
        String result = "";
//        if(item != null)
//        {
            List<PhoneLogEntry> subset;
//            if(item.IsToggle)
//            {
//                subset = LogEntry.ExtractToggleLog(GetLogEntries(), item.Name, filter, dayFilters);
//            }
//            else
//            {
                subset = PhoneLogEntry.ExtractEventLog(GetLogEntries(), filter, dayFilters);
//            }

            //Build the dictionary of comment instances
            List<String> comments = new ArrayList<>();
            List<Integer> occurrences = new ArrayList<>();
            for(int i=0; i<subset.size(); i++)
            {
                PhoneLogEntry entry = subset.get(i);
                if(entry.GetName() != null && entry.GetName().length() > 0)
                {
                    int index = comments.indexOf(entry.GetName());
                    if(index < 0)
                    {
                        comments.add(entry.GetName());
                        occurrences.add(1);
                    }
                    else
                    {
                        occurrences.set(index, occurrences.get(index) + 1);
                    }
                }
            }

            //Extract the entries from most-to-least occurring
            while(comments.size() > 0)
            {
                int mostOccurrences = -1;
                int mostIndex = 0;
                for(int i=0; i<comments.size(); i++)
                {
                    int curOccurrence = occurrences.get(i);
                    if(curOccurrence > mostOccurrences)
                    {
                        mostOccurrences = curOccurrence;
                        mostIndex = i;
                    }
                }

                result += String.format(Locale.getDefault(), "%d: %s\n", occurrences.get(mostIndex), comments.get(mostIndex));

                comments.remove(mostIndex);
                occurrences.remove(mostIndex);
            }
//        }

        return result;
    }
}
