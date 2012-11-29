package com.Dave.Files;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Calendar;
import java.util.List;

import android.util.Log;

import com.Dave.DateStrings.DateStrings;

public class LoggerState
{
	public String TempFilename = "/sdcard/DaveLogger/TempFile.txt";
	public Calendar ActiveDate = null;
	
	public String[] EventNames = null;
	public int[] EventDailyCounts = null;
	public Calendar[][] EventRecentHistories = null;
	
	public String[] ToggleNames = null;
	public boolean[] ToggleStates = null;
	public Calendar[] ToggleLastDates = null;
	
	private LoggerState(String tempFilename)
	{
		TempFilename = tempFilename;
	}
	
	public static LoggerState FromFile(String filename, LoggerConfig config)
	{
		Log.i("LoggerState", "Loading from file");
		LoggerState ret = new LoggerState(filename);
		
		try
    	{
    		BufferedReader br = new BufferedReader(new FileReader(ret.TempFilename));
    		
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
    		
    		while(true)
    		{
    			String line = br.readLine();
    			if(line == null)
    				break;
    			
    			String[] parts = line.split(" = ");
    			if(parts==null)
    				continue;
    			
    			if(parts[0].equals("ActiveDate"))
    				ret.ActiveDate = DateStrings.ParseDateTimeString(parts[1]);
    			if(parts[0].equals("Event"))
    			{
    				String[] subparts = parts[1].split(", ");
    				int index = config.Buttons.indexOf(subparts[0]);
    				if(index < 0)
    					continue;
    				
    				ret.EventNames[index] = subparts[0];
    				ret.EventDailyCounts[index] = Integer.valueOf(subparts[1]);
    				
    				for(int i=0; i<3; i++)
    					ret.EventRecentHistories[index][i] = DateStrings.ParseDateTimeString(subparts[i + 2].trim());
    			}
    			if(parts[0].equals("Toggle"))
    			{
    				String[] subparts = parts[1].split(", ");
    				int index = config.Toggles.indexOf(subparts[0]);
    				if(index < 0)
    					continue;
    				
    				ret.ToggleNames[index] = subparts[0];
    				ret.ToggleStates[index] = subparts[1].trim().equals("on");
    				ret.ToggleLastDates[index] = DateStrings.ParseDateTimeString(subparts[2].trim());
    			}
    		}
    		br.close();
    		
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
	
	public static LoggerState Create(String filename, List<LogEntry> entries, LoggerConfig config)
	{
		int numButtons = config.Buttons.size();
		int numToggles = config.Toggles.size();
		
		int numEntries = 0;
		if(entries != null)
			numEntries = entries.size();
		Log.i("LoggerState", String.format("Creating new: %d buttons, %d toggles (%d entries)", numButtons, numToggles, numEntries));
		
		LoggerState ret = new LoggerState(filename);
		
		ret.ActiveDate = DateStrings.GetActiveDate(Calendar.getInstance(), config.MidnightHour);
		
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
    		FileWriter fw = new FileWriter(TempFilename, false);
    		
    		fw.write(String.format("ActiveDate = %s\n", DateStrings.GetDateTimeString(ActiveDate)));
    		
    		for(int i=0; i<config.Buttons.size(); i++)
    		{
    			fw.write(String.format("Event = %s, %d, %s, %s, %s\n",
    					config.Buttons.get(i),
    									EventDailyCounts[i],
    									DateStrings.GetDateTimeString(EventRecentHistories[i][0]),
    									DateStrings.GetDateTimeString(EventRecentHistories[i][1]),
    									DateStrings.GetDateTimeString(EventRecentHistories[i][2])
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
