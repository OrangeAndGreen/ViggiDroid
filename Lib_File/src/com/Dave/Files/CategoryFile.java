package com.Dave.Files;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Calendar;

import com.Dave.DateStrings.DateStrings;

public class CategoryFile
{
	public String Filename = null;
	private CategoryEntry[] mEntries = null;
	
	public CategoryFile(String filename)
	{
		Filename = filename;
		LoadFile();
	}
	
	public void LoadFile()
	{
		//Get the lines from "Filename" file
		String[] logEntries = new String[0];
		try
		{
			File f = new File(Filename);
			BufferedReader br = new BufferedReader(new FileReader(f));
			String log = "";
			char[] buf = new char[(int)f.length()];
			br.read(buf, 0, (int)f.length());
			br.close();
			log = String.copyValueOf(buf);
			logEntries = log.split("\n");
		}
		catch(Exception e) {}
		
		//Parse the entries into mEntries
		mEntries = new CategoryEntry[logEntries.length];
		for(int i=0; i<logEntries.length; i++)
		{
			String[] parts = logEntries[i].split(" - ");
			int numEntries = Integer.parseInt(parts[0]);
			Calendar startDate = DateStrings.ParseDateTimeString(parts[1]);
			String category = "";
			for(int j=2; j<parts.length; j++)
				category += parts[j];
			mEntries[i] = new CategoryEntry(category, numEntries, startDate);
		}
	}
	
	public void SaveFile()
	{
    	try
    	{
    		FileWriter fw = new FileWriter(Filename, false);
    		
    		for(int i=0; i<mEntries.length; i++)
    			fw.write(String.format("%d - %s - %s\n", mEntries[i].NumEntries, 
    					DateStrings.GetDateTimeString(mEntries[i].StartDate), mEntries[i].Category));
    		
    		fw.close();
    	}
    	catch(Exception e) {}
	}
	
	public String[] GetList()
	{
		if(mEntries == null)
		{
			return new String[0];
		}
		
		String[] ret = new String[mEntries.length];
		
		//Calculate the frequency of each category
		float[] frequencies = new float[mEntries.length];
		for(int i=0; i<mEntries.length; i++)
		{
			CategoryEntry curEntry = mEntries[i];
			if(curEntry.StartDate != null && curEntry.NumEntries > 0)
			{
				float elapsed = (Calendar.getInstance().getTimeInMillis() - curEntry.StartDate.getTimeInMillis()) / 1000;
				frequencies[i] = curEntry.NumEntries / elapsed;
			}
		}
		
		//Sort the frequencies in descending order
		for(int i=0; i<mEntries.length; i++)
		{
			float bestVal = -1;
			int bestIndex = 0;
			for(int j=0; j<mEntries.length; j++)
			{
				if(frequencies[i] > bestVal)
				{
					bestVal = frequencies[i];
					bestIndex = i;
				}
			}
			ret[i] = mEntries[bestIndex].Category;
			frequencies[bestIndex] = -1;
		}
		
		return ret;
	}
	
	public void UpdateCategory(String category)
	{
		//Create an empty mEntries if it is null
		if(mEntries == null)
		{
			mEntries = new CategoryEntry[0];
		}
		
		//Increment the NumEntries if the category exists
		for(int i=0; i<mEntries.length; i++)
		{
			if(mEntries[i].Category.equals(category))
			{
				mEntries[i].NumEntries++;
				return;
			}
		}
		
		//Add a new category to the end of mEntries
		CategoryEntry[] newList = new CategoryEntry[mEntries.length + 1];
		for(int i = 0; i<mEntries.length; i++)
			newList[i] = mEntries[i];
		newList[mEntries.length] = new CategoryEntry(category, 1, Calendar.getInstance());
		mEntries = newList;
		
		SaveFile();
	}
	
	private class CategoryEntry
	{
		public String Category;
		public int NumEntries;
		public Calendar StartDate;
		
		public CategoryEntry(String category, int numEntries, Calendar startDate)
		{
			Category = category;
			NumEntries = numEntries;
			StartDate = startDate;
		}
	}
}
