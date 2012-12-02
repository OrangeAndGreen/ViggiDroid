package com.Dave.Logger;

import java.io.File;
import java.util.Calendar;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.view.ViewStub;
import com.Dave.DateTimeSlider.DateSlider;
import com.Dave.DateTimeSlider.DateTimeSlider;
import com.Dave.DateStrings.DateStrings;
import com.Dave.Files.ErrorFile;
import com.Dave.Files.LoggerState;
import com.Dave.Files.LoggerConfig;
import com.Dave.Files.LogFile;

/*
 * HINTS:
 * 		-Control-F12 rotates display to landscape in emulator
 * 		-Use Log. (d, v, i, etc.) to write messages to emulator debug log
 *		-When a GUI component can't be found:
 *			Verify correct XML layout loaded in setContentView
 * 			Sometimes the IDE just needs to be reloaded
 * 			Make sure the correct packages are listed in the manifest
 * 			Pass AttributeSet to super() in View constructor
 * 
 * To do:
 *************************************************************************************
 ** -A different, more-efficient kind of layout:
 **		No config for buttons, all adaptive
 **		Main interface:
 **			-Customizable quick links (using the old toggles and buttons)
 **			-Toggle "interface" (interface = new window? menu? popup?)
 **			-Event "interface"
 **			-Stats and Graphs
 **				Select from combobox of: existing histories or all-of-the-above
 **				Select from combobox of: stats, historical graph, daily histogram (by time), weekly histogram (by daily total)
 **			-Editor
 **				View recent log history
 **				Add/delete entries
 **				Add/edit extra info
 **			-Settings
 **				-Setup user-customizable quick links for events/toggles
 **				
 **		When interface clicked, list:
 **			-Textbox to start typing (type a new event/toggle anytime)
 **			-Toggles: list of active toggles (click to de-activate)
 **			-List of most common previous events/toggles (click to fire/activate)
 **			-Checkboxes to add extra info (friend's name, blood pressure)
 **				Remember history of extras to suggest i.e. common friend's names
 *************************************************************************************
 * 
 * -ConfigFile setting for logger's name (i.e. for DaveLog, or email subject)
 * -Better curve fitting (multiple humps)
 * 
 * -New activity to adjust config settings
 * -Configure extra log options: string (i.e. friend name), int (i.e. weight)
 * 		-Log "other" smoke
 * -Stats to get:
 *		-Average interval (minus 7 hours sleep)
 * -Move log-adding dialogs into LogFile library
 * 		CommentPrompt and comment.xml
 * 		LogFile references DateTimeSlider instead of Logger
 * -Better log storage:
 * 		-DATABASE, with methods to import/export text file
 * 		-Break log files by month
 * 		-Automatically store a second file for backup (interval, i.e. once/day?)
 * -Better graphing:
 * 		-Different graph histories (daily rate, total/day, daily average/week)
 * 				-X-axis: date, Y-axis: 0-24 hours (with 5AM offset) with points at each event/toggle time-of-day
 * 		-Graph options
 * 				-Time span (start and end dates)
 * 				-Time intervals (seconds, minutes, hours, days, weeks, months, years)
 * 		-Zoom/Scroll graph
 * 		-Allow plotting multiple graphs at once, use radio buttons
 * 		-Optionally shade background of graph by percentage (i.e. green for most-common, red for most-rare)
 * 		-Vary horizontal labels depending on history length (days, day-of-month on saturdays, month on first-of-month, year)
 * -Eventually, protect secret logs better:
 *    Run in two modes right from startup
 *    Either way, show "hide" button
 *    If nothing to hide or in safe mode, say:
 *         "Nothing to hide!"
 *    Once in safe mode, password to restore
 *         Always prompt for password
 *    Different passwords could enable different
 *         different secrets...
 * 
 * BUGS:
 * 		-Crash when viewing toggle after adding entry
 * 
 * 20120730 Refactoring:
 * 		-Fix LogViewer
 */

