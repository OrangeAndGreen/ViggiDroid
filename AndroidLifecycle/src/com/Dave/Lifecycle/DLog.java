package com.Dave.Lifecycle;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Calendar;

import android.content.Context;
import android.widget.Toast;

import com.Dave.DateStrings.DateStrings;

public class DLog
{
	public static void Write(Context context, String filename, Calendar date, String message)
	{
		try
		{
			FileWriter writer = new FileWriter(filename, true);
			writer.append(String.format("%s: %s\n", DateStrings.GetDateTimeString(date), message));
			writer.close();
		}
		catch (Exception e)
		{
			Toast t = Toast.makeText(context, "Failed to write log entry", Toast.LENGTH_SHORT);
			t.show();
		}
	}
	
	public static String Read(Context context, String filename)
	{
		String ret = null;
		try
		{
			File f = new File(filename);
			BufferedReader br = new BufferedReader(new FileReader(f));
			char[] buf = new char[(int)f.length()];
			br.read(buf, 0, (int)f.length());
			br.close();
			ret = String.copyValueOf(buf);
		}
		catch(Exception e)
		{
			Toast t = Toast.makeText(context, "Failed to read log", Toast.LENGTH_SHORT);
			t.show();
		}
		return ret;
	}
	
	public static void Reset(Context context, String filename)
	{
		try
		{
			File f = new File(filename);
			if(f.exists())
				if(!f.delete())
					throw new Exception();
			f.createNewFile();
		}
		catch(Exception e)
		{
			Toast t = Toast.makeText(context, "Failed to reset log", Toast.LENGTH_SHORT);
			t.show();
		}
	}
}