package com.viggi.lib_file;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Dave on 2/22/2015.
 */
public class LogFile
{
    public String LogFilename = "/sdcard/00Logs/DaveLog.txt";
    private List<LogEntry> LogEntries = null;

    public LogFile(String filename, boolean loadNow)
    {
        LogFilename = filename;

        if(loadNow)
            Load();
    }

    private void Load()
    {
        try
        {
            File f = new File(LogFilename);
            BufferedReader br = new BufferedReader(new FileReader(f));
            String log = "";
            char[] buf = new char[(int)f.length()];
            br.read(buf, 0, (int)f.length());
            br.close();
            log = String.copyValueOf(buf);
            String[] logEntries = log.split("\n");

            //Convert the String array to a List<LogEntry>
            LogEntries = new ArrayList<LogEntry>();
            for(int i=0; i<logEntries.length; i++)
            {
                String[] parts = logEntries[i].split(" - ");
                if(parts != null)
                {
                    String type = parts[1];
                    String state = null;
                    String[] split = parts[1].split(" ");
                    if(split.length > 1)
                    {
                        type = split[0];
                        state = split[1].trim();
                    }

                    String comment = null;
                    if(parts.length >= 3)
                        comment = parts[2];
                    LogEntries.add(new LogEntry(parts[0], type, state, comment));
                }
                else
                    LogEntries.add(new LogEntry("-", "-", null, null));
            }
        }
        catch(Exception e)
        {
            Log.e("LogFile", "Failed to load log file");
        }
    }

    public List<LogEntry> GetLogEntries()
    {
        if(LogEntries == null)
            Load();

        if(LogEntries == null)
            LogEntries = new ArrayList<LogEntry>();

        return LogEntries;
    }

    public Map<String, Integer> GetNumEntriesPerType(LoggerConfig config)
    {
        int numButtons = config.Buttons.size();
        int numToggles = config.Toggles.size();
        int[] ret = new int[numButtons + numToggles];

        for(int i=0; i<GetLogEntries().size(); i++)
        {
            int index = GetLogEntries().get(i).GetId(config);
            if(index < 0)
            {
                index = GetLogEntries().get(i).GetToggleId(config);
                if(index >=0 )
                    index += config.Buttons.size();
            }
            if(index >= 0 && index < ret.length)
                ret[index]++;
        }

        Map<String, Integer> dict = new HashMap<String, Integer>();
        for(int i=0; i<numButtons; i++)
            dict.put(config.Buttons.get(i), ret[i]);
        for(int i=0; i<numToggles; i++)
            dict.put(config.Toggles.get(i), ret[numButtons + i]);

        return dict;
    }

    public List<LogEntry> ExtractLog(String category, LoggerConfig config, String filter, List<Boolean> dayFilters)
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

        boolean isToggle = true;
        int searchIndex = config.Toggles.indexOf(category);
        if(searchIndex < 0)
        {
            isToggle = false;
            searchIndex = config.Buttons.indexOf(category);
        }

