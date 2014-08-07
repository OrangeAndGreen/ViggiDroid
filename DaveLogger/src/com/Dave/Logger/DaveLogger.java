package com.Dave.Logger;

import java.io.File;
import java.util.Calendar;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.view.ViewStub;
import com.Dave.DateTimeSlider.DateSlider;
import com.Dave.DateTimeSlider.DateTimeSlider;
import com.Dave.DateStrings.DateStrings;
import com.Dave.Files.DebugFile;
import com.Dave.Files.DirectoryPicker;
import com.Dave.Files.ErrorFile;
import com.Dave.Files.LoggerStateFile;
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
 * 
 */

@TargetApi(Build.VERSION_CODES.FROYO)
public class DaveLogger extends Activity implements Runnable
{
	//Tracking Fields
    private LogAdder mAdder = new LogAdder();
    
    private String mStorageDirectory = null;
    private LoggerConfig mConfig = null;
    private String mConfigFile = "Config.txt";
    
    private LoggerStateFile mState = null;
    private String mStateFile = "Temp.txt";

    private LogFile mLog = null;    
    private String mLogFile = "Log.txt";

    private boolean mHadUpdateError = false;

    //Fields to edit when adding new log sets
	private int[] mButtonIds = { R.id.logstub0, R.id.logstub1, R.id.logstub2, R.id.logstub3, R.id.logstub4, R.id.logstub5, R.id.logstub6, R.id.logstub7, R.id.logstub8, R.id.logstub9,
								 R.id.logstub10, R.id.logstub11, R.id.logstub12, R.id.logstub13, R.id.logstub14, R.id.logstub15, R.id.logstub16, R.id.logstub17, R.id.logstub18, R.id.logstub19,
								 R.id.logstub20, R.id.logstub21, R.id.logstub22, R.id.logstub23, R.id.logstub24, R.id.logstub25, R.id.logstub26, R.id.logstub27, R.id.logstub28, R.id.logstub29,
								 R.id.logstub30, R.id.logstub31, R.id.logstub32, R.id.logstub33, R.id.logstub34, R.id.logstub35, R.id.logstub36, R.id.logstub37, R.id.logstub38, R.id.logstub39,
								 R.id.logstub40, R.id.logstub41, R.id.logstub42, R.id.logstub43, R.id.logstub44, R.id.logstub45, R.id.logstub46, R.id.logstub47, R.id.logstub48, R.id.logstub49
								 };
	private int[] mToggleIds = { R.id.toggleStub0, R.id.toggleStub1, R.id.toggleStub2, R.id.toggleStub3, R.id.toggleStub4, R.id.toggleStub5,
								 R.id.toggleStub6, R.id.toggleStub7, R.id.toggleStub8, R.id.toggleStub9, R.id.toggleStub10, R.id.toggleStub11
								 };
    
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
        	SharedPreferences prefs = getPreferences(0);
        	mStorageDirectory = prefs.getString("storageDirectory", null);
        	
