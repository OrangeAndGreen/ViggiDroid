package com.viggi.lib_logger;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.graphics.Color;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewStub;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ListPopupWindow;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.ConnectionResult;
//import com.google.android.gms.common.GooglePlayServicesClient;
//import com.google.android.gms.common.GooglePlayServicesClient.ConnectionCallbacks;
//import com.google.android.gms.common.GooglePlayServicesClient.OnConnectionFailedListener;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.plus.Plus;
import com.example.dave.lib_logger.R;
import com.google.android.gms.drive.Drive;
import com.viggi.lib_dateslider.DateSlider;
import com.viggi.lib_dateslider.DateTimeSlider;
import com.viggi.lib_datestring.DateStrings;
import com.viggi.lib_file.DebugFile;
import com.viggi.lib_file.DirectoryPicker;
import com.viggi.lib_file.ErrorFile;
import com.viggi.lib_file.LogFile;
import com.viggi.lib_file.LoggerConfig;
import com.viggi.lib_file.LoggerStateFile;

import java.io.File;
import java.util.Calendar;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.crashlytics.android.Crashlytics;
import io.fabric.sdk.android.Fabric;

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

//Method for splitting app into lite/full versions using a common library came from here:
//http://www.donnfelker.com/howto-android-full-and-lite-versions/

