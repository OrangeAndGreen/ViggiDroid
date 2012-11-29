package com.Dave.DateStrings;

import java.util.Calendar;

public class DateStrings
{
	public static String GetDateString(Calendar curDate)
    {
    	if(curDate == null)
    	{
    		return "-";
    	}
    	int year = curDate.get(Calendar.YEAR);
    	int month = curDate.get(Calendar.MONTH) + 1;
    	int day = curDate.get(Calendar.DAY_OF_MONTH);
    	String dateStr = String.format("%04d%02d%02d", year, month, day);
    	return dateStr;
    }
    
    public static String GetDateTimeString(Calendar curDate)
    {
    	if(curDate == null)
    	{
    		return "-";
    	}
    	int year = curDate.get(Calendar.YEAR);
    	int month = curDate.get(Calendar.MONTH) + 1;
    	int day = curDate.get(Calendar.DAY_OF_MONTH);
    	int hour = curDate.get(Calendar.HOUR_OF_DAY);
    	int minute = curDate.get(Calendar.MINUTE);
    	int second = curDate.get(Calendar.SECOND);
    	String dateStr = String.format("%04d%02d%02d_%02d%02d%02d", year, month, day, hour, minute, second);
    	return dateStr;
    }
    
    public static String GetPrintableDateString(Calendar curDate)
    {
    	if(curDate == null)
    	{
    		return "-";
    	}
    	int year = curDate.get(Calendar.YEAR);
    	int month = curDate.get(Calendar.MONTH) + 1;
    	int day = curDate.get(Calendar.DAY_OF_MONTH);
    	return String.format("%d/%d/%04d", month, day, year);
    }
    
    public static String GetPrintableDateTimeString(Calendar curDate, int midnightHour)
    {
    	if(curDate == null)
    	{
    		return "-";
    	}
    	int month = curDate.get(Calendar.MONTH) + 1;
    	int day = curDate.get(Calendar.DAY_OF_MONTH);
    	int hour = curDate.get(Calendar.HOUR_OF_DAY);
    	int minute = curDate.get(Calendar.MINUTE);
    	int second = curDate.get(Calendar.SECOND);
    	String dateString = String.format("%d/%d", month, day);
    	if(SameDay(curDate, Calendar.getInstance(), midnightHour))
    		dateString = "today";
    	return String.format("%s %d:%02d:%02d", dateString, hour, minute, second);
    }
    
    public static String GetPresentableDateTimeString(Calendar curDate)
    {
    	if(curDate == null)
    	{
    		return "-";
    	}
    	int month = curDate.get(Calendar.MONTH) + 1;
    	int day = curDate.get(Calendar.DAY_OF_MONTH);
    	int hour = curDate.get(Calendar.HOUR_OF_DAY);
    	int minute = curDate.get(Calendar.MINUTE);
    	int second = curDate.get(Calendar.SECOND);
    	String dateString = String.format("%d/%d", month, day);
    	return String.format("%s %d:%02d:%02d", dateString, hour, minute, second);
    }
    
    public static String GetElapsedTimeString(Calendar oldDate, Calendar newDate, int maxCount)
    {
    	if(oldDate == null || newDate == null)
    	{
    		return "-";
    	}
    	long elapsed = newDate.getTimeInMillis() - oldDate.getTimeInMillis();
    	
    	return GetElapsedTimeString(elapsed, maxCount);
    }
    
    public static String GetElapsedTimeString(long milliseconds, int maxCount)
    {
    	milliseconds = milliseconds / 1000;
    	String output = "";
    	
    	int months = (int) milliseconds / 2580000;
    	milliseconds = milliseconds - (months * 2580000);
    	int weeks = (int) milliseconds / 602000;
    	milliseconds = milliseconds - (weeks * 602000);
    	int days = (int) milliseconds / 86000;
    	milliseconds = milliseconds - (days * 86000);
    	int hours = (int) milliseconds / 3600;
    	milliseconds = milliseconds - (hours * 3600);
    	int minutes = (int) milliseconds / 60;
    	milliseconds = milliseconds - (minutes * 60);
    	int seconds = (int) milliseconds;
    	
    	int outCount = 0;
    	
    	if(months>0 && outCount < maxCount)
    	{
    		outCount++;
    		output += String.format("%dM", months);
    	}
    	if(weeks>0 && months == 0 && outCount < maxCount)
    	{
    		outCount++;
    		output += String.format("%dW", weeks);
    	}
    	if(days>0 && outCount < maxCount)
    	{
    		outCount++;
    		output += String.format("%dd", days);
    	}
    	if(hours>0 && outCount < maxCount)
    	{
    		outCount++;
    		output += String.format("%dh", hours);
    	}
    	if(minutes>0 && outCount < maxCount)
    	{
    		outCount++;
    		output += String.format("%dm", minutes);
    	}
    	if(outCount < maxCount)
    	{
    		outCount++;
    		output += String.format("%ds", seconds);
    	}

    	return output;
    }

