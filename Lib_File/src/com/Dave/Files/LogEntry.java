package com.Dave.Files;

import java.util.Calendar;
import com.Dave.DateStrings.DateStrings;

public class LogEntry
{
	private String Date = null;
	private String Type = null;
	private String Comment = null;
	private Calendar mDate = null;
	private int mIndex = -1;
	private int mToggleIndex = -1;
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
	
	public void SetDate(String date)
	{
		Date = date;
		mDate = null;
	}
	
	public String GetDateString()
	{
		return Date;
	}
	
	public int GetId(LoggerConfig config)
	{
		mIndex = config.Buttons.indexOf(Type.trim());
		return mIndex;
	}
	
	public int GetToggleId(LoggerConfig config)
	{
		mToggleIndex = config.Toggles.indexOf(Type.trim());
		return mToggleIndex;
	}
	
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
	
	public void SetType(String type)
	{
		Type = type;
		mIndex = -1;
	}
	
	public String GetComment()
	{
		return Comment;
	}
	
	public void SetComment(String inComment)
	{
		Comment = inComment;
	}
}
