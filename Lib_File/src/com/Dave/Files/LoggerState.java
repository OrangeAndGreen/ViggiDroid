package com.Dave.Files;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import android.content.SharedPreferences;
import android.util.Log;
import com.Dave.DateStrings.DateStrings;

public class LoggerState
{
	private SharedPreferences mPrefs = null;
	private String mStorageDirectory = null;
		
    public Calendar ActiveDate = null;
    public boolean Safe = false;
        
    public String[] EventNames = null;
    public int[] EventDailyCounts = null;
    public Calendar[][] EventRecentHistories = null;
        
    public String[] ToggleNames = null;
    public boolean[] ToggleStates = null;
    public Calendar[] ToggleLastDates = null;
         
    private LoggerState(SharedPreferences prefs, String storageDirectory)
    {
    	mPrefs = prefs;
		mStorageDirectory = storageDirectory;
    }
        
    public static LoggerState Load(SharedPreferences prefs, LoggerConfig config)
	{
		Log.i("LoggerState", "Loading from internal storage");
		LoggerState ret = new LoggerState(prefs, null);
						
		try
		{
			int numButtons = config.Buttons.size();
			int numToggles = config.Toggles.size();
				
			ret.EventNames = new String[numButtons];
			ret.EventDailyCounts = new int[numButtons];
			ret.EventRecentHistories = new Calendar[numButtons][];
			for(int i=0; i<numButtons; i++)
				ret.EventRecentHistories[i] = new Calendar[3];
				
			ret.ToggleNames = new String[numToggles];
			ret.ToggleStates = new boolean[numToggles];
			ret.ToggleLastDates = new Calendar[numToggles];
				
			ret.mStorageDirectory = prefs.getString("storageDirectory", null);
			if(ret.mStorageDirectory == null)
				return null;
				
			String activeDate = prefs.getString("activeDate", null);
			if(activeDate == null)
				return null;
			ret.ActiveDate = DateStrings.ParseDateTimeString(activeDate);
			
			ret.Safe = prefs.getBoolean("safe", false);
				
			for(int index=0; index<numButtons; index++)
			{
				String buttonName = config.Buttons.get(index);
				String buttonInfo = prefs.getString(buttonName, null);
				if(buttonInfo == null)
					return null;
				
				String[] parts = buttonInfo.split(", ");
						
				ret.EventNames[index] = buttonName;
				ret.EventDailyCounts[index] = Integer.valueOf(parts[0]);
						
				for(int i=0; i<3; i++)
					ret.EventRecentHistories[index][i] = DateStrings.ParseDateTimeString(parts[i + 1].trim());
			}
				
			for(int index=0; index<numToggles; index++)
			{
				String toggleName = config.Toggles.get(index);
				String toggleInfo = prefs.getString(toggleName, null);
				if(toggleInfo == null)
					return null;
				
				String[] parts = toggleInfo.split(", ");
						
				ret.ToggleNames[index] = toggleName;
				ret.ToggleStates[index] = parts[0].trim().equals("on");
				ret.ToggleLastDates[index] = DateStrings.ParseDateTimeString(parts[1].trim());
			}
						
			Log.i("LoggerState", String.format("%d buttons, %d toggles", ret.EventDailyCounts.length, ret.ToggleStates.length));
						
			if(!DateStrings.SameDay(Calendar.getInstance(), ret.ActiveDate, config.MidnightHour))
			{
				Log.i("LoggerState", "Starting new day");
				ret.StartNewActiveDay(config);
				ret.Save(config);
			}
						
			return ret;
		}
		catch(Exception e)
		{
			Log.e("LoggerState", String.format("Error loading TempFile: %s", e.toString()));
			return null;
		}
	}
        
    public static LoggerState Create(SharedPreferences prefs, List<LogEntry> entries, LoggerConfig config, String storageDirectory)
    {
    	int numButtons = config.Buttons.size();
        int numToggles = config.Toggles.size();
                
        int numEntries = 0;
        if(entries != null)
        	numEntries = entries.size();
        Log.i("LoggerState", String.format("Creating new: %d buttons, %d toggles (%d entries)", numButtons, numToggles, numEntries));
                
        LoggerState ret = new LoggerState(prefs, storageDirectory);
                
        ret.ActiveDate = DateStrings.GetActiveDate(Calendar.getInstance(), config.MidnightHour);
		ret.Safe = false;
                
        ret.EventNames = new String[numButtons];
        ret.EventDailyCounts = new int[numButtons];
        ret.EventRecentHistories = new Calendar[numButtons][];
        for(int i=0; i<numButtons; i++)
        	ret.EventRecentHistories[i] = new Calendar[3];
                
        ret.ToggleNames = new String[numToggles];
        ret.ToggleStates = new boolean[numToggles];
        ret.ToggleLastDates = new Calendar[numToggles];
                
        for(int i=0; i<numButtons; i++)
        {
        	ret.EventNames[i] = config.Buttons.get(i);
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
                if(DateStrings.SameDay(ret.EventRecentHistories[index][0], Calendar.getInstance(), config.MidnightHour))
                	ret.EventDailyCounts[index]++;
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
            
            for(int i=0; i<EventDailyCounts.length; i++)
                    EventDailyCounts[i] = 0;
    }
       
    //Updates the list of 3 most recent entries
    private void UpdateEventHistory(Calendar curDate, int index)
    {
            EventRecentHistories[index][2] = EventRecentHistories[index][1];
            EventRecentHistories[index][1] = EventRecentHistories[index][0];
            EventRecentHistories[index][0] = curDate;
    }
    
    public void UpdateEvent(int index, Calendar date, LoggerConfig config)
    {
            EventDailyCounts[index]++;
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
				SharedPreferences.Editor editor = mPrefs.edit();
				
				//Clear whatever was in the prefs before
				editor.clear();
		
				editor.putString("storageDirectory", mStorageDirectory);
		
				editor.putString("activeDate", DateStrings.GetDateTimeString(ActiveDate));
				editor.putBoolean("safe", Safe);
				
				for(int i=0; i<config.Buttons.size(); i++)
				{
					String entry = String.format(Locale.getDefault(), "%d, %s, %s, %s", EventDailyCounts[i],
																   DateStrings.GetDateTimeString(EventRecentHistories[i][0]),
																   DateStrings.GetDateTimeString(EventRecentHistories[i][1]),
																   DateStrings.GetDateTimeString(EventRecentHistories[i][2])
																   );
					editor.putString(config.Buttons.get(i), entry);
				}
				
				for(int i=0; i<config.Toggles.size(); i++)
				{
					String toggleState = "off";
					if(ToggleStates[i])
						toggleState = "on";
					String entry = String.format("%s, %s", toggleState, DateStrings.GetDateTimeString(ToggleLastDates[i]));
					editor.putString(config.Toggles.get(i), entry);
				}
				
				editor.commit();
            }
            catch(Exception e)
            {
                    Log.e("LoggerState", "Error saving LoggerState");
            }
    }

}