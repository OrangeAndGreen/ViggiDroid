package com.viggi.lib_file;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Dave on 2/22/2015.
 */
public class LoggerConfig
{
    public String ConfigFilename = "/sdcard/00Logs/DaveConfig.txt";
    public boolean Simulation = true;
    public boolean Debug = true;
    public String Username = "Dave";
    public int AveragingWindow = 30;
    public List<String> Toggles = new ArrayList<String>();
    public List<String> Buttons = new ArrayList<String>();
    public List<Boolean> ButtonValues = new ArrayList<Boolean>();
    public List<String> SafeItems = new ArrayList<String>();
    public List<String> BackupTriggers = new ArrayList<String>();
    public String LogFilePath = "/sdcard/00Logs/DaveLog.txt";
    public String ExportDirectory = "/sdcard/01Files/";
    public int MidnightHour = 5;
    public String EmailAddress = "";
    public String EmailAutoSubject = "Auto Email from Droid Logger";
    public HashMap<Integer, String> Triggers = new HashMap<Integer, String>();

    private LoggerConfig(String filename)
    {
        ConfigFilename = filename;
    }

    public static LoggerConfig FromFile(String filename, Context context)
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

                String[] parts = line.split("=");
                if(parts==null || parts.length < 2)
                    continue;

                String tag = parts[0].trim();
                String value = parts[1].trim();

                if(tag.equals("Username"))
                    ret.Username = value.trim();
                else if(tag.equals("Simulation"))
                {
                    ret.Simulation = value.equals("on");
                    if(!ret.Simulation)
                        ret.Simulation = value.equals("true");
                }
                else if(tag.equals("Debug"))
                {
                    ret.Debug = value.equals("on");
                    if(!ret.Debug)
                        ret.Debug = value.equals("true");
                }
                else if(tag.equals("AveragingWindow"))
                    ret.AveragingWindow = Integer.parseInt(value);
                else if(tag.equals("Toggle"))
                    ret.Toggles.add(value);
                else if(tag.equals("Button") || tag.equals("Value"))
                {
                    ret.Buttons.add(value);
                    ret.ButtonValues.add(tag.equals("Value"));
                }
                else if(tag.equals("Safe"))
                    ret.SafeItems.add(value);
                else if(tag.equals("Backup"))
                    ret.BackupTriggers.add(value);
                else if(tag.equals("LogFilePath"))
                    ret.LogFilePath = value;
                else if(tag.equals("ExportDirectory"))
                    ret.ExportDirectory = value;
                else if(tag.equals("MidnightHour"))
                    ret.MidnightHour = Integer.parseInt(value);
                else if(tag.equals("EmailAddress"))
                    ret.EmailAddress = value;
                else if(tag.equals("EmailAutoSubject"))
                    ret.EmailAutoSubject = value;
                else if(tag.startsWith("Trigger"))
                {
                    String[] tagParts = tag.split(" ");

                    int triggerNum = Integer.parseInt(tagParts[1]);

                    ret.Triggers.put(triggerNum, value);
                }
            }
            br.close();

            Log.i("LoggerConfig", String.format("%d buttons, %d toggles", ret.Buttons.size(), ret.Toggles.size()));

            return ret;
        }
        catch(Exception e)
        {
            //Setup simulation
            Log.e("LoggerConfig", "Failed to load LoggerConfig");
            ErrorFile.WriteException(e, null);

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

        String[] toggles = { "Home", "Sleep", "Wash", "Drive", "Work", "Fish"};

        for(int i=0; i<toggles.length; i++)
            ret.Toggles.add(toggles[i]);

        String[] buttons = { "Caffeine", "Weight", "Eat", "Friend", "Family", "Teeth", "Trash", "Diaper"};
        boolean[] buttonTypes = { false, true, false, false, false, false, false, false};

        for(int i=0; i<buttons.length; i++)
        {
            ret.Buttons.add(buttons[i]);
            ret.ButtonValues.add(buttonTypes[i]);
        }

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
            for(int i=0; i<BackupTriggers.size(); i++)
                fw.write("Backup = " + BackupTriggers.get(i) + "\n");

            fw.close();
        }
        catch(Exception e)
        {
            Log.e("LoggerConfig", "Failed to save file");
        }
    }
}
