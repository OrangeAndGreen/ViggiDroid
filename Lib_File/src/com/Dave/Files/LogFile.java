package com.Dave.Files;

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

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import com.Dave.DateStrings.DateStrings;
import com.Dave.Files.LogEntry;
import com.Dave.Math.ArrayMath;

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
    
    public List<LogEntry> ExtractEventLog(int searchIndex, LoggerConfig config)
    {
		//Populate the output array
		List<LogEntry> output = new ArrayList<LogEntry>();
		for(int i=0; i<GetLogEntries().size(); i++)
		{
			boolean addLine = true;
			if(searchIndex >=0)
			{
	    		int index = GetLogEntries().get(i).GetId(config);
	    		addLine = index == searchIndex;
			}
			if(addLine)
				output.add(GetLogEntries().get(i));
		}
		return output;
    }

    public List<LogEntry> ExtractToggleLog(int searchIndex, LoggerConfig config)
    {
    	List<LogEntry> output = new ArrayList<LogEntry>();
		for(int i=0; i<GetLogEntries().size(); i++)
		{
			boolean addLine = true;
			if(searchIndex >=0)
			{
	    		int index = GetLogEntries().get(i).GetToggleId(config);
	    		addLine = index == searchIndex;
			}
			if(addLine)
			{
				output.add(GetLogEntries().get(i));
			}
		}
		return output;
    }

    public float[] ExtractDailyEventTotals(int searchIndex, Calendar startDate, LoggerConfig config)
    {
    	List<LogEntry> entries = ExtractEventLog(searchIndex, config);
		List<Float> countsPerDay = new ArrayList<Float>();
		Calendar lastDate = null;
		int dayCount = 0;
		startDate.setTimeInMillis(entries.get(0).GetDate().getTimeInMillis());
		for(int j=0; j<entries.size(); j++) //for each date
		{
			Calendar curDate = entries.get(j).GetDate();
			if(curDate == null)
				continue;
			
			if(lastDate != null && !DateStrings.SameDay(lastDate, curDate, config.MidnightHour))
			{
				//This entry starts a new day
				int days = DateStrings.GetActiveDiffInDays(lastDate, curDate, config.MidnightHour);
				for(int k=0; k<days; k++)
				{
					//Add the current count for the end of the previous day,
						//then any zero-days to catch up to the current day
					countsPerDay.add(new Float(dayCount));
					dayCount = 0;
				}
				dayCount = 1;
			}
			else
				dayCount++; //Increment the counter for the current day
			
			lastDate = curDate;
		}
		
		//Add entries for every day between the last entry and now
		int days = DateStrings.GetActiveDiffInDays(lastDate, Calendar.getInstance(), config.MidnightHour) + 1;
		for(int d=0; d<days; d++)
		{
			countsPerDay.add(new Float(dayCount));
			dayCount = 0;
		}
			
		//Copy List<float> to DailyCounts[typeIndex][]
		float[] ret = new float[countsPerDay.size()];
		for(int q=0; q<countsPerDay.size(); q++)
			ret[q] = countsPerDay.get(q);
    	
    	return ret;
    }
    
    public float[] ExtractDailyToggleTotals(int searchIndex, Calendar startDate, LoggerConfig config)
    {
    	List<LogEntry> subset = ExtractToggleLog(searchIndex, config);
		int subsetSize = subset.size();
		LogEntry lastEntry = subset.get(subsetSize - 1);
		if(lastEntry.ToggleState != null && lastEntry.ToggleState.equals("on"))
		{
			//Add an artificial "off" with now as the time, so the last toggle "on" gets included 
			subset.add(new LogEntry(DateStrings.GetDateTimeString(Calendar.getInstance()),
					lastEntry.GetType(), "off", null));
			subsetSize++;
		}
		
		Calendar onDate = null;
		Calendar endDate = null;
		List<Float> totals = new ArrayList<Float>(); 
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
					onDate = curDate; //Turned a toggle on
					int days = DateStrings.GetActiveDiffInDays(endDate, curDate, config.MidnightHour);
					for(int d=0; d<days; d++)
					{
						totals.add(new Float(dayTotal));
						dayTotal = 0;
					}
				}
				else if(onDate != null)
				{
					//Turned a toggle off
					int days = DateStrings.GetActiveDiffInDays(onDate, curDate, config.MidnightHour);
					float totalElapsed = (curDate.getTimeInMillis() - onDate.getTimeInMillis()) / (float)3600000;
					Calendar firstMidnight = (Calendar) onDate.clone();
					int midnightShift = config.MidnightHour;
					if(firstMidnight.get(Calendar.HOUR_OF_DAY) >= midnightShift)
					{
						firstMidnight.add(Calendar.HOUR, 24);
					}
					firstMidnight.set(Calendar.HOUR_OF_DAY, midnightShift);
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
							totals.add(new Float(dayTotal));
							dayTotal = 0;
							totalElapsed -= elapsedTime;
						}
						else
						{
							totals.add(new Float(24));
							totalElapsed -= 24;
						}
					}
					dayTotal += totalElapsed;
					endDate = curDate;
					onDate = null;
				}
			}
			//Add entries from the last "off" date to now
			int days = DateStrings.GetActiveDiffInDays(endDate, Calendar.getInstance(), config.MidnightHour) + 1;
			for(int d=0; d<days; d++)
			{
				if(d==0)
				{
					totals.add(new Float(dayTotal));
				}
				else
				{
					totals.add(new Float(0));
				}
			}
			endDate = Calendar.getInstance();
		}
		
		//Convert the list of floats to an array for graphing
		float[] values = new float[totals.size()];
		for(int q=0; q<totals.size(); q++)
			values[q] = totals.get(q);
    	
    	return values;
    }
    
    public float[] GetDailyEventHistogram(int searchIndex, LoggerConfig config)
    {
    	Calendar startDate = Calendar.getInstance();
    	float[] dailyTotals = ExtractDailyEventTotals(searchIndex, startDate, config);
    	
    	float[] weekDays = new float[7];
    	float[] counts = new float[7];
    	int curDay = startDate.get(Calendar.DAY_OF_WEEK) % 7;
    	
    	for(int i=0; i<dailyTotals.length; i++)
    	{
    		weekDays[curDay] += dailyTotals[i];
    		counts[curDay]++;
    		curDay = (curDay + 1) %7;
    	}
    	
    	for(int j=0; j<7; j++)
    		weekDays[j] /= counts[j];
    	
    	return weekDays;
    }
    
    public float[] GetDailyToggleHistogram(int searchIndex, LoggerConfig config)
    {
    	Calendar startDate = Calendar.getInstance();
    	float[] dailyTotals = ExtractDailyToggleTotals(searchIndex, startDate, config);
    	
    	float[] weekDays = new float[7];
    	float[] counts = new float[7];
    	int curDay = startDate.get(Calendar.DAY_OF_WEEK) % 7;
    	
    	for(int i=0; i<dailyTotals.length; i++)
    	{
    		weekDays[curDay] += dailyTotals[i];
    		counts[curDay]++;
    		curDay = (curDay + 1) %7;
    	}
    	
    	for(int j=0; j<7; j++)
    		weekDays[j] /= counts[j];
    	
    	return weekDays;

    }
    
    public String GetEventStats(int searchIndex, LoggerConfig config)
    {
    	Calendar now = Calendar.getInstance();
    	String stats = "";
		List<LogEntry> subset = ExtractEventLog(searchIndex, config);
		if(subset == null)
		{
			Log.d("", "Event stats subset is null");
			return stats;
		}
		
		if(searchIndex >= 0)
			stats += config.Buttons.get(searchIndex) + ":\n";
		else
			stats += "All:\n";
		
		int subsetSize = subset.size();
		if(subsetSize == 0)
		{
			Log.d("", "Event stats subset is empty");
			return stats + "0 logged\n";
		}
		
		//First entry stats
		Calendar firstDate = subset.get(0).GetDate();
		String elapsed = DateStrings.GetElapsedTimeString(firstDate, now, 3);
		stats += String.format("First entry: %s (%s ago)\n", DateStrings.GetPrintableDateString(firstDate), elapsed);
	
		//Total entry stats
		float elapsedDays = ((float)(now.getTimeInMillis() - firstDate.getTimeInMillis())) / 1000 / 3600 / 24;
		float dailyAverage = subsetSize/elapsedDays;
		stats += String.format("Total: %d entries (%.02f/day, %d/year)\n", subsetSize,
								dailyAverage, (int)(dailyAverage * 365));
		stats += "\n";
		
		//Daily total stats
		Calendar startDate = Calendar.getInstance();
		float[] dailyTotals = ExtractDailyEventTotals(searchIndex, startDate, config);
		float min = Float.MAX_VALUE;//ArrayMath.GetMin(dailyTotals);
		Calendar minDate = (Calendar)startDate.clone();
		float max = Float.MIN_VALUE;//ArrayMath.GetMax(dailyTotals);
		Calendar maxDate = (Calendar)startDate.clone();
		
		Calendar curDate = (Calendar)startDate.clone();
		for(int i=0; i<dailyTotals.length; i++)
		{
			float curValue = dailyTotals[i];
			if(curValue < min)
			{
				min = curValue;
				minDate = (Calendar)curDate.clone();
			}
			if(curValue > max)
			{
				max = curValue;
				maxDate = (Calendar)curDate.clone();
			}
			
			curDate.add(Calendar.HOUR, 24);
		}
		
		stats += String.format("Least/day: %d (%s)\nMost/day: %d (%s)\n", (int)min, DateStrings.GetDateTimeString(minDate), (int)max, DateStrings.GetDateTimeString(maxDate));
		stats += "\n";
		
		//Interval stats
		int numEntries = subset.size();
		float[] intervals = new float[numEntries];
		Calendar lastDate = null;
		min = Float.MAX_VALUE;
		max = Float.MIN_VALUE;
		for(int i=0; i<numEntries; i++)
		{
			curDate = subset.get(i).GetDate();
			if(i > 0)
				intervals[i] = (curDate.getTimeInMillis() - lastDate.getTimeInMillis()) / (float) 3600000;
			
			if(lastDate != null && intervals[i] < min)
			{
				min = intervals[i];
				minDate = (Calendar) lastDate.clone();
			}
			
			if(lastDate != null && intervals[i] > max)
			{
				max = intervals[i];
				maxDate = (Calendar) lastDate.clone();
			}
			
			//Use the following debug line when looking for out-of-order dates
			//if(intervals[i] < 0)
			//	Log.d("CHECK", "Adding interval " + intervals[i] + " between " + DateStrings.GetDateTimeString(lastDate) + " and " + DateStrings.GetDateTimeString(curDate));
			lastDate = curDate;
		}
		stats += String.format("Shortest interval: %.02f hours (%s)\nLongest interval: %.02f hours (%s)\n", min, DateStrings.GetDateTimeString(minDate), max, DateStrings.GetDateTimeString(maxDate));
		stats += "\n";
		
		float[] allAve = ArrayMath.GetAllTimeRunningAverageCurve(dailyTotals);
		min = Float.MAX_VALUE;
		max = Float.MIN_VALUE;
		for(int i=0; i<allAve.length; i++)
		{
			curDate = subset.get(i).GetDate();
			if(allAve[i] < min)
			{
				min = allAve[i];
				minDate = (Calendar) curDate.clone();
			}
			
			if(allAve[i] > max)
			{
				max = allAve[i];
				maxDate = (Calendar) curDate.clone();
			}
		}
		stats += String.format("All-time average:\nMin: %.02f/day (%s)\nMax: %.02f/day (%s)\n", min, DateStrings.GetDateTimeString(minDate), max, DateStrings.GetDateTimeString(maxDate));
		stats += "\n";
		
		float[] ave = ArrayMath.GetRunningAverageCurve(dailyTotals, 30);
		min = Float.MAX_VALUE;
		max = Float.MIN_VALUE;
		for(int i=0; i<allAve.length; i++)
		{
			curDate = subset.get(i).GetDate();
			if(ave[i] < min)
			{
				min = ave[i];
				minDate = (Calendar) curDate.clone();
			}
			
			if(ave[i] > max)
			{
				max = ave[i];
				maxDate = (Calendar) curDate.clone();
			}
		}
		stats += String.format("Running average:\nMin: %.02f/day (%s)\nMax: %.02f/day (%s)", min, DateStrings.GetDateTimeString(minDate), max, DateStrings.GetDateTimeString(maxDate));
		
    	return stats;
    }
    
    public String GetToggleStats(int searchIndex, LoggerConfig config)
    {
    	Calendar now = Calendar.getInstance();
    	String stats = "";
    	List<LogEntry> subset = ExtractToggleLog(searchIndex, config);
    	if(subset == null)
    		return stats;
    	stats += config.Toggles.get(searchIndex) + ":\n";
    	int subsetSize = subset.size();
		if(subsetSize == 0)
			return stats + "0 logged\n";

		//First entry stats
		Calendar firstDate = subset.get(0).GetDate();
		String elapsed = DateStrings.GetElapsedTimeString(firstDate, now, 3);
		stats += String.format("First entry: %s (%s ago)\n", DateStrings.GetPrintableDateString(firstDate), elapsed);
	
		//Total entry stats
		float elapsedDays = ((float)(now.getTimeInMillis() - firstDate.getTimeInMillis())) / 1000 / 3600 / 24;
		float dailyAverage = subsetSize/elapsedDays;
		stats += String.format("Total: %d entries (%.02f/day, %d/year)\n", subsetSize/2, dailyAverage/2, (int)(dailyAverage * 365/2));

		//Total time stats
		float totalTime = 0;
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
				totalTime += ((float)(curEntry.GetDate().getTimeInMillis() - onDate.getTimeInMillis())) / 1000 / 3600;
				onDate = null;
			}
		}
		if(onDate != null)
			totalTime += ((float)(now.getTimeInMillis() - onDate.getTimeInMillis())) / 1000 / 3600;
		stats += String.format("Time: %.02f days (%.02f hrs/day, %d hrs/yr)\n", totalTime/24, totalTime/elapsedDays, (int)(totalTime/elapsedDays * 365));
		stats += "\n";
		
		//Daily total stats
		Calendar startDate = Calendar.getInstance();
		Calendar minDate = Calendar.getInstance();
		Calendar maxDate = Calendar.getInstance();
		float[] dailyTotals = ExtractDailyToggleTotals(searchIndex, startDate, config);
		float min = Float.MAX_VALUE;
		float max = Float.MIN_VALUE;
		for(int i=0; i<dailyTotals.length; i++)
		{
			Calendar curDate = subset.get(i).GetDate();
			if(dailyTotals[i] < min)
			{
				min = dailyTotals[i];
				minDate = (Calendar)curDate.clone();
			}
			if(dailyTotals[i] > max)
			{
				max = dailyTotals[i];
				maxDate = (Calendar)curDate.clone();
			}
		}
		stats += String.format("Least/day: %.02f hours (%s)\nMost/day: %.02f hours (%s)\n", min, DateStrings.GetDateTimeString(minDate), max, DateStrings.GetDateTimeString(maxDate));
		stats += "\n";
		
		//Min/max on/off times
		float minOff = Float.MAX_VALUE;
		Calendar minOffDate = Calendar.getInstance();
		float minOn = Float.MAX_VALUE;
		Calendar minOnDate = Calendar.getInstance();
		float maxOff = Float.MIN_VALUE;
		Calendar maxOffDate = Calendar.getInstance();
		float maxOn = Float.MIN_VALUE;
		Calendar maxOnDate = Calendar.getInstance();
		Calendar lastDate = null;
		for(int i=0; i<subsetSize; i++)
		{
			Calendar curDate = subset.get(i).GetDate();
			if(lastDate != null)
			{
				float hours = (curDate.getTimeInMillis() - lastDate.getTimeInMillis()) / (float)3600000;
				if(subset.get(i).ToggleState.equals("off"))
				{
					if(hours < minOn)
					{
						minOn = hours;
						minOnDate = (Calendar)lastDate.clone();
					}
					if(hours < maxOn)
					{
						maxOn = hours;
						maxOnDate = (Calendar)lastDate.clone();
					}
				}
				else
				{
					if(hours < minOff)
					{
						minOff = hours;
						minOffDate = (Calendar)lastDate.clone();
					}
					if(hours < maxOff)
					{
						maxOff = hours;
						maxOffDate = (Calendar)lastDate.clone();
					}
				}
			}
			lastDate = curDate;
		}
		stats += String.format("Shortest off: %.02f hours (%s)\nLongest off: %.02f hours (%s)\n", minOff, DateStrings.GetDateTimeString(minOffDate), maxOff, DateStrings.GetDateTimeString(maxOffDate));
		stats += String.format("Shortest on: %.02f hours (%s)\nLongest on: %.02f hours (%s)\n", minOn, DateStrings.GetDateTimeString(minOnDate), maxOn, DateStrings.GetDateTimeString(maxOnDate));
		stats += "\n";
		
		float[] allAve = ArrayMath.GetAllTimeRunningAverageCurve(dailyTotals);
		min = Float.MAX_VALUE;
		max = Float.MIN_VALUE;
		
		for(int i=0; i<allAve.length; i++)
		{
			Calendar curDate = subset.get(i).GetDate();
			if(allAve[i] < min)
			{
				min = allAve[i];
				minDate = (Calendar)curDate.clone();
			}
			if(allAve[i] > max)
			{
				max = allAve[i];
				maxDate = (Calendar)curDate.clone();
			}
		}
		
		stats += String.format("All-time average:\nMin: %.02f hours/day (%s)\nMax: %.02f hours/day (%s)\n", min, DateStrings.GetDateTimeString(minDate), max, DateStrings.GetDateTimeString(maxDate));
		stats += "\n";
		
		float[] ave = ArrayMath.GetRunningAverageCurve(dailyTotals, 30);
		min = Float.MAX_VALUE;
		max = Float.MIN_VALUE;
		
		for(int i=0; i<ave.length; i++)
		{
			Calendar curDate = subset.get(i).GetDate();
			if(ave[i] < min)
			{
				min = ave[i];
				minDate = (Calendar)curDate.clone();
			}
			if(ave[i] > max)
			{
				max = ave[i];
				maxDate = (Calendar)curDate.clone();
			}
		}
		stats += String.format("Running average:\nMin: %.02f hours/day (%s)\nMax: %.02f hours/day (%s)", min, DateStrings.GetDateTimeString(minDate), max, DateStrings.GetDateTimeString(maxDate));
				
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