    public static String GetMonthString(Calendar date)
    {
    	int month = date.get(Calendar.MONTH) + 1;
    	
    	switch(month)
    	{
    	case 1:
    		return "January";
    	case 2:
    		return "February";
    	case 3:
    		return "March";
    	case 4:
    		return "April";
    	case 5:
    		return "May";
    	case 6:
    		return "June";
    	case 7:
    		return "July";
    	case 8:
    		return "August";
    	case 9:
    		return "September";
    	case 10:
    		return "October";
    	case 11:
    		return "November";
    	case 12:
    		return "December";
    	}
    	
    	return "-";
    }
    
    public static int GetElapsedMinutes(Calendar oldDate, Calendar newDate)
    {
    	if(oldDate == null || newDate == null)
    		return 0;
    	return (int)(newDate.getTimeInMillis() - oldDate.getTimeInMillis()) / 60000;
    }
    
    public static Calendar ParseDateTimeString(String curDate)
    {
    	if(curDate == null || curDate.equals("-"))
    		return null;
    	Calendar gc = Calendar.getInstance();
    	try
    	{
    	int year = Integer.parseInt(curDate.substring(0, 4));
    	int month = Integer.parseInt(curDate.substring(4, 6)) - 1;
    	int day = Integer.parseInt(curDate.substring(6, 8));
    	int hour = Integer.parseInt(curDate.substring(9, 11));
    	int minute = Integer.parseInt(curDate.substring(11, 13));
    	int second = Integer.parseInt(curDate.substring(13, 15));
    	gc.set(year, month, day, hour, minute, second);
    	}
    	catch(Exception e)
    	{
    		return null;
    	}
    	return gc;
    }
    
    public static boolean SameDay(Calendar inDate1, Calendar inDate2, int midnightHour)
    {
    	if(inDate1==null || inDate2==null)
    		return false;
    	Calendar date1 = (Calendar) inDate1.clone();
    	Calendar date2 = (Calendar) inDate2.clone();
    	date1.add(Calendar.HOUR, -midnightHour);
    	date2.add(Calendar.HOUR, -midnightHour);
    	int year1 = date1.get(Calendar.YEAR);
    	int month1 = date1.get(Calendar.MONTH);
    	int day1 = date1.get(Calendar.DAY_OF_MONTH);
    	int year2 = date2.get(Calendar.YEAR);
    	int month2 = date2.get(Calendar.MONTH);
    	int day2 = date2.get(Calendar.DAY_OF_MONTH);
    	
    	return year1 == year2 && month1 == month2 && day1 == day2;
    }
    
    public static int GetActiveDiffInDays(Calendar oldDate, Calendar newDate, int midnightHour)
    {
    	Calendar oldActDate = null;
    	Calendar newActDate = null;
    	int reverser = 1;
    	if(oldDate.getTimeInMillis() < newDate.getTimeInMillis())
    	{
    		oldActDate = (Calendar) oldDate.clone();
    		newActDate = (Calendar) newDate.clone();
    	}
    	else
    	{
    		reverser = -1;
    		oldActDate = (Calendar) newDate.clone();
    		newActDate = (Calendar) oldDate.clone();
    	}
    	oldActDate.add(Calendar.HOUR, -midnightHour);
    	newActDate.add(Calendar.HOUR, -midnightHour);
    	
    	int newActYear = newActDate.get(Calendar.YEAR);
    	int newActMonth = newActDate.get(Calendar.MONTH);
    	int newActDay = newActDate.get(Calendar.DAY_OF_MONTH);
    	int daysApart = 0;
    	while(true)
    	{

    		
    		int oldActYear = oldActDate.get(Calendar.YEAR);
        	int oldActMonth = oldActDate.get(Calendar.MONTH);
        	int oldActDay = oldActDate.get(Calendar.DAY_OF_MONTH);
        	
        	if(newActYear == oldActYear && newActMonth == oldActMonth && newActDay == oldActDay)
        		break;
    		oldActDate.add(Calendar.HOUR, 24);
        	daysApart++;
    	}
    	
    	return daysApart * reverser;
    }
    
    public static int GetActiveDayOfWeek(Calendar dateIn, int midnightHour)
    {
    	if(dateIn==null)
    		return 0;
    	Calendar curDate = (Calendar) dateIn.clone();
    	curDate.add(Calendar.HOUR, -midnightHour);
    	int day = curDate.get(Calendar.DAY_OF_WEEK)%7;
    	return day;
    }

    public static Calendar GetActiveDate(Calendar dateIn, int midnightHour)
    {
    	if(dateIn == null)
    		return null;
    	
    	Calendar activeDate = (Calendar) dateIn.clone();
    	
    	if(activeDate.get(Calendar.HOUR_OF_DAY) <= midnightHour)
		{
    		activeDate.add(Calendar.HOUR, -24);
		}
    	activeDate.set(Calendar.HOUR_OF_DAY, 12);
    	activeDate.set(Calendar.MINUTE, 0);
    	activeDate.set(Calendar.SECOND, 0);
    	
    	return activeDate;
    }

}