        	if(mStorageDirectory == null)
        	{
        		ShowStorageDirectoryPicker();
        	}
        	else
        	{
        		InitApp();
        	}
        }
        catch(Exception e)
        {
        	Debug("Logger", "Error encountered during startup", false);
        	Error(e);
        }
    }
    
    @Override
    protected void onSaveInstanceState (Bundle outState)
    {
    	super.onSaveInstanceState(outState);
    }
    
    private void InitApp()
    {
    	try
        {
        	File dir = new File(mStorageDirectory);
        	if(!dir.exists() && !dir.mkdirs())
        	{
        		Toast t = Toast.makeText(this, "Could not create directory " + mStorageDirectory, Toast.LENGTH_LONG);
        		t.show();
        	}
        	
        	Debug("Logger", "Starting app", false);
        	
        	setContentView(R.layout.main);

        	Debug("Logger", "Loading config file", false);
        	String configPath = mStorageDirectory + "/"+ mConfigFile;
        	mConfig = LoggerConfig.FromFile(configPath, getApplicationContext());
        	if(mConfig == null)
        	{
        		Debug("Logger", "Creating new config at " +configPath, false);
        		
        		Toast t = Toast.makeText(getApplicationContext(), "Creating new config at " + configPath, Toast.LENGTH_SHORT);
        		t.show();
        		
        		mConfig = LoggerConfig.Create(configPath);
        	}
    		int numButtons = mConfig.Buttons.size();
    		int numToggles = mConfig.Toggles.size();
    		
    		Debug("Logger", "Build: " + GetBuildDate(), false);
    		
    		Debug("Logger", String.format("Loaded config from: %s (%d buttons, %d toggles)" , configPath, numButtons, numToggles), false);
    		
    		String logPath = mStorageDirectory + "/" + mLogFile;
        	Debug("Logger", "Loading log file at " + logPath, false);
        	mLog = new LogFile(logPath, false);
        
        	//Create member arrays
        	Debug("Logger", "Creating internal arrays", false);
        	mToggleButtons = new ToggleButton[numToggles];
        	mToggleElapsedLabels = new TextView[numToggles];
        	mLayouts = new View[numButtons];
        	mAddButtons = new Button[numButtons];
        	mSummaryTextViews = new TextView[numButtons];

        	//Find and configure common GUI components
        	Debug("Logger", "Finding GUI components", false);
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
        	Debug("Logger", "Configuring toggles", false);
        	DisplayMetrics metrics = new DisplayMetrics();
        	getWindowManager().getDefaultDisplay().getMetrics(metrics);
        	int width =  metrics.widthPixels;
        	int numForCalc = Math.min(numToggles, 6);
        	int toggleWidth = width / numForCalc;
        	for(int i=0; i<numToggles; i++)
        	{
        		View stub = ((ViewStub) findViewById(mToggleIds[i])).inflate();
        		mToggleButtons[i] = (ToggleButton) stub.findViewById(R.id.toggleButton);
        		mToggleElapsedLabels[i] = (TextView) stub.findViewById(R.id.toggleElapsed);
        		mToggleButtons[i].setOnClickListener(new ToggleListener(i));
        		mToggleButtons[i].setTextOff(mConfig.Toggles.get(i));
        		mToggleButtons[i].setTextOn(mConfig.Toggles.get(i));
        		mToggleButtons[i].setLayoutParams(new LinearLayout.LayoutParams(toggleWidth, LayoutParams.WRAP_CONTENT));
        		mToggleElapsedLabels[i].setLayoutParams(new LinearLayout.LayoutParams(toggleWidth, LayoutParams.WRAP_CONTENT));
        		//mToggleButtons[i].setChecked(false);
        	}

        	//Setup log sets
        	Debug("Logger", "Configuring buttons", false);
        	for(int i=0; i<numButtons; i++)
        	{
        		mLayouts[i] = ((ViewStub) findViewById(mButtonIds[i])).inflate();
        		mAddButtons[i] = (Button) mLayouts[i].findViewById(R.id.addButton);
        		mAddButtons[i].setText(mConfig.Buttons.get(i));
        		mAddButtons[i].setOnClickListener(new AddListener(i, mConfig.ButtonValues.get(i)));
        		mSummaryTextViews[i] = (TextView) mLayouts[i].findViewById(R.id.summaryText);
        	}
        
        	Debug("Logger", "Loading temp file", false);
        	
        	//Internal storage version
        	//mState = LoggerState.Load(getPreferences(0), mConfig);
        	//File version
        	String stateFilePath = mStorageDirectory + "/" + mStateFile;
        	mState = LoggerStateFile.FromFile(stateFilePath, mConfig);
        	
        	if(mState == null)
        	{
        		Debug("Logger", "Temp file not found, creating new", false);
        		//Internal storage version
        		//mState = LoggerState.Create(getPreferences(0), mLog.GetLogEntries(), mConfig, mStorageDirectory);
        		//File version
        		mState = LoggerStateFile.Create(stateFilePath, mLog.GetLogEntries(), mConfig);
        	}
        	
        	for(int i=0; i<numToggles; i++)
        	{
        		mToggleButtons[i].setChecked(mState.ToggleStates[i]);
        	}
        
        	Debug("Logger", "Launching GUI update thread", false);
        	mUpdateThread = new Thread(this);
        	mUpdateThread.start();
        	
        	if(mConfig.SafeItems.size() == 0 || mState.Safe)
        		mSafeButton.setVisibility(View.GONE);
        	
        	if(mState.Safe)
        	{
        		Debug("Logger", "Going into safe mode during startup", false);
        		EnterSafeMode();
        	}
        }
        catch(Exception e)
        {
        	Debug("Logger", "Error encountered during startup", false);
        	Error(e);
        }
    }
    
    private String GetBuildDate()
    {
    	try
    	{
    	     ApplicationInfo ai = getPackageManager().getApplicationInfo(getPackageName(), 0);
    	     ZipFile zf = new ZipFile(ai.sourceDir);
    	     ZipEntry ze = zf.getEntry("classes.dex");
    	     Calendar curDate = Calendar.getInstance();
    	     curDate.setTimeInMillis(ze.getTime());
    	     return DateStrings.GetDateTimeString(curDate);
    	}
    	catch(Exception e)
    	{
    	}
    	
    	return "Unknown";
    }
    
    private void ShowStorageDirectoryPicker()
    {
    	Intent intent = new Intent(this, DirectoryPicker.class);
    	intent.putExtra(DirectoryPicker.ONLY_DIRS, false);
    	intent.putExtra(DirectoryPicker.START_DIR, "/");
    	startActivityForResult(intent, DirectoryPicker.PICK_DIRECTORY);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	if(requestCode == DirectoryPicker.PICK_DIRECTORY && resultCode == RESULT_OK) {
    		Bundle extras = data.getExtras();
    		String path = (String) extras.get(DirectoryPicker.CHOSEN_DIRECTORY);
    		
    		SharedPreferences settings = getPreferences(0);
    	    SharedPreferences.Editor editor = settings.edit();
    	    editor.putString("storageDirectory", path);
    	    editor.commit();
    		
    		mStorageDirectory = path;
    		Debug("Logger", "Storage directory set to " + path, false);
    		
    		Shutdown();
    		InitApp();
    	}
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
    				String summaryText = String.format(Locale.getDefault(), "%d today, last: %s\nHistory: %s, %s, %s",
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
        		Error(e);
        		mHadUpdateError = true;
        	}
    	}
    	else
    		Log.e("DaveLogger", "UpdateSummaries Error");
    }
    
    //Shows the LogViewer Activity
    private void ShowViewer()
    {
    	Debug("Logger", "Starting LogViewer", false);
		Intent i = new Intent(getApplicationContext(), LogViewer.class);
		i.putExtra("safe", mState.Safe);
		i.putExtra("directory", mStorageDirectory);
		i.putExtra("configfile", mConfigFile);
		i.putExtra("logfile", mLogFile);
		startActivity(i);
		//startActivityForResult(i, 0);
    }

    private void EnterSafeMode()
    {
    	try
		{
			mState.Safe = true;
			mState.Save(mConfig);
			//Hide safe button
			mSafeButton.setVisibility(View.GONE);
			
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
			Error(e);
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
    			Debug("Logger", "Logging toggle", false);
    			Calendar gc = Calendar.getInstance();
    			boolean checked =mToggleButtons[mIndex].isChecked();
    			mState.UpdateToggle(mIndex, gc, checked, mConfig);
    			String state = "off";
    			if(checked)
    				state = "on";
    			mLog.AddLogEntry(gc, mIndex+1000, state, null, mConfig);
    			
    			Debug("Logger", "Added toggle entry", false);
    			Toast t = Toast.makeText(getApplicationContext(), "Added toggle entry", Toast.LENGTH_SHORT);
    			t.show();
    		}
			catch(Exception e)
			{
				Error(e);
			}
    	}
    }

    private class AddListener implements View.OnClickListener
    {
    	private int mIndex = -1;
    	private boolean mIsValue = false;
    	
    	public AddListener(int id, boolean isValue)
    	{
    		mIndex = id;
    		mIsValue = isValue;
    	}
    	
    	public void onClick(View v)
    	{
    		try
    		{
    			boolean setDate = mDateCheck.isChecked();
    			boolean addComment = mIsValue || mCommentCheck.isChecked();

    			String withDate = "";
    			if(setDate)
    				withDate = " with custom date";
    			String withComment = "";
    			if(addComment)
    				withComment = " with comment";
    			Debug("Logger", "Logging button" + withDate + withComment, false);
    			
    			mAdder.PromptAndSave(mIndex, setDate, addComment);
    		}
    		catch(Exception e)
    		{
    			Error(e);
    		}
    	}
    }
        
    private class SafeListener implements View.OnClickListener
    {
    	public void onClick(View v)
    	{
    		Debug("Logger", "User entered safe mode", false);
    		EnterSafeMode();
    	}
    }
    
    private class GraphViewListener implements View.OnClickListener
    {
    	public void onClick(View v)
    	{
    		try
    		{
    			Debug("Logger", "User clicked View", false);
    			ShowViewer();
    		}
			catch(Exception e)
			{
				Error(e);
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
    				Debug("Logger", "Showing custom date prompt", false);
    				return new DateTimeSlider(this, mDateTimeSetListener, Calendar.getInstance());
    			}
    			catch(Exception e)
    			{
    				Error(e);
    			}
    			break;
    		case 1:
    			try
    			{
    				Debug("Logger", "Showing comment prompt", false);
    				
    				//DAVE: Think this is how to do an easier comment dialog
    				//AlertDialog.Builder builder = new AlertDialog.Builder(this);
    				// Get the layout inflater
    			    //LayoutInflater inflater = getLayoutInflater();
    			    // Inflate and set the layout for the dialog
    			    // Pass null as the parent view because its going in the dialog layout
    			    //builder.setView(inflater.inflate(R.layout.comment2, null));
    			    // Add action buttons
    			    //builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener()
    			    //{
    			    //    @Override
    			    //    public void onClick(DialogInterface dialog, int id)
    			    //    {
    			            // sign in the user ...
    			    //    }
    			    //});
    			    //builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener()
    			    //{
    			    //    public void onClick(DialogInterface dialog, int id)
    			    //    {
    			        	
    			    //    }
    			    //});      
    			    //return builder.create();
    				
    				return new CommentPrompt(this, mCommentSetListener);
    			}
    			catch(Exception e)
    			{
    				Error(e);
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
    				Error(e);
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
    				Error(e);
    			}
            }
    	};
    
    private class LogAdder
    {
    	/*
    	 * This class is used to show the prompts when adding a log entry 
    	 */
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
    			Comment = null;
    			mState.UpdateEvent(Index, Date, mConfig);
    			UpdateSummaries();
    			Toast t = Toast.makeText(getApplicationContext(), "Added log entry", Toast.LENGTH_SHORT);
    			t.show();
    		}
    		catch(Exception e)
    		{
    			Error(e);
    		}
    	}
    }

    
    
    //Main-menu handling
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
    	Debug("Logger", "Opening options menu", false);
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
        	Debug("Logger", "Showing viewer from options menu", false);
        	ShowViewer();
            return true;
        case R.id.mainmenu_export:
        	Debug("Logger", "Exporting log", false);
        	mLog.ExportLog(mConfig);
        	return true;
        case R.id.mainmenu_directory:
        	Debug("Logger", "Changing directory", false);
        	ShowStorageDirectoryPicker();
        	return true;
        case R.id.mainmenu_email:
        	Debug("Logger", "Emailing log", false);
        	mLog.EmailLog(this, mConfig);
        	return true;
        case R.id.mainmenu_refresh:
        	Debug("Logger", "Refreshing temp file", false);
        	mProgress = ProgressDialog.show(this, "Loading", "Please wait...", true);
        	new Thread(new Runnable()
        	{
				@Override
				public void run()
				{
					//Internal storage version
	        		//mState = LoggerState.Create(getPreferences(0), mLog.GetLogEntries(), mConfig, mStorageDirectory);
	        		//File version
					String stateFilePath = mStorageDirectory + "/" + mStateFile;
	        		mState = LoggerStateFile.Create(stateFilePath, mLog.GetLogEntries(), mConfig);
	        		
					mProgress.dismiss();
				}
        	}).start();
        	return true;
        case R.id.mainmenu_settings:
        	Debug("Logger", "User clicked Settings, not implemented yet", false);
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
    	Debug("Logger", "Logger destroyed", false);
    	Shutdown();
        super.onDestroy();
    }
    
    private void Shutdown()
    {
    	Debug("Logger", "Shutting down", false);
    	mQuitThread = true;
    	try
    	{
    		mUpdateThread.join();
    	}
    	catch(Exception e){}
    	mQuitThread = false;
    }

    
    //Debug helper
    private void Debug(String tag, String message, boolean showToast)
    {
    	Log.i(tag, message);
    	if(mConfig != null && mConfig.Debug)
    	{
    		Context context = null;
    		if(showToast)
    			context = getApplicationContext();
    		DebugFile.Write(mStorageDirectory, tag, message, context);
    	}
    }
    
    private void Error(Exception e)
    {
    	String entry = ErrorFile.WriteException(e, getApplicationContext());
    	Debug("Error", entry, false);
    }
}