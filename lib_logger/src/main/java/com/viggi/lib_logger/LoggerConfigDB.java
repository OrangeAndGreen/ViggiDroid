package com.viggi.lib_logger;

import com.viggi.lib_file.LoggerConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Dave on 3/31/2015.
 */
public class LoggerConfigDB
{
    private int mAveragingWindow;
    private int mRolloverHour;
    private boolean mSafeMode;

    private List<LoggerConfigEntry> mConfigItems;

    public LoggerConfigDB()
    {
        mConfigItems = new ArrayList<LoggerConfigEntry>();
    }

    public LoggerConfigDB loadFromDB()
    {
        //TODO: Attempt to load from DB
        return createDefault();
    }

    public LoggerConfigDB createDefault()
    {
        LoggerConfigDB ret = new LoggerConfigDB();

        ret.mAveragingWindow = 30;
        ret.mRolloverHour = 5;
        ret.mSafeMode = false;

        ret.mConfigItems.add(new LoggerConfigEntry(true, 0, "Sleep", false, false));
        ret.mConfigItems.add(new LoggerConfigEntry(true, 1, "Drive", false, false));
        ret.mConfigItems.add(new LoggerConfigEntry(true, 1, "Wash", false, false));

        ret.mConfigItems.add(new LoggerConfigEntry(false, 0, "Drink", false, false));
        ret.mConfigItems.add(new LoggerConfigEntry(false, 1, "Friend", false, false));
        ret.mConfigItems.add(new LoggerConfigEntry(false, 2, "Teeth", false, false));

        return ret;
    }

    public boolean save()
    {
        //TODO: Save to DB
        return false;
    }

    public boolean equalTo(LoggerConfigDB other)
    {
        if(mAveragingWindow != other.mAveragingWindow)
            return false;

        if(mRolloverHour!= other.mRolloverHour)
            return false;

        if(mSafeMode != other.mSafeMode)
            return false;

        //Compare buttons

        List<LoggerConfigEntry> myButtons = getButtons();
        List<LoggerConfigEntry> otherButtons = other.getButtons();
        if(myButtons.size() != otherButtons.size())
            return false;

        for(int i=0; i<myButtons.size(); i++)
        {
            if(!myButtons.get(i).equalTo(otherButtons.get(i)))
                return false;
        }

        //Compare toggles

        List<LoggerConfigEntry> myToggles = getToggles();
        List<LoggerConfigEntry> otherToggles = other.getToggles();
        if(myToggles.size() != otherToggles.size())
            return false;

        for(int i=0; i<myToggles.size(); i++)
        {
            if(!myToggles.get(i).equalTo(otherToggles.get(i)))
                return false;
        }

        return true;
    }


    // ACCESSORS

    public List<LoggerConfigEntry> getButtons()
    {
        List<LoggerConfigEntry> ret = new ArrayList<LoggerConfigEntry>();

        for(LoggerConfigEntry entry : mConfigItems)
        {
            if(!entry.IsToggle)
            {
                ret.add(entry);
            }
        }

        return ret;
    }

    public List<LoggerConfigEntry> getToggles()
    {
        List<LoggerConfigEntry> ret = new ArrayList<LoggerConfigEntry>();

        for(LoggerConfigEntry entry : mConfigItems)
        {
            if(entry.IsToggle)
            {
                ret.add(entry);
            }
        }

        return ret;
    }
}