public class LoggerActivity extends FragmentActivity implements Runnable, AdapterView.OnItemClickListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener
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

    private int mStatsSelector = 0;

    //Fields to edit when adding new log sets
    private int[] mButtonIds = { R.id.logstub0, R.id.logstub1, R.id.logstub2, R.id.logstub3, R.id.logstub4, R.id.logstub5, R.id.logstub6, R.id.logstub7, R.id.logstub8, R.id.logstub9,
            R.id.logstub10, R.id.logstub11, R.id.logstub12, R.id.logstub13, R.id.logstub14, R.id.logstub15, R.id.logstub16, R.id.logstub17, R.id.logstub18, R.id.logstub19,
            R.id.logstub20, R.id.logstub21, R.id.logstub22, R.id.logstub23, R.id.logstub24, R.id.logstub25, R.id.logstub26, R.id.logstub27, R.id.logstub28, R.id.logstub29,
            R.id.logstub30, R.id.logstub31, R.id.logstub32, R.id.logstub33, R.id.logstub34, R.id.logstub35, R.id.logstub36, R.id.logstub37, R.id.logstub38, R.id.logstub39,
            R.id.logstub40, R.id.logstub41, R.id.logstub42, R.id.logstub43, R.id.logstub44, R.id.logstub45, R.id.logstub46, R.id.logstub47, R.id.logstub48, R.id.logstub49
    };
    private int[] mToggleIds = { R.id.toggleStub0, R.id.toggleStub1, R.id.toggleStub2, R.id.toggleStub3, R.id.toggleStub4, R.id.toggleStub5,
            R.id.toggleStub6, R.id.toggleStub7, R.id.toggleStub8, R.id.toggleStub9
    };

    //GUI components
    private TextView mIntroText = null;
    private CheckBox mDateCheck = null;
    private CheckBox mCommentCheck = null;
    private Button mSafeButton = null;
    private Button mMenuButton = null;
    private ListPopupWindow mPopup;
    private String[] mMenuOptions={"Backup", "Refresh", "Directory", "Email", "Settings"};
    //private Button mViewButton = null;
    //private TextView mDebugText = null;
    //private TextView[] mToggleLabels = null;
    private ToggleButton[] mToggleButtons = null;
    private TextView[] mToggleElapsedLabels = null;
    private View[] mLayouts = null;
    //private Button[] mAddButtons = null;
    private TextView[] mSummaryTextViews = null;

    //Timed update thread
    private Thread mUpdateThread = null;
    private boolean mQuitThread = false;

    private boolean mFullVersion = false;

    private GoogleApiClient mGoogleApiClient = null;
    private static final int REQUEST_CODE_RESOLUTION = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try
        {
            //Debug("Logger", "App starting", false);
            Fabric.with(this, new Crashlytics());

            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(Drive.API)
                    .addScope(Drive.SCOPE_FILE)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();

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
    protected void onStart()
    {
        super.onStart();
        //mGoogleApiClient.connect();
    }

    @Override
    protected void onSaveInstanceState (Bundle outState)
    {
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onConnected(Bundle var1)
    {
        Toast.makeText(this, "Connected!", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onConnectionSuspended(int var1)
    {
        Toast.makeText(this, "onConnectionSuspended", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onConnectionFailed(ConnectionResult result)
    {
        // An unresolvable error has occurred and a connection to Google APIs
        // could not be established. Display an error message, or handle
        // the failure silently
        //Toast.makeText(this, "onConnectionFailed: " + result.getErrorCode(), Toast.LENGTH_SHORT).show();

        //GoogleApiAvailability.getInstance().getErrorDialog(this, result.getErrorCode(), 0).show();

        if (result.hasResolution())
        {
            try
            {
                result.startResolutionForResult(this, REQUEST_CODE_RESOLUTION);
                //Toast.makeText(this, "onConnectionFailed 4", Toast.LENGTH_SHORT).show();
            }
            catch (IntentSender.SendIntentException e)
            {
                // Unable to resolve, message user appropriately
                Toast.makeText(this, "onConnectionFailed 3", Toast.LENGTH_SHORT).show();
            }
        }
        else
        {
            //GooglePlayServicesUtil.getErrorDialog(result.getErrorCode(), this, 0).show();
            Toast.makeText(this, "onConnectionFailed 2", Toast.LENGTH_SHORT).show();
        }
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

            setContentView(R.layout.activity_logger);

            mFullVersion = !getPackageName().toLowerCase().contains("lite");
            if(!mFullVersion)
            {
                Toast.makeText(this, (mFullVersion ? "Full" : "Lite") + " version", Toast.LENGTH_SHORT).show();
            }

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
            Button[] addButtons = new Button[numButtons];
            mSummaryTextViews = new TextView[numButtons];

            //Find and configure common GUI components
            Debug("Logger", "Finding GUI components", false);
            mIntroText = (TextView) findViewById(R.id.introText);
            mDateCheck = (CheckBox) findViewById(R.id.dateCheck);
            mCommentCheck = (CheckBox) findViewById(R.id.commentCheck);
            mSafeButton = (Button) findViewById(R.id.safeButton);
            mSafeButton.setOnClickListener(new SafeListener());

            mMenuButton = (Button) findViewById(R.id.menuButton);
            mMenuButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Debug("Logger", "User clicked Menu button", true);
                    mPopup.show();
                }
            });

            mPopup = new ListPopupWindow(this);
            mPopup.setAdapter(new ArrayAdapter(this, R.layout.list_item, mMenuOptions));
            mPopup.setAnchorView(mMenuButton);
            mPopup.setWidth(300);
            mPopup.setHeight(400);
            mPopup.setModal(true);
            mPopup.setOnItemClickListener(this);








            Button viewButton = (Button) findViewById(R.id.viewButton);
            viewButton.setOnClickListener(new GraphViewListener());
            //TextView debugText = (TextView) findViewById(R.id.debugText);
            //debugText.setText("");

            //Setup toggle sets
            Debug("Logger", "Configuring toggles", false);
            DisplayMetrics metrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(metrics);
            int width =  metrics.widthPixels;
            int numForCalc = Math.min(numToggles, 5);
            int toggleWidth = width / numForCalc;
            for(int i=0; i<numToggles; i++)
            {
                View stub = ((ViewStub) findViewById(mToggleIds[i])).inflate();
                mToggleButtons[i] = (ToggleButton) stub.findViewById(R.id.toggleButton);
                mToggleElapsedLabels[i] = (TextView) stub.findViewById(R.id.toggleElapsed);
                mToggleButtons[i].setOnClickListener(new ToggleListener(i));
                mToggleButtons[i].setTextOff(mConfig.Toggles.get(i));
                mToggleButtons[i].setTextOn(mConfig.Toggles.get(i));
                mToggleButtons[i].setLayoutParams(new LinearLayout.LayoutParams(toggleWidth, LinearLayout.LayoutParams.WRAP_CONTENT));
                mToggleElapsedLabels[i].setLayoutParams(new LinearLayout.LayoutParams(toggleWidth, LinearLayout.LayoutParams.WRAP_CONTENT));
                //mToggleButtons[i].setChecked(false);
            }

            //Setup log sets
            Debug("Logger", "Configuring buttons", false);
            //LinearLayout listLayout = (LinearLayout) findViewById(R.id.listLayout);
            for(int i=0; i<numButtons; i++)
            {
                //mLayouts[i] = LayoutInflater.from(this).inflate(R.layout.logstub, null);
                mLayouts[i] = ((ViewStub) findViewById(mButtonIds[i])).inflate();
                addButtons[i] = (Button) mLayouts[i].findViewById(R.id.addButton);
                addButtons[i].setText(mConfig.Buttons.get(i));
                addButtons[i].setOnClickListener(new AddListener(i, mConfig.ButtonValues.get(i)));
                mSummaryTextViews[i] = (TextView) mLayouts[i].findViewById(R.id.summaryText);
                mSummaryTextViews[i].setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mStatsSelector++;
                    }
                });

                //listLayout.addView(mLayouts[i]);
            }

            Debug("Logger", "Loading temp file", false);

            //Internal storage version
            //mState = LoggerState.Load(getPreferences(0), mConfig);
            //File version
            String stateFilePath = mStorageDirectory + "/" + mStateFile;
            try
            {
                mState = LoggerStateFile.FromFile(stateFilePath, mConfig);
            }
            catch(Exception e)
            {
                Debug("Logger", String.format("Error loading temp file: %s", e.toString()), false);
            }

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

                //Need to clean this up, duplicated elsewhere in this file
                mToggleButtons[i].setTextColor(mState.ToggleStates[i] ? Color.CYAN : Color.WHITE);
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
            Debug("LoggerActivity", "Error getting build date for app", false);
        }

        return "Unknown";
    }

    private void ShowStorageDirectoryPicker()
    {
        Intent intent = new Intent(this, DirectoryPicker.class);
        intent.putExtra(DirectoryPicker.ONLY_DIRS, false);
        //intent.putExtra(DirectoryPicker.START_DIR, "/");
        startActivityForResult(intent, DirectoryPicker.PICK_DIRECTORY);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if(requestCode == DirectoryPicker.PICK_DIRECTORY && resultCode == RESULT_OK)
        {
            Bundle extras = data.getExtras();
            String path = (String) extras.get(DirectoryPicker.CHOSEN_DIRECTORY);

            SharedPreferences settings = getPreferences(0);
            SharedPreferences.Editor editor = settings.edit();
            editor.putString("storageDirectory", path);
            editor.apply();

            mStorageDirectory = path;
            Debug("Logger", "Storage directory set to " + path, false);

            Shutdown();
            InitApp();
        }
        else if(requestCode == REQUEST_CODE_RESOLUTION)
        {
            Toast.makeText(this, "Check: " + resultCode, Toast.LENGTH_SHORT).show();
            if (resultCode == RESULT_OK)
            {
                mGoogleApiClient.connect();
            }
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
                    if(mStatsSelector > 4)
                        mStatsSelector = 0;

                    //Log.e("DaveLogger", "Check " + mState.ActiveDate.toString());

                    String summaryText = String.format(Locale.getDefault(), " %d today\n",
                            mState.EventRecentCounts[index][0]);

                    switch(mStatsSelector)
                    {
                        case 0:
                        {
                            summaryText = String.format(Locale.getDefault(), "%s     Last (1): %s, %s",
                                    summaryText,
                                    DateStrings.GetElapsedTimeString(mState.EventRecentHistories[index][0], now, 2),
                                    DateStrings.GetPrintableDateTimeString(mState.EventRecentHistories[index][0], mConfig.MidnightHour));

                            break;
                        }
                        case 1:
                        {
                            summaryText = String.format(Locale.getDefault(), "%s     Last (2): %s, %s",
                                    summaryText,
                                    DateStrings.GetElapsedTimeString(mState.EventRecentHistories[index][1], now, 2),
                                    DateStrings.GetPrintableDateTimeString(mState.EventRecentHistories[index][1], mConfig.MidnightHour));

                            break;
                        }
                        case 2:
                        {
                            summaryText = String.format(Locale.getDefault(), "%s     Last (3): %s, %s",
                                    summaryText,
                                    DateStrings.GetElapsedTimeString(mState.EventRecentHistories[index][2], now, 2),
                                    DateStrings.GetPrintableDateTimeString(mState.EventRecentHistories[index][2], mConfig.MidnightHour));

                            break;
                        }
                        case 3:
                        {
                            String totalsString = "";
                            for(int j=1; j<LoggerStateFile.RecentTotalsHistoryLength; j++)
                            {
                                if(j > 1)
                                {
                                    totalsString += ",  ";
                                }

                                totalsString += String.format("%d", mState.EventRecentCounts[index][j]);
                            }

                            summaryText = String.format(Locale.getDefault(), "%s     Recent days: %s",
                                    summaryText,
                                    totalsString);

                            break;
                        }
                        case 4:
                        {
                            summaryText = String.format(Locale.getDefault(), "%s     All-time: %.02f/day",
                                    summaryText,
                                    mState.EventAllTimeAverages[index]);

                            break;
                        }
                        default:
                        {
                            summaryText = "ERROR";
                            break;
                        }
                    }

                    mSummaryTextViews[index].setText(summaryText);
                }
                for(int i=0; i<mConfig.Toggles.size(); i++)
                {
                    //Errors.Write("Logger Summary", "Toggle " + ConfigFile.Toggles.get(i) + " = " + TempFile.ToggleStates[i]);
                    mToggleButtons[i].setChecked(mState.ToggleStates[i]);

                    //Need to clean this up, duplicated elsewhere in this file
                    mToggleButtons[i].setTextColor(mState.ToggleStates[i] ? Color.CYAN : Color.WHITE);

                    String elapsedLabel = DateStrings.GetElapsedTimeString(mState.ToggleLastDates[i], now, 2);
                    mToggleElapsedLabels[i].setText(elapsedLabel);
                }
            }
            catch(Exception e)
            {
                String stackTrace = "";
                for(StackTraceElement part : e.getStackTrace())
                {
                    stackTrace += part.toString() + "\n";
                }
                Log.e("DaveLogger", String.format("UpdateSummaries Error: %s\n%s", e.toString(), stackTrace));
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

    //Shows the Settings Activity
    private void ShowSettings()
    {
        Debug("Logger", "Starting Config Activity", false);
        Intent i = new Intent(getApplicationContext(), LoggerConfigActivity.class);
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

                //Need to clean this up, duplicated elsewhere in this file
                mToggleButtons[mIndex].setTextColor(checked ? Color.CYAN : Color.WHITE);

                Debug("Logger", "Added toggle entry", false);
                Toast t = Toast.makeText(getApplicationContext(), "Added toggle entry", Toast.LENGTH_SHORT);
                t.show();

                String triggerString = mConfig.Toggles.get(mIndex) + " " + (checked ? "on" : "off");
                for(int i=0; i<mConfig.BackupTriggers.size(); i++)
                {
                    if(mConfig.BackupTriggers.get(i).equals(triggerString))
                    {
                        backupLogFile();
                    }
                }
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

                for(int i=0; i<mConfig.BackupTriggers.size(); i++)
                {
                    if(mConfig.BackupTriggers.get(i).equals(mConfig.Buttons.get(Index)))
                    {
                        backupLogFile();
                    }
                }
            }
            catch(Exception e)
            {
                Error(e);
            }
        }
    }

    public void backupLogFile()
    {
        Intent intent = new Intent(android.content.Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://" + mLog.LogFilename));
        startActivity(Intent.createChooser(intent, ""));
    }


    //Main-menu handling
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id)
    {
        //productName.setText(products[position]);
        mPopup.dismiss();

        //private String[] mMenuOptions={"Backup", "Refresh", "Directory", "Email", "Settings"};
        switch(position)
        {
            case 0:
                backupLogFile();
                break;
            case 1:
                Debug("Logger", "Refreshing temp file", false);
                mProgress = ProgressDialog.show(this, "Loading", "Please wait...", true);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        //Internal storage version
                        //mState = LoggerState.Create(getPreferences(0), mLog.GetLogEntries(), mConfig, mStorageDirectory);
                        //File version
                        try
                        {
                            String stateFilePath = mStorageDirectory + "/" + mStateFile;
                            mState = LoggerStateFile.Create(stateFilePath, mLog.GetLogEntries(), mConfig);

                            mProgress.dismiss();
                        }
                        catch(Exception e)
                        {
                            Error(e);
                        }
                    }
                }).start();
                break;
            case 2:
                Debug("Logger", "Changing directory", false);
                ShowStorageDirectoryPicker();
                break;
            case 3:
                Debug("Logger", "Emailing log", false);
                mLog.EmailLog(this, mConfig);
                break;
            case 4:
                Debug("Logger", "User clicked Settings", false);
                ShowSettings();
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        Debug("Logger", "Opening options menu", false);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_logger, menu);
        return true;
    }

    private ProgressDialog mProgress = null;

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        //Can't use a switch here because we're in a library
        //http://stackoverflow.com/questions/8476912/menu-item-ids-in-an-android-library-project

        int id = item.getItemId();

        // Handle item selection
        if (id == R.id.mainmenu_view) {
            Debug("Logger", "Showing viewer from options menu", false);
            ShowViewer();
            return true;
        }
        if (id == R.id.mainmenu_export) {
            Debug("Logger", "Exporting log", false);
            mLog.ExportLog(mConfig);
            return true;
        }
        if (id == R.id.mainmenu_directory) {
            Debug("Logger", "Changing directory", false);
            ShowStorageDirectoryPicker();
            return true;
        }
        if (id == R.id.mainmenu_email) {
            Debug("Logger", "Emailing log", false);
            mLog.EmailLog(this, mConfig);
            return true;
        }
        if (id == R.id.mainmenu_refresh) {
            Debug("Logger", "Refreshing temp file", false);
            mProgress = ProgressDialog.show(this, "Loading", "Please wait...", true);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    //Internal storage version
                    //mState = LoggerState.Create(getPreferences(0), mLog.GetLogEntries(), mConfig, mStorageDirectory);
                    //File version
                    String stateFilePath = mStorageDirectory + "/" + mStateFile;
                    mState = LoggerStateFile.Create(stateFilePath, mLog.GetLogEntries(), mConfig);

                    mProgress.dismiss();
                }
            }).start();
            return true;
        }
        if (id == R.id.mainmenu_settings) {
            Debug("Logger", "User clicked Settings", false);
            ShowSettings();
            return true;
        }

        return super.onOptionsItemSelected(item);
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
