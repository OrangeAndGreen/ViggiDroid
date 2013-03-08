package com.Dave.Sudoku;

import java.io.FileWriter;
import java.util.Calendar;

import com.Dave.DateStrings.DateStrings;

import android.content.Context;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

public class DebugLog
{
	public static String Filename = "SudokuLog.txt";
	public static boolean Enable = true;
	
	public static String Write(String message, Context context)
	{
		String fullPath = "";
		if(Enable)
		{
			Calendar date = Calendar.getInstance();
			String entry = String.format("%s: %s\n", DateStrings.GetDateTimeString(date), message);
			fullPath = Environment.getExternalStorageDirectory().getPath() + "/" + Filename;
			try
			{
				FileWriter fw = new FileWriter(fullPath, true);
				fw.write(entry);
				fw.close();
			}
			catch (Exception e)
			{
				Log.e("DebugFile", "Log failed: " + entry);
				//Log.e("DebugFile", "File: " + fullPath);
				
				//if(context != null)
				//{
				//	Toast t = Toast.makeText(context, e.getMessage(), Toast.LENGTH_LONG);
				//	t.show();
				//}
			}
			
			if(context != null)
			{
				Toast t = Toast.makeText(context, entry, Toast.LENGTH_LONG);
				t.show();
			}
		}
		
		return fullPath;
	}
}