        //Populate the output array
        List<LogEntry> output = new ArrayList<LogEntry>();
        for(int i=0; i<GetLogEntries().size(); i++)
        {
            boolean addLine = true;
            //Filter for desired searchIndex
            if(searchIndex >=0)
            {
                int index = GetLogEntries().get(i).GetId(config);
                if(isToggle)
                    index = GetLogEntries().get(i).GetToggleId(config);
                addLine = index == searchIndex;
            }

            //Filter by day-of-week
            if(addLine && dayFilteringEnabled)
            {
                //1=Sunday, 7=Saturday
                int dayOfWeek = (GetLogEntries().get(i).GetDate().get(Calendar.DAY_OF_WEEK) + 5) % 7;

                if(!dayFilters.get(dayOfWeek))
                {
                    addLine = false;

                    //if(keepNextOff && GetLogEntries(.get(i).GetToggleState() != null && GetLogEntries(.get(i).GetToggleState().equals("off"))
                    //{
                    //    addLine = true;
                    //    //TODO: Would probably be better to add an artificial "off" entry at midnight of the "on" date
                    //}
                }
            }

            //Filter by comment ("on" entries only for toggles)
            if(addLine)
            {
                if(filter == null || (GetLogEntries().get(i).GetComment() != null && GetLogEntries().get(i).GetComment().contains(filter)) || (isToggle && GetLogEntries().get(i).GetToggleState() == "off"))
                {
                    output.add(GetLogEntries().get(i));
                }
            }
        }
        return output;
    }

    public static float[] ExtractDailyTotals(List<LogEntry> allEntries, String category, Calendar startDate, LoggerConfig config, String filter, List<Boolean> dayFilters)
    {
        int catIndex = config.Toggles.indexOf(category);
        if(catIndex >= 0)
            return LogEntry.ExtractDailyToggleTotals(allEntries, catIndex, startDate, config, filter, dayFilters);
        else
        {
            catIndex = config.Buttons.indexOf(category);
            return LogEntry.ExtractDailyEventTotals(allEntries, catIndex, startDate, config, filter, dayFilters);
        }
    }

    public float[] GetHistogram(List<LogEntry> entries, String timeScale)
    {
        int histSize = GetNumEntriesForHistogramTimeScale(timeScale);
        float[] hist = new float[histSize];

        for(int i=0; i<entries.size(); i++)
        {
            LogEntry entry = entries.get(i);

            int value = GetDataValueForHistogramTimeScale(timeScale, entry.GetDate());


            hist[value]++;
        }

        //Convert the counts to percentages
        if(entries.size() > 0)
            for(int i=0; i<hist.length; i++)
                hist[i] = hist[i] / entries.size() * 100;

        return hist;
    }


    public static int GetNumEntriesForHistogramTimeScale(String timeScale)
    {
        //{"Hour of day", "Day of week", "Day of month", "Day of year", "Week of year", "Month of year"};
        if(timeScale.equals("Hour of day"))
            return 24;
        if(timeScale.equals("Day of week"))
            return 7;
        if(timeScale.equals("Day of month"))
            return 31;
        if(timeScale.equals("Day of year"))
            return 366;
        if(timeScale.equals("Week of year"))
            return 53;
        if(timeScale.equals("Month of year"))
            return 12;

        return 0;
    }

    public static int GetDataValueForHistogramTimeScale(String timeScale, Calendar entryDate)
    {
        //{"Hour of day", "Day of week", "Day of month", "Day of year", "Week of year", "Month of year"};
        if (timeScale.equals("Hour of day"))
            return entryDate.get(Calendar.HOUR_OF_DAY);
        if (timeScale.equals("Day of week"))
            return entryDate.get(Calendar.DAY_OF_WEEK) % 7;
        if (timeScale.equals("Day of month"))
            return entryDate.get(Calendar.DAY_OF_MONTH) - 1;
        if (timeScale.equals("Day of year"))
            return entryDate.get(Calendar.DAY_OF_YEAR) - 1;
        if (timeScale.equals("Week of year"))
            return entryDate.get(Calendar.WEEK_OF_YEAR) - 1;
        if (timeScale.equals("Month of year"))
            return entryDate.get(Calendar.MONTH);

        return 0;
    }

    public String GetStats(String category, LoggerConfig config, String filter, List<Boolean> dayFilters)
    {
        String stats = "";

        boolean isToggle = config.Toggles.indexOf(category) >= 0;
        String label = "";
        int multiplier = 1;
        if(isToggle)
        {
            label = " hours";
            multiplier = 2;
        }

        List<LogEntry> subset = ExtractLog(category, config, filter, dayFilters);
        if(subset == null)
            return stats;
        stats += category + ":\n";
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
        stats += String.format("Total: %d entries (%.02f/day, %d/year)\n", subsetSize/multiplier, dailyAverage/multiplier, (int)(dailyAverage * 365/multiplier));

        if(isToggle)
        {
            //Total time stats
            long totalTime = 0;
            Calendar onDate = null;
            for(int j=0; j<subsetSize; j++)
            {
                LogEntry curEntry = subset.get(j);
                if(subset.get(j).GetToggleState().equals("on"))
                {
                    onDate = curEntry.GetDate();
                }
                else if(onDate != null)
                {
                    totalTime += curEntry.GetDate().getTimeInMillis() - onDate.getTimeInMillis();
                    onDate = null;
                }
            }
            if(onDate != null)
                totalTime += now.getTimeInMillis() - onDate.getTimeInMillis();
            float totalHours = (float)totalTime / 1000 / 3600;
            stats += String.format("Time: %s (%.02f hrs/day, %d hrs/yr)\n", DateStrings.GetElapsedTimeString(totalTime, 3), totalHours/elapsedDays, (int)(totalHours/elapsedDays * 365));
        }

        //Calculate the data
        Calendar startDate = Calendar.getInstance();
        float[] dailyTotals = ExtractDailyTotals(GetLogEntries(), category, startDate, config, filter, dayFilters);
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

        long minOff = Long.MAX_VALUE;
        long maxOff = Long.MIN_VALUE;
        Calendar minOffDate = Calendar.getInstance();
        Calendar maxOffDate = Calendar.getInstance();
        long minOn = Long.MAX_VALUE;
        long maxOn = Long.MIN_VALUE;
        Calendar minOnDate = Calendar.getInstance();
        Calendar maxOnDate = Calendar.getInstance();

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
                if(isToggle)
                {
                    long interval = curDate.getTimeInMillis() - lastDate.getTimeInMillis();
                    if(subset.get(i).ToggleState.equals("off"))
                    {
                        if(interval < minOn)
                        {
                            minOn = interval;
                            minOnDate = lastDate;
                        }
                        if(interval > maxOn)
                        {
                            maxOn = interval;
                            maxOnDate = lastDate;
                        }
                    }
                    else
                    {
                        if(interval < minOff)
                        {
                            minOff = interval;
                            minOffDate = lastDate;
                        }
                        if(interval > maxOff)
                        {
                            maxOff = interval;
                            maxOffDate = lastDate;
                        }
                    }
                }
                else
                {
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
                }
            }

            lastDate = curDate;
        }

        stats += "\n";
        stats += String.format("Least/day: %.02f%s (%s)\nMost/day: %.02f%s (%s)\n", minTotal, label, DateStrings.GetDateString(minTotalDate),
                maxTotal, label, DateStrings.GetDateString(maxTotalDate));
        stats += "\n";

        if(isToggle)
        {
            stats += String.format("Shortest off: %s (%s)\nLongest off: %s (%s)\n", DateStrings.GetElapsedTimeString(minOff, 3), DateStrings.GetDateTimeString(minOffDate),
                    DateStrings.GetElapsedTimeString(maxOff, 3), DateStrings.GetDateTimeString(maxOffDate));
            stats += String.format("Shortest on: %s (%s)\nLongest on: %s (%s)\n", DateStrings.GetElapsedTimeString(minOn, 3), DateStrings.GetDateTimeString(minOnDate),
                    DateStrings.GetElapsedTimeString(maxOn, 3), DateStrings.GetDateTimeString(maxOnDate));
        }
        else
        {
            stats += String.format("Shortest interval: %s (%s)\nLongest interval: %s (%s)\n",
                    DateStrings.GetElapsedTimeString(minInterval, 3), DateStrings.GetDateTimeString(minIntervalDate),
                    DateStrings.GetElapsedTimeString(maxInterval, 3), DateStrings.GetDateTimeString(maxIntervalDate));
        }

        stats += "\n";
        stats += String.format("All-time average:\nMin: %.02f%s/day (%s)\nNow: %.02f%s/day\nMax: %.02f%s/day (%s)\n", minAve, label, DateStrings.GetDateString(minAveDate), allAve[allAve.length - 1], label,
                maxAve, label, DateStrings.GetDateString(maxAveDate));
        stats += "\n";
        stats += String.format("Running average:\nMin: %.02f%s/day (%s)\nNow: %.02f%s/day\nMax: %.02f%s/day (%s)\n", minRunning, label, DateStrings.GetDateString(minRunningDate), runningAve[runningAve.length - 1], label,
                maxRunning, label, DateStrings.GetDateString(maxRunningDate));

        if(!isToggle)
        {
            int index = config.Buttons.indexOf(category);
            if(index >= 0 && config.ButtonValues.get(index))
            {
                List<LogEntry> entries = ExtractLog(category, config, filter, dayFilters);
                float[] values = new float[entries.size()];
                for(int i=0; i<entries.size(); i++)
                {
                    try
                    {
                        values[i] = Float.parseFloat(entries.get(i).GetComment());
                    }
                    catch(Exception e)
                    { }
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
                stats += String.format("Least: %.02f (%s)\nMost: %.02f (%s)\n", minTotal, DateStrings.GetDateString(minTotalDate),
                        maxTotal, DateStrings.GetDateString(maxTotalDate));
                stats += "\n";
                stats += String.format("All-time average:\nMin: %.02f (%s)\nNow: %.02f\nMax: %.02f (%s)\n", minAve, DateStrings.GetDateString(minAveDate), allAve[allAve.length - 1],
                        maxAve, DateStrings.GetDateString(maxAveDate));
                stats += "\n";
                stats += String.format("Running average:\nMin: %.02f (%s)\nNow: %.02f\nMax: %.02f (%s)", minRunning, DateStrings.GetDateString(minRunningDate), runningAve[runningAve.length - 1],
                        maxRunning, DateStrings.GetDateString(maxRunningDate));
            }
        }

        return stats;
    }

    public String GetAllTimeStats()
    {
        String stats = "";
        Calendar now = Calendar.getInstance();
        Calendar firstDate = GetLogEntries().get(0).GetDate();
        String elapsed = DateStrings.GetElapsedTimeString(firstDate, now, 10);

        stats += String.format("First log entry: %s (%s ago)\n", DateStrings.GetPrintableDateString(firstDate), elapsed);
        stats += String.format("Total: %d entries\n", GetLogEntries().size());

        return stats;
    }

    public String GetCommentSummary(String category, LoggerConfig config, String filter, List<Boolean> dayFilters)
    {
        List<LogEntry> subset = null;
        int catIndex = config.Toggles.indexOf(category);
        if(catIndex >= 0)
            subset = LogEntry.ExtractToggleLog(GetLogEntries(), catIndex, config, filter, dayFilters);
        else
        {
            catIndex = config.Buttons.indexOf(category);
            subset = LogEntry.ExtractEventLog(GetLogEntries(), catIndex, config, filter, dayFilters);
        }

        //Build the dictionary of comment instances
        List<String> comments = new ArrayList<String>();
        List<Integer> occurrences = new ArrayList<Integer>();
        for(int i=0; i<subset.size(); i++)
        {
            LogEntry entry = subset.get(i);
            if(entry.GetComment() != null && entry.GetComment().length() > 0)
            {
                int index = comments.indexOf(entry.GetComment());
                if(index < 0)
                {
                    comments.add(entry.GetComment());
                    occurrences.add(1);
                }
                else
                {
                    occurrences.set(index, occurrences.get(index) + 1);
                }
            }
        }

        //Extract the entries from most-to-least occurring
        String result = "";
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

            result += String.format("%d: %s\n", occurrences.get(mostIndex), comments.get(mostIndex));

            comments.remove(mostIndex);
            occurrences.remove(mostIndex);
        }

        return result;
    }

    public void ExportLog(LoggerConfig config)
    {
        String directory = config.ExportDirectory;
        String date = DateStrings.GetDateTimeString(Calendar.getInstance());

        String filename = String.format("%sDaveLog_%s.txt", directory, date);

        ExportLog(filename);
    }

    public void ExportLog(String filename)
    {
        try
        {
            FileWriter fw = new FileWriter(filename, false);

            for(int i=0; i<GetLogEntries().size(); i++)
            {
                fw.write(GetLogEntries().get(i).GetEntryString() + "\n");
            }

            fw.close();
        }
        catch(Exception e) {}
    }

    public void EmailLog(Context context, LoggerConfig config)
    {
        String directory = config.ExportDirectory;
        String date = DateStrings.GetDateTimeString(Calendar.getInstance());
        String filename = String.format("%sDaveLog_%s.txt", directory, date);

        ExportLog(filename);

        String body = "Log attached";

        final Intent emailIntent = new Intent(Intent.ACTION_SEND);
        emailIntent.setType("plain/text");
        emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{ config.EmailAddress });
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, config.EmailAutoSubject);
        emailIntent.putExtra(Intent.EXTRA_TEXT, body);
        emailIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://" + filename));

        context.startActivity(Intent.createChooser(emailIntent, "Send mail..."));
    }

    public void WriteLog(String log)
    {
        try
        {
            FileWriter fw = new FileWriter(LogFilename, false);
            fw.write(log);
            fw.close();
        }
        catch (Exception e) {}
    }

    public void AddLogEntry(Calendar curDate, int index, String state, String comment, LoggerConfig config)
    {
        //Find button or toggle
        String label = null;
        if(index < 1000)
            label = config.Buttons.get(index);
        else
            label = config.Toggles.get(index-1000);

        //Add LogEntry to log file in memory
        if(comment != null)
            comment = comment.trim();

        try
        {
            //Write entry to log file
            FileWriter fw = new FileWriter(LogFilename, true);
            fw.append(DateStrings.GetDateTimeString(curDate) + " - " + label);
            if(state != null)
            {
                fw.append(" " + state);
            }
            if(comment != null)
                fw.append(" - " + comment);
            fw.append("\n");
            fw.close();
        }
        catch(IOException e)
        {
            Log.e("LogFile", "Failed to write log entry");
        }

        if(LogEntries != null)
            LogEntries.add(new LogEntry(DateStrings.GetDateTimeString(curDate), label, state, comment));
    }

}
