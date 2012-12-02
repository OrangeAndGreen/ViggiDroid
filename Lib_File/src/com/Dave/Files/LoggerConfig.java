package com.Dave.Files;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

import android.util.Log;

public class LoggerConfig
{
	public String ConfigFilename = "/sdcard/00Logs/DaveConfig.txt";
	public boolean Simulation = true;
	public boolean Debug = true;
	public String Username = "Dave";
	public int AveragingWindow = 30;
	public List<String> Toggles = new ArrayList<String>();
	public List<String> Buttons = new ArrayList<String>();
	public List<String> SafeItems = new ArrayList<String>();
	public String LogFilePath = "/sdcard/00Logs/DaveLog.txt";
	public String ExportDirectory = "/sdcard/01Files/";
	public int MidnightHour = 5;
	public String EmailAddress = "";
	public String EmailAutoSubject = "Auto Email from Droid Logger";

	private LoggerConfig(String filename)
	{
		ConfigFilename = filename;
	}
	
	public static LoggerConfig FromFile(String filename)
	{
		Log.i("LoggerConfig", "Loading from file " + filename);
		
		LoggerConfig ret = new LoggerConfig(filename);
		
		try
    	{
    		BufferedReader br = new BufferedReader(new FileReader(ret.ConfigFilename));
    		ret.Toggles.clear();
    		ret.Buttons.clear();
    		
    		while(true)
    		{
    			String line = br.readLine();
    			if(line == null)
    				break;
    			
    			String[] parts = line.split(" = ");
    			if(parts==null)
    				continue;
    			
    			if(parts[0].equals("Username"))
    				ret.Username = parts[1].trim();
    			if(parts[0].equals("Simulation"))
    			{
    				ret.Simulation = parts[1].trim().equals("on");
    				if(!ret.Simulation)
    					ret.Simulation = parts[1].trim().equals("true");
    			}
    			if(parts[0].equals("Debug"))
    			{
    				ret.Debug = parts[1].trim().equals("on");
    				if(!ret.Debug)
    					ret.Debug = parts[1].trim().equals("true");
    			}
    			if(parts[0].equals("AveragingWindow"))
    				ret.AveragingWindow = Integer.parseInt(parts[1].trim());
    			if(parts[0].equals("Toggle"))
    				ret.Toggles.add(parts[1].trim());
    			if(parts[0].equals("Button"))
    				ret.Buttons.add(parts[1].trim());
    			if(parts[0].equals("Safe"))
    				ret.SafeItems.add(parts[1].trim());
    			if(parts[0].equals("LogFilePath"))
    				ret.LogFilePath = parts[1].trim();
    			if(parts[0].equals("ExportDirectory"))
    				ret.ExportDirectory = parts[1].trim();
    			if(parts[0].equals("MidnightHour"))
    				ret.MidnightHour = Integer.parseInt(parts[1].trim());
    			if(parts[0].equals("EmailAddress"))
    				ret.EmailAddress = parts[1].trim();
    			if(parts[0].equals("EmailAutoSubject"))
    				ret.EmailAutoSubject = parts[1].trim();
    		}
    		br.close();
    		
    		Log.i("LoggerConfig", String.format("%d buttons, %d toggles", ret.Buttons.size(), ret.Toggles.size()));
    		
    		return ret;
    	}
    	catch(Exception e)
    	{
    		//Setup simulation
    		Log.e("LoggerConfig", "Failed to load LoggerConfig");
    		
    		return null;
    	}
	}
	
	public static LoggerConfig Create(String filename)
	{
		Log.i("LoggerConfig", "Creating new config");
		
		LoggerConfig ret = new LoggerConfig(filename);
		
		ret.Simulation = false;
		ret.Username = "Me";
		
		String directory = filename.substring(0, filename.lastIndexOf('/'));
		ret.LogFilePath = directory + "/Log.txt";
		ret.ExportDirectory = directory + '/';
		
		ret.Toggles.add("Home");
		ret.Toggles.add("Sleep");
		ret.Toggles.add("Wash");
		ret.Toggles.add("Drive");
		ret.Toggles.add("Work");
		ret.Toggles.add("Fish");
		
		ret.Buttons.add("Caffeine");
		ret.Buttons.add("Eat");
		ret.Buttons.add("Friend");
		ret.Buttons.add("Family");
		ret.Buttons.add("Teeth");
		ret.Buttons.add("Trash");
		ret.Buttons.add("Diaper");

		ret.Save();
		
		return ret;
	}
    
    public void Save()
    {
    	Log.i("LoggerConfig", "Saving file");
    	
    	try
    	{
    		FileWriter fw = new FileWriter(ConfigFilename, false);
    		
    		String sim = "off";
    		if(Simulation)
    			sim="on";
    		fw.write("Simulation = " + sim + "\n");
    		
    		String debug = "off";
    		if(Debug)
    			debug="on";
    		fw.write("Debug = " + debug + "\n");
    		
    		fw.write("Username = " + Username + "\n");
    		fw.write("LogFilePath = " + LogFilePath + "\n");
    		fw.write("ExportDirectory = " + ExportDirectory + "\n");
    		fw.write("AveragingWindow = " + AveragingWindow + "\n");
    		fw.write("MidnightHour = " + MidnightHour + "\n");
    		fw.write("EmailAddress = " + EmailAddress + "\n");
    		fw.write("EmailAutoSubject = " + EmailAutoSubject + "\n");
    		
    		for(int i=0; i<Toggles.size(); i++)
    			fw.write("Toggle = " + Toggles.get(i) + "\n");
    		for(int i=0; i<Buttons.size(); i++)
    			fw.write("Button = " + Buttons.get(i) + "\n");
    		for(int i=0; i<SafeItems.size(); i++)
    			fw.write("Safe = " + SafeItems.get(i) + "\n");
    		
    		fw.close();
    	}
    	catch(Exception e)
    	{
    		Log.e("LoggerConfig", "Failed to save file");
    	}
    }
}
