package com.viggi.lib_file;

/**
 * Created by dave on 8/27/17.
 */

public class LogItem
{
    public String Name = null;
    public boolean IsToggle = false;

    public boolean IsSafe = false;
    public boolean IsValue = false;
    public boolean IsBackup = false;
    public boolean IsLocation = false;

    public int ReminderDays = -1;
    public int TriggerID = -1;

    //For toggles only
    public boolean IsValueOn = false;
    public boolean IsValueOff = false;

    public boolean IsBackupOn = false;
    public boolean IsBackupOff = false;
}