public class DaveLogger extends Activity implements Runnable
{
	//Tracking Fields
	public static boolean Safe = false;
    private LogAdder mAdder = new LogAdder();
    
    private String mRootDirectory = null;
    private String mStorageDirectory = "Logger";//"00Logs/";
    private LoggerConfig mConfig = null;
    private String mConfigFile = "Config.txt";
    
    private LoggerState mState = null;
    private String mStateFile = "Temp.txt";

    private LogFile mLog = null;    
    private String mLogFile = "Log.txt";

    private boolean mHadUpdateError = false;

    //Fields to edit when adding new log sets
	private int[] mButtonIds = { R.id.logstub0, R.id.logstub1, R.id.logstub2, R.id.logstub3,
								 R.id.logstub4, R.id.logstub5, R.id.logstub6, R.id.logstub7,
								 R.id.logstub8, R.id.logstub9, R.id.logstub10 };
	private int[] mToggleIds = { R.id.toggleStub0, R.id.toggleStub1, R.id.toggleStub2,
								 R.id.toggleStub3, R.id.toggleStub4, R.id.toggleStub5 };
    
    //GUI components
	private TextView mIntroText = null;
	private CheckBox mDateCheck = null;
	private CheckBox mCommentCheck = null;
	private Button mSafeButton = null;
	private Button mViewButton = null;
	private TextView mDebugText = null;
	private ToggleButton[] mToggleButtons = null;
	private TextView[] mToggleElapsedLabels = null;
	private View[] mLayouts = null;
	private Button[] mAddButtons = null;
	private TextView[] mSummaryTextViews = null;
    
