package com.Dave.Files;

import java.io.FileWriter;
import java.util.Calendar;

import com.Dave.DateStrings.DateStrings;

import android.content.Context;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;


public class ErrorFile
{
	public static String ErrorFile = "ErrorLog.txt";
	
	public static void Write(String who, String error, Context context)
	{
		Calendar date = Calendar.getInstance();
		String entry = String.format("%s - %s: %s\n", DateStrings.GetDateTimeString(date), who, error);
		try
		{
			FileWriter fw = new FileWriter(Environment.getExternalStorageDirectory().getPath() + "/" + ErrorFile, true);
			fw.write(entry);
			fw.close();
		}
		catch (Exception e)
		{
			Log.e("ErrorFile", "Failed to write log entry");
		}
		
		if(context != null)
		{
			Toast t = Toast.makeText(context, entry, Toast.LENGTH_LONG);
			t.show();
		}
	}
	
	public static void WriteException(Exception e, Context context)
	{
		String stackTrace = "";
		for (StackTraceElement ste : e.getStackTrace())
			stackTrace += ste.toString() + "\n";
		
		Calendar date = Calendar.getInstance();
	   	String entry = String.format("%s:\n%s\n%s\n", DateStrings.GetDateTimeString(date), e.getCause(), stackTrace);
	   	try
	   	{
	   		FileWriter fw = new FileWriter(Environment.getExternalStorageDirectory().getPath() + "/" + ErrorFile, true);
	   		fw.write(entry);
	   		fw.close();
	   	}
	   	catch (Exception e1)
	   	{
	   		Log.e("ErrorFile", "Failed to write log entry");
	   	}
	   	
	   	if(context != null)
		{
	   		Toast t = Toast.makeText(context, stackTrace, Toast.LENGTH_LONG);
	   		t.show();
		}
	}
}