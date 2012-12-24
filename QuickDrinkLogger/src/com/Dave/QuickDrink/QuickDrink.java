package com.Dave.QuickDrink;

import java.util.Calendar;

import com.Dave.Files.LogFile;
import com.Dave.Files.LoggerConfig;
import com.Dave.Files.LoggerState;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

public class QuickDrink extends Activity
{
	//ONLY USE THIS CODE FOR EVENTS
	public static String EventType = "Drink";
	
	private String mConfigFile = "/sdcard/00Logs/DaveConfig.txt";
	private String mStateFile = "/sdcard/00Logs/DaveTemp.txt";
	private String mLogFile = "/sdcard/00Logs/DaveLog.txt";
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        
        PromptToAddLog();
    }
    
    private void PromptToAddLog()
    {
    	AlertDialog.Builder builder =  new AlertDialog.Builder(this);
    	builder.setMessage("Log " + EventType + "?");
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
    		LoggerConfig config = LoggerConfig.FromFile(mConfigFile, getApplicationContext());
    	    
    		LogFile log = new LogFile(mLogFile, false);
    
    		int index = config.Buttons.indexOf(EventType);
    		log.AddLogEntry(Calendar.getInstance(), index, null, null, config);
    		
    		LoggerState state = LoggerState.FromFile(mStateFile, config);
    		if(state == null)
    			state = LoggerState.Create(mStateFile, log.GetLogEntries(), config);
    		state.UpdateEvent(index, Calendar.getInstance(), config);
    	
    		Toast t = Toast.makeText(this, String.format("Added %s entry (%d today)",
    				EventType, state.EventDailyCounts[index]), Toast.LENGTH_LONG);
    		t.show();
    	}
    	catch(Exception e)
    	{
    		Log.e("QuickDrink", "Failed to add log entry");
    	}
    }
}