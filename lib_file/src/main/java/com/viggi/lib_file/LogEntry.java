package com.viggi.lib_file;

import com.viggi.lib_datestring.DateStrings;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Created by Dave on 2/22/2015.
 * Holds a single log entry
 */
public class LogEntry
{
    private String Date = null;
    private String Type = null;
    private String Comment = null;
    private Calendar mDate = null;
    public String ToggleState = null;

    public LogEntry(String inDate, String inType, String inState, String inComment)
    {
        Date = inDate;
        Type = inType;
        ToggleState = inState;
        Comment = inComment;
    }

    public String GetEntryString()
    {
        String ret = Date + " - " + Type;
        if(ToggleState != null)
            ret += " " + ToggleState;
        if(Comment != null)
            ret += " - " + Comment;
        return ret;
    }

    public Calendar GetDate()
    {
        if(mDate == null)
            mDate = DateStrings.ParseDateTimeString(Date);
        return mDate;
    }

//    public void SetDate(String date)
//    {
//        Date = date;
//        mDate = null;
//    }
//
//    public String GetDateString()
//    {
//        return Date;
//    }

    public String GetToggleState()
    {
        //if(ToggleState == null)
        //{
        //	GetToggleId();

        //}
        //if(ToggleState == null)
        //	return "";
        return ToggleState;
    }

    public String GetType()
    {
        return Type;
    }

//    public void SetType(String type)
//    {
//        Type = type;
//        //mIndex = -1;
//    }

    public String GetComment()
    {
        return Comment;
    }

//    public void SetComment(String inComment)
//    {
//        Comment = inComment;
//    }

