package com.viggi.lib_file;

import com.viggi.lib_datestring.DateStrings;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * Created by dave on 11/15/17.
 */

public class PhoneLogEntry
{
    private String Date = null;
    private String Type = null;
    private String Number = null;
    private String Name = null;
    private double Duration = 0;
    private Calendar mDate = null;

    public PhoneLogEntry(String inDate, String inType, String number, String name, double duration)
    {
        Date = inDate;
        Type = inType;
        Number = number;
        Name = name;
        Duration = duration;
    }

    public String GetEntryString()
    {
        String nameStr = "";
        if(Name != null)
        {
            nameStr = " ("+Name+")";
        }

        return String.format(Locale.getDefault(), "%s, %s: %s%s >> %s\n", DateStrings.GetDateTimeString(GetDate()), Type, Number, nameStr, Duration);
    }

    public Calendar GetDate()
    {
        if(mDate == null)
            mDate = DateStrings.ParseDateTimeString(Date);
        return mDate;
    }

    public String GetType()
    {
        return Type;
    }

    public String GetName()
    {
        return Name;
    }

    public double GetDuration()
    {
        return Duration;
    }

    public static List<PhoneLogEntry> ExtractEventLog(List<PhoneLogEntry> allEntries, String filter, List<Boolean> dayFilters)
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
        for(int i=0; i<allEntries.size(); i++)
        {
            boolean addLine = true;
//            if(name != null)
//            {
//                addLine = name.equals(allEntries.get(i).GetType());
//            }

            if(dayFilteringEnabled)
            {
                //1=Sunday, 7=Saturday
                int dayOfWeek = (allEntries.get(i).GetDate().get(Calendar.DAY_OF_WEEK) + 5) % 7;

                if(!dayFilters.get(dayOfWeek))
                {
                    addLine = false;
                }
            }

            if(addLine && (filter == null || (allEntries.get(i).GetName() != null && allEntries.get(i).GetName().toLowerCase().contains(filter.toLowerCase()))))
                output.add(allEntries.get(i));
        }
        return output;
    }

    public static float[] ExtractDailyTotals(List<PhoneLogEntry> allEntries, Calendar startDate, int midnightHour, String filter, List<Boolean> dayFilters, boolean timeTotal)
    {
        List<PhoneLogEntry> entries = ExtractEventLog(allEntries, filter, dayFilters);
        List<Float> countsPerDay = new ArrayList<>();
        Calendar lastDate = null;
        int dayCount = 0;
        startDate.setTimeInMillis(entries.get(0).GetDate().getTimeInMillis());
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
                dayCount = timeTotal ? (int)entries.get(j).GetDuration() : 1;
            }
            else
                dayCount += timeTotal ? (int)entries.get(j).GetDuration() : 1;

            lastDate = curDate;
        }

        //Add entries for every day between the last entry and now
        int days = DateStrings.GetActiveDiffInDays(lastDate, Calendar.getInstance(), midnightHour) + 1;
        for(int d=0; d<days; d++)
        {
            countsPerDay.add((float)dayCount);
            dayCount = 0;
        }

        //Copy List<float> to DailyCounts[typeIndex][]
        float[] ret = new float[countsPerDay.size()];
        for(int q=0; q<countsPerDay.size(); q++)
            ret[q] = countsPerDay.get(q);

        return ret;
    }
}
