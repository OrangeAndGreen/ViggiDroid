package com.Dave.GasLogger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.widget.Toast;

public class LogFile
{
	public static String LogFilename = "/sdcard/FuelLog.txt";
	
	public static void ImportToDB(DBAdapter db, Context context)
	{
		ArrayList<Fillup> fillups = new ArrayList<Fillup>();
		try
		{
			File f = new File(LogFilename);
			BufferedReader br = new BufferedReader(new FileReader(f));
			String log = "";
			char[] buf = new char[(int)f.length()];
			br.read(buf, 0, (int)f.length());
			br.close();
			log = String.copyValueOf(buf);
			String[] lines = log.split("\n");
			
			for(String line : lines)
			{
				Fillup fillup = Fillup.FromString(line);
				if(fillup != null)
					fillups.add(fillup);
			}
			
		}
		catch(Exception e) {}
		
		int importCount = 0;
		for(Fillup fillup : fillups)
		{
			List<Fillup> results = db.FindFillups(DBAdapter.KEY_DATE, fillup.Date);
			if(results == null || results.size() == 0)
			{
				db.AddFillup(fillup);
				importCount++;
			}
		}
		
		Toast t = Toast.makeText(context, String.format("Imported %d fillups", importCount), Toast.LENGTH_SHORT);
		t.show();
	}
	
	public static void ExportFromDB(DBAdapter db)
	{
		try
    	{
    		FileWriter fw = new FileWriter(LogFilename, false);
		
    		for(Fillup fillup :db.GetAllFillups(true))
    		{
    			fw.write(fillup.toString());
    		}
		
    		fw.close();
    	}
    	catch(Exception e) {}
	}
}