    public static List<LogEntry> ExtractEventLog(List<LogEntry> allEntries, String name, String filter, List<Boolean> dayFilters)
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
        List<LogEntry> output = new ArrayList<>();
        for(int i=0; i<allEntries.size(); i++)
        {
            boolean addLine = true;
            if(name != null)
            {
                addLine = name.equals(allEntries.get(i).GetType());
            }

            if(dayFilteringEnabled)
            {
                //1=Sunday, 7=Saturday
                int dayOfWeek = (allEntries.get(i).GetDate().get(Calendar.DAY_OF_WEEK) + 5) % 7;

                if(!dayFilters.get(dayOfWeek))
                {
                    addLine = false;
                }
            }

            if(addLine && (filter == null || (allEntries.get(i).Comment != null && allEntries.get(i).Comment.toLowerCase().contains(filter.toLowerCase()))))
                output.add(allEntries.get(i));
        }
        return output;
    }

    public static float[] ExtractDailyEventTotals(List<LogEntry> allEntries, String name, Calendar startDate, int midnightHour, String filter, List<Boolean> dayFilters)
    {
        List<LogEntry> entries = ExtractEventLog(allEntries, name, filter, dayFilters);
        List<Float> countsPerDay = new ArrayList<>();
        Calendar lastDate = null;
        int dayCount = 0;
        if(entries.size() > 0)
        {
            startDate.setTimeInMillis(entries.get(0).GetDate().getTimeInMillis());
        }
        for(int j=0; j<entries.size(); j++) //for each date
        {
            Calendar curDate = entries.get(j).GetDate();
            if(curDate == null)
                continue;

            if(lastDate != null && !DateStrings.SameDay(lastDate, curDate, midnightHour))
            {
                //This entry starts a new day
                //Add entries for every day between the last entry and this entry
                int days = DateStrings.GetActiveDiffInDays(lastDate, curDate, midnightHour);
                for(int k=0; k<days; k++)
                {
                    //Add the current count for the end of the previous day,
                    //then any zero-days to catch up to the current day
                    countsPerDay.add((float)dayCount);
                    dayCount = 0;
                }
                dayCount = 1;
            }
            else
                dayCount++; //Increment the counter for the current day

            lastDate = curDate;
        }

        if(lastDate != null)
        {
            //Add entries for every day between the last entry and now
            int days = DateStrings.GetActiveDiffInDays(lastDate, Calendar.getInstance(), midnightHour) + 1;
            for(int d=0; d<days; d++)
            {
                countsPerDay.add((float)dayCount);
                dayCount = 0;
            }
        }

        //Copy List<float> to DailyCounts[typeIndex][]
        float[] ret = new float[countsPerDay.size()];
        for(int q=0; q<countsPerDay.size(); q++)
            ret[q] = countsPerDay.get(q);

        return ret;
    }

    public static List<LogEntry> ExtractToggleLog(List<LogEntry> allEntries, String name, String filter, List<Boolean> dayFilters)
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

        //This is used to keep the next "off" after keeping a filtered "on"
        boolean keepNextOff = false;
        List<LogEntry> output = new ArrayList<>();
        for(int i=0; i<allEntries.size(); i++)
        {
            boolean addLine = true;
            if(name != null)
            {
                addLine = name.equals(allEntries.get(i).GetType());
            }

            if(dayFilteringEnabled)
            {
                //1=Sunday, 7=Saturday
                int dayOfWeek = (allEntries.get(i).GetDate().get(Calendar.DAY_OF_WEEK) + 5) % 7;

                if(!dayFilters.get(dayOfWeek))
                {
                    addLine = false;

                    if(keepNextOff && allEntries.get(i).GetToggleState() != null && allEntries.get(i).GetToggleState().equals("off"))
                    {
                        addLine = true;
                        //TODO: Would probably be better to add an artificial "off" entry at midnight of the "on" date
                    }
                }
            }

            if(addLine)
            {
                addLine = false;
                if(filter == null)
                {
                    addLine = true;
                }
                else if(keepNextOff && allEntries.get(i).GetToggleState() != null && allEntries.get(i).GetToggleState().equals("off"))
                {
                    addLine = true;
                    keepNextOff = false;
                }
                else if(allEntries.get(i).Comment != null && allEntries.get(i).Comment.trim().toLowerCase().contains(filter.trim().toLowerCase()))
                {
                    addLine = true;
                    keepNextOff = true;
                }

                if(addLine)
                {
                    output.add(allEntries.get(i));
                }
            }
        }
        return output;
    }

    public static float[] ExtractDailyToggleTotals(List<LogEntry> allEntries, String name, Calendar startDate, int midnightHour, String filter, List<Boolean> dayFilters)
    {
        List<LogEntry> subset = ExtractToggleLog(allEntries, name, filter, dayFilters);
        int subsetSize = subset.size();
        LogEntry lastEntry = null;
        if(subsetSize > 0)
            lastEntry = subset.get(subsetSize - 1);
        if(lastEntry != null && lastEntry.ToggleState != null && lastEntry.ToggleState.equals("on"))
        {
            //Add an artificial "off" with now as the time, so the last toggle "on" gets included
            subset.add(new LogEntry(DateStrings.GetDateTimeString(Calendar.getInstance()),
                    lastEntry.GetType(), "off", null));
            subsetSize++;
        }

        Calendar onDate = null;
        Calendar endDate;
        List<Float> totals = new ArrayList<>();
        if(subsetSize > 1)
        {
            //Sort through the toggles and calculate hours/day
            startDate.setTimeInMillis(subset.get(0).GetDate().getTimeInMillis());
            endDate = (Calendar)startDate.clone();
            float dayTotal = 0;
            for(int i=0; i<subsetSize; i++)
            {
                Calendar curDate = subset.get(i).GetDate();
                if(subset.get(i).GetToggleState().equals("on"))
                {
                    //Turned a toggle on
                    onDate = curDate;
                    int days = DateStrings.GetActiveDiffInDays(endDate, curDate, midnightHour);
                    for(int d=0; d<days; d++)
                    {
                        totals.add(dayTotal);
                        dayTotal = 0;
                    }
                }
                else if(onDate != null)
                {
                    //Turned a toggle off
                    int days = DateStrings.GetActiveDiffInDays(onDate, curDate, midnightHour);
                    float totalElapsed = (curDate.getTimeInMillis() - onDate.getTimeInMillis()) / (float)3600000;
                    Calendar firstMidnight = (Calendar) onDate.clone();
                    if(firstMidnight.get(Calendar.HOUR_OF_DAY) >= midnightHour)
                    {
                        firstMidnight.add(Calendar.HOUR, 24);
                    }
                    firstMidnight.set(Calendar.HOUR_OF_DAY, midnightHour);
                    firstMidnight.set(Calendar.MINUTE, 0);
                    firstMidnight.set(Calendar.SECOND, 0);

                    //Handles the special case for starting a toggle after 11PM the night before daylight savings ("Spring ahead")
                    if((firstMidnight.getTimeInMillis() - onDate.getTimeInMillis()) / (float)3600000 > 24)
                        firstMidnight.add(Calendar.HOUR, -24);
                    //Handles the special case for daylight savings ("Fall back")
                    if((firstMidnight.getTimeInMillis() - onDate.getTimeInMillis()) / (float)3600000 < 0)
                        firstMidnight.add(Calendar.HOUR, 24);

                    for(int d=0; d<days; d++)
                    {
                        //Add daily totals for every day between the last "on" and this "off"
                        //Could be 0 if this toggle started and ended in the same day
                        if(d==0)
                        {
                            float elapsedTime = (firstMidnight.getTimeInMillis() - onDate.getTimeInMillis()) / (float)3600000;
                            dayTotal += elapsedTime;
                            totals.add(dayTotal);
                            dayTotal = 0;
                            totalElapsed -= elapsedTime;
                        }
                        else
                        {
                            totals.add(24.f);
                            totalElapsed -= 24;
                        }
                    }
                    dayTotal += totalElapsed;
                    endDate = curDate;
                    onDate = null;
                }
            }

            //Add entries from the last "off" date to now
            int days = DateStrings.GetActiveDiffInDays(endDate, Calendar.getInstance(), midnightHour) + 1;
            for(int d=0; d<days; d++)
            {
                if(d==0)
                {
                    totals.add(dayTotal);
                }
                else
                {
                    totals.add(0.0f);
                }
            }
            //Not needed?
            //endDate = Calendar.getInstance();
        }

        //Convert the list of floats to an array for graphing
        float[] values = new float[totals.size()];
        for(int q=0; q<totals.size(); q++)
            values[q] = totals.get(q);

        return values;
    }
}
