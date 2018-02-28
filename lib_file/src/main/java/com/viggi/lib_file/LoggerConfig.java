package com.viggi.lib_file;

import android.util.Log;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Created by Dave on 2/22/2015.
 * Holds config info for the logger
 */
public class LoggerConfig
{
    private String ConfigFilename = null;
    private boolean Simulation = true;
    public boolean SafeMode = false;
    public boolean Debug = true;
    public boolean LogGPS = false;
    public boolean LogPhone = false;
    private String Username = "Dave";
    public int AveragingWindow = 30;
    public int MidnightHour = 5;
    public String EmailAddress = "";
    public String EmailAutoSubject = "Auto Email from Droid Logger";

    public List<LogItem> Items = new ArrayList<>();

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
            ret.Items.clear();

            while(true)
            {
                String line = br.readLine();
                if(line == null)
                    break;

                if(line.startsWith("//"))
                {
                    continue;
                }

                String[] parts = line.split("=");
                if(parts.length < 2)
                    continue;

                String tagString = parts[0].trim();
                String value = parts[1].trim();

                //General keys: Username, AveragingWindow, MidnightHour, EmailAddress, EmailAutoSubject, Debug, GPS, Simulation
                switch(tagString)
                {
                    case "Username":
                        ret.Username = value.trim();
                        break;
                    case "AveragingWindow":
                        ret.AveragingWindow = Integer.parseInt(value);
                        break;
                    case "MidnightHour":
                        ret.MidnightHour = Integer.parseInt(value);
                        break;
                    case "EmailAddress":
                        ret.EmailAddress = value;
                        break;
                    case "EmailAutoSubject":
                        ret.EmailAutoSubject = value;
                        break;
                    case "Debug":
                        ret.Debug = value.equals("on");
                        if(!ret.Debug)
                            ret.Debug = value.equals("true");
                        break;
                    case "GPS":
                        ret.LogGPS = value.equals("on");
                        if(!ret.LogGPS)
                            ret.LogGPS = value.equals("true");
                        break;
                    case "Phone":
                        ret.LogPhone = value.equals("on");
                        if(!ret.LogPhone)
                            ret.LogPhone = value.equals("true");
                        break;
                    case "Simulation":
                        ret.Simulation = value.equals("on");
                        if(!ret.Simulation)
                            ret.Simulation = value.equals("true");
                        break;
                    default:
                        //Remaining assumption is that this is a log item
                        String[] tagComponents = tagString.split(",");
                        List<String> allTags = new ArrayList<>();

                        //Trim white space from all the entries (to handle ", ")
                        for(int i=0; i<tagComponents.length; i++)
                        {
                            allTags.add(i, tagComponents[i].trim());
                        }

                        if(allTags.contains("Button") || allTags.contains("Toggle"))
                        {
                            //See if we already created this item
                            LogItem item = null;
                            for (LogItem searchItem: ret.Items)
                            {
                                if(searchItem.Name.equals(value))
                                {
                                    item = searchItem;
                                    break;
                                }
                            }

                            if(item == null)
                            {
                                item = new LogItem();
                                item.Name = value;
                                ret.Items.add(item);
                            }

                            //LogItem keys: Toggle, Button, Safe, Location, Backup/On/Off, Value/On/Off, Remind #, Trigger #
                            if(allTags.contains("Toggle"))
                            {
                                item.IsToggle = true;
                            }
                            if(allTags.contains("Button"))
                            {
                                item.IsToggle = false;
                            }
                            if(allTags.contains("Safe"))
                            {
                                item.IsSafe = true;
                            }
                            if(allTags.contains("Location"))
                            {
                                item.IsLocation = true;
                            }
                            if(allTags.contains("Backup"))
                            {
                                item.IsBackup = true;
                            }
                            if(allTags.contains("BackupOn"))
                            {
                                item.IsBackupOn = true;
                            }
                            if(allTags.contains("BackupOff"))
                            {
                                item.IsBackupOff = true;
                            }
                            if(allTags.contains("Value"))
                            {
                                item.IsValue = true;
                            }
                            if(allTags.contains("ValueOn"))
                            {
                                item.IsValueOn = true;
                            }
                            if(allTags.contains("ValueOff"))
                            {
                                item.IsValueOff = true;
                            }

                            //Now search for the tags with dynamic names
                            for (String tag: allTags)
                            {
                                if(tag.startsWith("Trigger"))
                                {
                                    String[] tagParts = tag.split(" ");

                                    item.TriggerID = Integer.parseInt(tagParts[1]);
                                }
                                else if(tag.startsWith("Remind"))
                                {
                                    String[] tagParts = tag.split(" ");

                                    item.ReminderDays = Integer.parseInt(tagParts[1]);
                                }
                            }
                        }
                        break;
                }
            }
            br.close();

