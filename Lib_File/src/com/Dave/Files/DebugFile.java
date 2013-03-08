package com.Dave.Files;

import java.io.FileWriter;
import java.util.Calendar;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.Dave.DateStrings.DateStrings;

public class DebugFile
{
	public static String Filename = "Debug.txt";
	
	public static void Write(String directory, String tag, String message, Context context)
	{
		Calendar date = Calendar.getInstance();
		String entry = String.format("%s - %s: %s\n", DateStrings.GetDateTimeString(date), tag, message);
		try
		{
			FileWriter fw = new FileWriter(directory + Filename, true);
			fw.write(entry);
			fw.close();
		}
		catch (Exception e)
		{
			Log.e("DebugFile", "Failed to write log entry");
		}
		
		if(context != null)
		{
			Toast t = Toast.makeText(context, entry, Toast.LENGTH_LONG);
			t.show();
		}
	}
}
