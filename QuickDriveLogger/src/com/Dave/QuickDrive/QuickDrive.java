package com.Dave.QuickDrive;

import java.util.Calendar;

import com.Dave.Files.LogFile;
import com.Dave.Files.LoggerConfig;
import com.Dave.Files.LoggerState;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.widget.Toast;

public class QuickDrive extends Activity
{
	//ONLY USE THIS CODE FOR TOGGLES
	public static String ToggleType = "Drive";
	public static int ToggleIndex = -1;
	public static boolean ToggleState = false;
	
	private LoggerConfig mConfig = null;
	private String mConfigFile = "/sdcard/00Logs/DaveConfig.txt";
	private LoggerState mState = null;
	private String mStateFile = "/sdcard/00Logs/DaveTemp.txt";
	private LogFile mLog = null;
	private String mLogFile = "/sdcard/00Logs/DaveLog.txt";
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        
        mConfig = LoggerConfig.FromFile(mConfigFile, getApplicationContext());
        
        mLog = new LogFile(mLogFile, false);
        
        for(int i=0; i<mConfig.Toggles.size(); i++)
        	if(mConfig.Toggles.get(i).equals(ToggleType))
        		ToggleIndex = i;
        
        mState = LoggerState.FromFile(mStateFile, mConfig);
        if(mState == null)
        	mState = LoggerState.Create(mStateFile, mLog.GetLogEntries(), mConfig);
        ToggleState = mState.ToggleStates[ToggleIndex];
        
        PromptToAddLog();
    }
    
    private void PromptToAddLog()
    {
    	String proposedState = "on";
    	if(ToggleState)
    		proposedState = "off";
    	AlertDialog.Builder builder =  new AlertDialog.Builder(this);
    	builder.setMessage("Log " + ToggleType + " " + proposedState + "?");
    	builder.setPositiveButton("Yes", new DialogInterface.OnClickListener()
    	{
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
            	AddLogEntry();
            	finish();
            }
        });
    	builder.setNegativeButton("No", new DialogInterface.OnClickListener()
    	{
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
            	finish();
            }
        });
    	builder.show();
    }
    
    private void AddLogEntry()
    {
    	try
    	{
    		String newState = "on";
        	if(ToggleState)
        		newState = "off";
    
    		int index = mConfig.Toggles.indexOf(ToggleType);
    		mLog.AddLogEntry(Calendar.getInstance(), index + 1000, newState, null, mConfig);
    		mState.UpdateToggle(index, Calendar.getInstance(), !ToggleState, mConfig);
    	
    		Toast t = Toast.makeText(this, "Added " + ToggleType + " " + newState + " entry", Toast.LENGTH_LONG);
    		t.show();
    	}
    	catch(Exception e)
    	{
    		
    	}
    }
}