            //Log.i("LoggerConfig", String.format("%d buttons, %d toggles", ret.Buttons.size(), ret.Toggles.size()));
            Log.i("LoggerConfig", String.format("%d items", ret.Items.size()));

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

        String[] toggles = { "Home", "Sleep", "Wash", "Drive", "Work", "Fish"};

        for(String toggleName : toggles)
        {
            LogItem toggle = new LogItem();
            toggle.Name = toggleName;
            toggle.IsToggle = true;
            ret.Items.add(toggle);
        }

        String[] buttons = { "Caffeine", "Weight", "Eat", "Friend", "Family", "Teeth", "Trash", "Diaper"};
        boolean[] buttonTypes = { false, true, false, false, false, false, false, false};

        for(int i=0; i<buttons.length; i++)
        {
            LogItem event = new LogItem();
            event.Name = buttons[i];
            event.IsValue = buttonTypes[i];
            ret.Items.add(event);
        }

        ret.Save();

        return ret;
    }

    private void Save()
    {
        Log.i("LoggerConfig", "Saving file");

        try
        {
            FileWriter fw = new FileWriter(ConfigFilename, false);

            //General keys: Username, AveragingWindow, MidnightHour, EmailAddress, EmailAutoSubject, Debug, GPS, Simulation
            fw.write("Simulation = " + (Simulation ? "on" : "off") + "\n");
            fw.write("Debug = " + (Debug ? "on" : "off") + "\n");
            fw.write("GPS = " + (LogGPS ? "on" : "off") + "\n");
            fw.write("Phone = " + (LogPhone ? "on" : "off") + "\n");
            fw.write("Username = " + Username + "\n");
            fw.write("AveragingWindow = " + AveragingWindow + "\n");
            fw.write("MidnightHour = " + MidnightHour + "\n");
            fw.write("EmailAddress = " + EmailAddress + "\n");
            fw.write("EmailAutoSubject = " + EmailAutoSubject + "\n");

            for(LogItem item : Items)
            {
                //LogItem keys: Toggle, Button, Safe, Location, Backup/On/Off, Value/On/Off, Remind #, Trigger #
                String tags = item.IsToggle ? "Toggle" : "Button";
                if(item.IsSafe)
                    tags += ",Safe";
                if(item.IsLocation)
                    tags += ",Location";
                if(item.IsValue)
                    tags += ",Value";
                if(item.IsValueOn)
                    tags += ",ValueOn";
                if(item.IsValueOff)
                    tags += ",ValueOff";
                if(item.IsBackup)
                    tags += ",Backup";
                if(item.IsBackupOn)
                    tags += ",BackupOn";
                if(item.IsBackupOff)
                    tags += ",BackupOff";
                if(item.ReminderDays > 0)
                    tags += String.format(Locale.getDefault(), ",Remind %d", item.ReminderDays);
                if(item.TriggerID > 0)
                    tags += String.format(Locale.getDefault(), ",Trigger %d", item.TriggerID);

                fw.write(tags + " = " + item.Name + "\n");
            }

            fw.close();
        }
        catch(Exception e)
        {
            Log.e("LoggerConfig", "Failed to save file");
        }
    }

    public LogItem GetEntryByName(String name)
    {
        for(LogItem item : Items)
        {
            if(item.Name.equals(name))
            {
                return item;
            }
        }

        return null;
    }
}