	//Timed update thread
    private Thread mUpdateThread = null;
    private boolean mQuitThread = false;
    
    
    //Called when the activity is first created
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        try
        {
        	if(savedInstanceState != null)
            {
        		Log.i("Logger", "Reloaded bundle");
            }
        	
        	setContentView(R.layout.main);

        	mRootDirectory = Environment.getExternalStorageDirectory().getPath() + '/' + mStorageDirectory + "/";
        	
        	File dir = new File(mRootDirectory);
        	if(!dir.exists() && !dir.mkdirs())
        	{
        		Toast t = Toast.makeText(this, "Could not create directory " + mRootDirectory, Toast.LENGTH_LONG);
        		t.show();
        	}
        	
        	mConfig = LoggerConfig.FromFile(mRootDirectory + mConfigFile);
        	if(mConfig == null)
        	{
        		Toast t = Toast.makeText(this, "Creating new config at " + mRootDirectory + mConfigFile, Toast.LENGTH_LONG);
        		t.show();
        		
        		mConfig = LoggerConfig.Create(mRootDirectory + mConfigFile);
        	}
    		
        	mLog = new LogFile(mRootDirectory + mLogFile, false);
        
        	int numLogsets = mConfig.Buttons.size();
        
        	//Create member arrays
        	mToggleButtons = new ToggleButton[mConfig.Toggles.size()];
        	mToggleElapsedLabels = new TextView[mConfig.Toggles.size()];
        	mLayouts = new View[numLogsets];
        	mAddButtons = new Button[numLogsets];
        	mSummaryTextViews = new TextView[numLogsets];

        	//Find and configure common GUI components
        	mIntroText = (TextView) findViewById(R.id.introText);
        	mDateCheck = (CheckBox) findViewById(R.id.dateCheck);
        	mCommentCheck = (CheckBox) findViewById(R.id.commentCheck);
        	mSafeButton = (Button) findViewById(R.id.safeButton);
        	mSafeButton.setOnClickListener(new SafeListener());
        	mViewButton = (Button) findViewById(R.id.viewButton);
        	mViewButton.setOnClickListener(new GraphViewListener());
        	mDebugText = (TextView) findViewById(R.id.debugText);
        	mDebugText.setText("");
        	
        	//Setup toggle sets
        	for(int i=0; i<mConfig.Toggles.size(); i++)
        	{
        		View stub = ((ViewStub) findViewById(mToggleIds[i])).inflate();
        		mToggleButtons[i] = (ToggleButton) stub.findViewById(R.id.toggleButton);
        		mToggleElapsedLabels[i] = (TextView) stub.findViewById(R.id.toggleElapsed);
        		mToggleButtons[i].setOnClickListener(new ToggleListener(i));
        		mToggleButtons[i].setTextOff(mConfig.Toggles.get(i));
        		mToggleButtons[i].setTextOn(mConfig.Toggles.get(i));
        		//mToggleButtons[i].setChecked(false);
        	}

        	//Setup log sets
        	for(int i=0; i<numLogsets; i++)
        	{
        		mLayouts[i] = ((ViewStub) findViewById(mButtonIds[i])).inflate();
        		mAddButtons[i] = (Button) mLayouts[i].findViewById(R.id.addButton);
        		mAddButtons[i].setText(mConfig.Buttons.get(i));
        		mAddButtons[i].setOnClickListener(new AddListener(i));
        		mSummaryTextViews[i] = (TextView) mLayouts[i].findViewById(R.id.summaryText);
        	}
        
        	mState = LoggerState.FromFile(mRootDirectory + mStateFile, mConfig);
        	if(mState == null)
        	{
        		Log.i("DaveLogger", "Temp file not found, creating new");
        		mState = LoggerState.Create(mRootDirectory + mStateFile, mLog.GetLogEntries(), mConfig);
        	}
        	
        	for(int i=0; i<mConfig.Toggles.size(); i++)
        	{
        		mToggleButtons[i].setChecked(mState.ToggleStates[i]);
        	}
        
        	mUpdateThread = new Thread(this);
        	mUpdateThread.start();
        	
        	Safe = mState.Safe;
        	
        	if(mConfig.SafeItems.size() == 0 || mState.Safe)
        		mSafeButton.setVisibility(View.GONE);
        	
        	if(mState.Safe)
        		EnterSafeMode();
        }
        catch(Exception e)
        {
        	ErrorFile.WriteException(e, this);
        }
    }
    
    @Override
    protected void onSaveInstanceState (Bundle outState)
    {
    	super.onSaveInstanceState(outState);
    }
    
    //Called to update the displayed text
    private void UpdateSummaries()
    {
    	if(!mHadUpdateError)
    	{
    		try
    		{
    			Calendar now = Calendar.getInstance();
    			mIntroText.setText(DateStrings.GetPresentableDateTimeString(now));
    			for(int index=0; index<mConfig.Buttons.size(); index++)
    			{
    				//Log.e("DaveLogger", "Check " + mState.ActiveDate.toString());
    				String hist1 = DateStrings.GetElapsedTimeString(mState.EventRecentHistories[index][0], now, 2);
    				String hist2 = DateStrings.GetElapsedTimeString(mState.EventRecentHistories[index][1], now, 2);
    				String hist3 = DateStrings.GetElapsedTimeString(mState.EventRecentHistories[index][2], now, 2);
    				String summaryText = String.format("%d today, last: %s\nHistory: %s, %s, %s",
    						mState.EventDailyCounts[index],
    						DateStrings.GetPrintableDateTimeString(mState.EventRecentHistories[index][0], mConfig.MidnightHour),
    						hist1, hist2, hist3);
    				mSummaryTextViews[index].setText(summaryText);
    			}
    			for(int i=0; i<mConfig.Toggles.size(); i++)
    			{
    				//Errors.Write("Logger Summary", "Toggle " + ConfigFile.Toggles.get(i) + " = " + TempFile.ToggleStates[i]);
    				mToggleButtons[i].setChecked(mState.ToggleStates[i]);
    				String elapsedLabel = DateStrings.GetElapsedTimeString(mState.ToggleLastDates[i], now, 2);
    				mToggleElapsedLabels[i].setText(elapsedLabel);
    			}
    		}
        	catch(Exception e)
        	{
        		Log.e("DaveLogger", String.format("UpdateSummaries Error: %s\n%s", e.toString(), e.getStackTrace().toString()));
        		ErrorFile.WriteException(e, this);
        		mHadUpdateError = true;
        	}
    	}
    	else
    		Log.e("DaveLogger", "UpdateSummaries Error");
    }
    
    //Shows the LogViewer Activity
    private void ShowViewer()
    {
    	Log.i("DaveLogger", "Starting LogViewer");
		Intent i = new Intent(DaveLogger.this, LogViewer.class);
		startActivity(i);
		//startActivityForResult(i, 0);
    }

    private void EnterSafeMode()
    {
    	try
		{
    		Safe = true;
			mState.Safe = true;
			mState.Save(mConfig);
			//Hide safe button
			mSafeButton.setVisibility(View.GONE);
			//Hide smoke logset
			
			for(int x = 0; x<mConfig.SafeItems.size(); x++)
			{
				int foundId = -1;
				for(int i=0; i<mConfig.Buttons.size(); i++)
					if(mConfig.Buttons.get(i).equals(mConfig.SafeItems.get(x)))
						foundId = i;
				if(foundId >= 0)
				{
					mLayouts[foundId].setVisibility(View.GONE);
				}
			}
		}
		catch(Exception e)
		{
			ErrorFile.WriteException(e, this);
		}
    }
    
    
    //Button Listeners
    private class ToggleListener implements View.OnClickListener
    {
    	private int mIndex = -1;
    	
    	public ToggleListener(int id)
    	{
    		mIndex = id;
    	}
    	public void onClick(View v)
    	{
    		try
    		{
    			Calendar gc = Calendar.getInstance();
    			boolean checked =mToggleButtons[mIndex].isChecked();
    			mState.UpdateToggle(mIndex, gc, checked, mConfig);
    			String state = "off";
    			if(checked)
    				state = "on";
    			mLog.AddLogEntry(gc, mIndex+1000, state, null, mConfig);
    			Toast t = Toast.makeText(getApplicationContext(), "Added toggle entry", Toast.LENGTH_SHORT);
    			t.show();
    		}
			catch(Exception e)
			{
				ErrorFile.WriteException(e, getApplicationContext());
			}
    	}
    }

    private class AddListener implements View.OnClickListener
    {
    	private int mIndex = -1;
    	
    	public AddListener(int id)
    	{
    		mIndex = id;
    	}
    	
    	public void onClick(View v)
    	{
    		try
    		{
    			boolean setDate = mDateCheck.isChecked();
    			boolean addComment = mCommentCheck.isChecked();

    			mAdder.PromptAndSave(mIndex, setDate, addComment);
    		}
    		catch(Exception e)
    		{
    			ErrorFile.WriteException(e, getApplicationContext());
    		}
    	}
    }
        
    private class SafeListener implements View.OnClickListener
    {
    	public void onClick(View v)
    	{
    		EnterSafeMode();
    	}
    }
    
    private class GraphViewListener implements View.OnClickListener
    {
    	public void onClick(View v)
    	{
    		try
    		{
    			ShowViewer();
    		}
			catch(Exception e)
			{
				ErrorFile.WriteException(e, getApplicationContext());
			}
    	}
    }

    
    
    //Custom log entry handling
    @Override
    protected Dialog onCreateDialog(int id)
    {
    	switch(id)
    	{
    		case 0:
    			try
    			{
    				return new DateTimeSlider(this, mDateTimeSetListener, Calendar.getInstance());
    			}
    			catch(Exception e)
    			{
    				ErrorFile.WriteException(e, getApplicationContext());
    			}
    			break;
    		case 1:
    			try
    			{
    				return new CommentPrompt(this, mCommentSetListener);
    			}
    			catch(Exception e)
    			{
    				ErrorFile.WriteException(e, getApplicationContext());
    			}
    			break;
    	}
    	return null;
    }

    private DateSlider.OnDateSetListener mDateTimeSetListener = new DateSlider.OnDateSetListener()
    	{
            public void onDateSet(DateSlider view, Calendar selectedDate)
            {
            	try
            	{
            		mAdder.Date = selectedDate;
            		mAdder.GetCommentAndSave();
            	}
    			catch(Exception e)
    			{
    				ErrorFile.WriteException(e, getApplicationContext());
    			}
            }
    	};
    
    private CommentPrompt.OnCommentSetListener mCommentSetListener = new CommentPrompt.OnCommentSetListener()
    	{
            public void onCommentSet(CommentPrompt view, String comment)
            {
            	try
            	{
            		mAdder.Comment = comment;
            		mAdder.SaveLog();
            	}
    			catch(Exception e)
    			{
    				ErrorFile.WriteException(e, getApplicationContext());
    			}
            }
    	};
    
    private class LogAdder
    {
    	public int Index = -1;
    	public Calendar Date = null;
    	public String Comment = null;
    	private boolean mDate = false;
    	private boolean mComment = false;
    	
    	public void PromptAndSave(int index, boolean customDate, boolean getComment)
    	{
    		Index = index;
    		Date = Calendar.getInstance();
    		mDate = customDate;
    		mComment = getComment;
    		if(mDate)
    			showDialog(0);
    		else
    			GetCommentAndSave();
    	}
    	
    	public void GetCommentAndSave()
    	{
    		if(mComment)
    			showDialog(1);
    		else
    			SaveLog();
    	}
    	
    	public void SaveLog()
    	{
    		try
    		{
    			mLog.AddLogEntry(Date, Index, null, Comment, mConfig);
    			mState.UpdateEvent(Index, Date, mConfig);
    			UpdateSummaries();
    			Toast t = Toast.makeText(getApplicationContext(), "Added log entry", Toast.LENGTH_SHORT);
    			t.show();
    		}
    		catch(Exception e)
    		{
    			ErrorFile.WriteException(e, getApplicationContext());
    		}
    	}
    }

    
    
    //Main-menu handling
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }
    
    private ProgressDialog mProgress = null;
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle item selection
        switch (item.getItemId())
        {
        case R.id.mainmenu_view:
        	ShowViewer();
            return true;
        case R.id.mainmenu_export:
        	mLog.ExportLog(mConfig);
        	return true;
        case R.id.mainmenu_email:
        	mLog.EmailLog(this, mConfig);
        	return true;
        case R.id.mainmenu_refresh:
        	mProgress = ProgressDialog.show(this, "Loading", "Please wait...", true);
        	new Thread(new Runnable()
        	{
				@Override
				public void run()
				{
					mState = LoggerState.Create(mRootDirectory + mStateFile, mLog.GetLogEntries(), mConfig);
					mProgress.dismiss();
				}
        	}).start();
        	return true;
        case R.id.mainmenu_settings:
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }
    
    //Runs the main update loop for the clock and elapsed times
    public void run()
    {
    	while(!mQuitThread)
    	{
        	handler.sendEmptyMessage(0);
        	try { Thread.sleep(1000); }
        		catch (Exception e) {}
    	}
    }
    
    private Handler handler = new Handler()
    {
        @Override
        public void handleMessage(Message msg)
        {
        	UpdateSummaries();
        }
    };

    
    
    //Called when the activity exits
    @Override
    public void onDestroy()
    {
    	Shutdown();
        super.onDestroy();
    }
    
    private void Shutdown()
    {
    	mQuitThread = true;
    	try
    	{
    		mUpdateThread.join();
    	}
    	catch(Exception e){}
    }

}