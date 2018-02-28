package com.viggi.lib_logger;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.provider.CallLog;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ListPopupWindow;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

//import com.google.android.gms.cast.framework.media.uicontroller.UIMediaController;
import com.google.android.gms.common.ConnectionResult;
//import com.google.android.gms.common.GoogleApiAvailability;
//import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
//import com.google.android.gms.common.ConnectionResult;
//import com.google.android.gms.common.GooglePlayServicesClient;
//import com.google.android.gms.common.GooglePlayServicesClient.ConnectionCallbacks;
//import com.google.android.gms.common.GooglePlayServicesClient.OnConnectionFailedListener;
//import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
//import com.google.android.gms.plus.Plus;
import com.example.dave.lib_logger.R;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.crash.FirebaseCrash;
import com.viggi.lib_dateslider.DateSlider;
import com.viggi.lib_dateslider.DateTimeSlider;
import com.viggi.lib_datestring.DateStrings;
import com.viggi.lib_file.DebugFile;
import com.viggi.lib_file.DirectoryPicker;
import com.viggi.lib_file.ErrorFile;
import com.viggi.lib_file.LogFile;
import com.viggi.lib_file.LogItem;
import com.viggi.lib_file.LoggerConfig;
import com.viggi.lib_file.LoggerStateFile;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

//Crashlytics
import com.crashlytics.android.Crashlytics;
import com.viggi.lib_file.LoggerStateItem;
import com.viggi.lib_file.PhoneLogFile;

import io.fabric.sdk.android.Fabric;
//import io.fabric.sdk.android.Logger;

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
    private Boolean mViewAsGrid = true;

    //Tracking Fields
    private LogAdder mAdder = new LogAdder();

    private String mStorageDirectory = null;
    private LoggerConfig mConfig = null;
    private String mConfigFile = "Config.txt";

    private LoggerStateFile mState = null;
    private String mStateFile = "Temp.txt";

    private LogFile mLog = null;
    private String mLogFile = "Log.txt";

    private PhoneLogFile mPhoneLog = null;
    private String mPhoneLogFile = "PhoneLog.txt";

    private boolean mHadUpdateError = false;

    private int mStatsSelector = 0;

    //GUI components
    private TextView mIntroText = null;
    private CheckBox mDateCheck = null;
    private CheckBox mCommentCheck = null;
    private Button mSafeButton = null;
    private ListPopupWindow mPopup;
    private String[] mMenuOptions={"Backup", "Refresh", "Directory", "Email", "Settings"};

    private HashMap<String, ToggleButton> mToggleButtonMap = new HashMap<>();
    private HashMap<String, TextView> mToggleLabelMap = new HashMap<>();
    private HashMap<String, Button> mEventButtonMap = new HashMap<>();
    private HashMap<String, TextView> mEventLabelMap = new HashMap<>();

    //Timed update thread
    private Thread mUpdateThread = null;
    private boolean mQuitThread = false;

    private boolean mFullVersion = false;

    private GoogleApiClient mGoogleApiClient = null;
    private static final int REQUEST_CODE_RESOLUTION = 3;

    private Location mLocation = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try
        {
            Log.i("Logger", "App starting up");

            //Toast.makeText(this, "Hi Mike!!!", Toast.LENGTH_LONG).show();

            //Crashlytics
            Fabric.with(this, new Crashlytics());

            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(Drive.API)
                    .addScope(Drive.SCOPE_FILE)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();

            LoadAppStorage();

            if(mStorageDirectory == null)
            {
                ShowStorageDirectoryPicker();
            }
            else
            {
                InitApp(mViewAsGrid);
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
    public void onConnectionFailed(@NonNull ConnectionResult result)
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

    public void LoadAppStorage()
    {
        SharedPreferences prefs = getPreferences(0);
        mStorageDirectory = prefs.getString("storageDirectory", null);
        mViewAsGrid = prefs.getBoolean("gridView", true);
    }

    public void SaveAppStorage()
    {
        SharedPreferences settings = getPreferences(0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("storageDirectory", mStorageDirectory);
        editor.putBoolean("gridView", mViewAsGrid);
        editor.apply();
    }

    private void InitApp(Boolean viewAsGrid)
    {
        try
        {
            Shutdown();

            mViewAsGrid = viewAsGrid;
            mStatsSelector = 0;

            File dir = new File(mStorageDirectory);
            if(!dir.exists() && !dir.mkdirs())
            {
                Toast t = Toast.makeText(this, "Could not create directory " + mStorageDirectory, Toast.LENGTH_LONG);
                t.show();
            }

            Debug("Logger", "Starting app", false);

            setContentView(R.layout.activity_logger2);

            mFullVersion = !getPackageName().toLowerCase().contains("lite");
            if(!mFullVersion)
            {
                Toast.makeText(this, (mFullVersion ? "Full" : "Lite") + " version", Toast.LENGTH_SHORT).show();
            }

            Debug("Logger", "Loading config file", false);
            String configPath = mStorageDirectory + "/"+ mConfigFile;
            mConfig = LoggerConfig.FromFile(configPath);
            if(mConfig == null)
            {
                Debug("Logger", "Creating new config at " +configPath, false);

                Toast t = Toast.makeText(getApplicationContext(), "Creating new config at " + configPath, Toast.LENGTH_SHORT);
                t.show();

                mConfig = LoggerConfig.Create(configPath);
            }

            Debug("Logger", "Build: " + GetBuildDate(), false);

            Debug("Logger", String.format(Locale.getDefault(), "Loaded config from: %s (%d items)" , configPath, mConfig.Items.size()), false);


            String logPath = mStorageDirectory + "/" + mLogFile;
            Debug("Logger", "Loading log file at " + logPath, false);
            mLog = new LogFile(logPath, false);

            String phoneLogPath = mStorageDirectory + "/" + mPhoneLogFile;
            Debug("Logger", "Loading phone log file at " + phoneLogPath, false);
            mPhoneLog = new PhoneLogFile(phoneLogPath);

            //Find and configure common GUI components
            LoadMainUIComponents(viewAsGrid);

            ConfigureGPS();

            LoadStateFile();

            if(mConfig.LogPhone)
            {
                LogPhoneCalls();
            }

            LoadLogItemUIComponents(viewAsGrid);

            for(LogItem item : mConfig.Items)
            {
                if(item.IsToggle)
                {
                    LoggerStateItem stateItem = mState.GetEntryByName(item.Name);

                    boolean state = false;
                    if(stateItem != null)
                    {
                        state = stateItem.ToggleState;
                    }
                    UpdateToggle(item, state);
                }
            }

            int numSafe = 0;
            for(LogItem safeItem : mConfig.Items)
            {
                if(safeItem.IsSafe)
                {
                    numSafe ++;
                }
            }
            if(numSafe == 0 || mState.Safe)
                mSafeButton.setVisibility(View.GONE);

            //Check for expired reminders (key=name, value=reminderDays)

            List<String> reminders = new ArrayList<>();
            for(LogItem item : mConfig.Items)
            {
                if(item.ReminderDays > 0)
                {
                    LoggerStateItem stateItem = mState.GetEntryByName(item.Name);
                    Calendar last = stateItem.RecentHistory[0];
                    Calendar now = Calendar.getInstance();

                    long elapsed = now.getTimeInMillis() - last.getTimeInMillis();
                    elapsed /= (1000 * 3600 * 24);

                    if(elapsed > item.ReminderDays)
                    {
                        reminders.add(String.format(Locale.getDefault(), "%s: %d day%s", item.Name, (int)elapsed, elapsed >= 2 ? "s" : ""));
                    }
                }
            }

            //See if we need to show any reminders
            if(reminders.size() > 0)
            {
                String message = "Overdue entries:\n";
                for(String reminder : reminders)
                {
                    message += reminder + "\n";
                }

                Toast t = Toast.makeText(this, message, Toast.LENGTH_LONG);
                t.show();
            }

            Debug("Logger", "Launching GUI update thread", false);
            mUpdateThread = new Thread(this);
            mUpdateThread.start();
        }
        catch(Exception e)
        {
            Debug("Logger", "Error encountered during startup", false);
            Error(e);
        }
    }

    private void LoadMainUIComponents(boolean viewAsGrid)
    {
        Debug("Logger", "Finding GUI components", false);
        mIntroText = (TextView) findViewById(R.id.introText);
        mDateCheck = (CheckBox) findViewById(R.id.dateCheck);
        mCommentCheck = (CheckBox) findViewById(R.id.commentCheck);
        mSafeButton = (Button) findViewById(R.id.safeButton);
        mSafeButton.setOnClickListener(new SafeListener());

        if(mState != null && mState.Safe)
        {
            mSafeButton.setVisibility(View.GONE);
        }

        CheckBox gridCheck = (CheckBox) findViewById(R.id.gridCheck);
        gridCheck.setChecked(viewAsGrid);
        gridCheck.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                InitApp(b);

                SaveAppStorage();
            }
        });

        Button menuButton = (Button) findViewById(R.id.menuButton);
        menuButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Debug("Logger", "User clicked Menu button", false);
                mPopup.show();
            }
        });

        mPopup = new ListPopupWindow(this);
        mPopup.setAdapter(new ArrayAdapter<>(this, R.layout.list_item, mMenuOptions));
        mPopup.setAnchorView(menuButton);
        mPopup.setWidth(400);
        mPopup.setHeight(600);
        mPopup.setModal(true);
        mPopup.setOnItemClickListener(this);

        Button viewButton = (Button) findViewById(R.id.viewButton);
        viewButton.setOnClickListener(new GraphViewListener());
    }

    private void LoadLogItemUIComponents(boolean viewAsGrid)
    {
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        LinearLayout container = (LinearLayout) findViewById(R.id.buttonContainer);

        LinearLayout listLayout = (LinearLayout)View.inflate(this, mViewAsGrid ? R.layout.logger_view_grid : R.layout.logger_view_list, null);
        container.addView(listLayout);

        int cellID = 1;
        int buttonsPerRow = 4;
        LinearLayout grid = null;
        LinearLayout row = null;
        int insertIndex = 0;

        int width =  metrics.widthPixels;
        int buttonWidth = width / buttonsPerRow;

        if(mViewAsGrid)
        {
            grid = (LinearLayout) listLayout.findViewById(R.id.grid_container);
            row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            grid.addView(row);
        }

        for(LogItem item : mConfig.Items)
        {
            try
            {
                if(!mState.Safe || !item.IsSafe)
                {
                    if(viewAsGrid && insertIndex % buttonsPerRow == 0)
                    {
                        row = new LinearLayout(this);
                        row.setOrientation(LinearLayout.HORIZONTAL);
                        row.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

                        grid.addView(row);
                    }

                    int itemResource = item.IsToggle ?
                            (mViewAsGrid ? R.layout.toggleset_grid : R.layout.toggleset2) :
                            (mViewAsGrid ? R.layout.logset_grid : R.layout.logset);

                    View stub = View.inflate(this, itemResource, null);
                    stub.setId(1000 + cellID);
                    cellID++;

                    if(item.IsToggle)
                    {
                        mToggleButtonMap.put(item.Name, (ToggleButton) stub.findViewById(R.id.toggleButton));
                        mToggleButtonMap.get(item.Name).setOnClickListener(new ToggleListener(item.Name));
                        mToggleButtonMap.get(item.Name).setTextOff(item.Name);
                        mToggleButtonMap.get(item.Name).setTextOn(item.Name);

                        mToggleLabelMap.put(item.Name, (TextView) stub.findViewById(R.id.toggleElapsed));
                        mToggleLabelMap.get(item.Name).setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                mStatsSelector++;
                            }
                        });

                        if(viewAsGrid)
                        {
                            int buttonHeight = mToggleButtonMap.get(item.Name).getLayoutParams().height;
                            mToggleButtonMap.get(item.Name).setLayoutParams(new LinearLayout.LayoutParams(buttonWidth, buttonHeight));

                            int labelHeight = mToggleLabelMap.get(item.Name).getLayoutParams().height;
                            mToggleLabelMap.get(item.Name).setLayoutParams(new LinearLayout.LayoutParams(buttonWidth, labelHeight));
                        }
                    }
                    else
                    {
                        mEventButtonMap.put(item.Name, (Button) stub.findViewById(R.id.addButton));
                        mEventButtonMap.get(item.Name).setText(item.Name);

                        mEventButtonMap.get(item.Name).setOnClickListener(new AddListener(item.Name, item.IsValue));

                        mEventLabelMap.put(item.Name, (TextView) stub.findViewById(R.id.summaryText));
                        mEventLabelMap.get(item.Name).setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                mStatsSelector++;
                            }
                        });

                        if(viewAsGrid)
                        {
                            int buttonHeight = mEventButtonMap.get(item.Name).getLayoutParams().height;
                            mEventButtonMap.get(item.Name).setLayoutParams(new LinearLayout.LayoutParams(buttonWidth, buttonHeight));

                            int labelHeight = mEventLabelMap.get(item.Name).getLayoutParams().height;
                            mEventLabelMap.get(item.Name).setLayoutParams(new LinearLayout.LayoutParams(buttonWidth, labelHeight));
                        }
                    }

                    if(mViewAsGrid)
                    {
                        row.addView(stub);
                    }
                    else
                    {
                        listLayout.addView(stub);
                    }

                    insertIndex++;
                }
            }
            catch(NullPointerException e)
            {
                Log.e("Logger", "Null exception while loading log items");
            }

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

    private void ConfigureGPS()
    {
        if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)
        {
            FusedLocationProviderClient locationClient = LocationServices.getFusedLocationProviderClient(this);
            locationClient.getLastLocation()
                    .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location)
                        {
                            // Got last known location. In some rare situations this can be null.
                            if (location != null)
                            {
                                Debug("Logger", "Got GPS", false);
                                mLocation = location;

                                Toast t = Toast.makeText(getApplicationContext(), "Got GPS", Toast.LENGTH_SHORT);
                                t.show();
                            }
                        }
                    });
        }
        else
        {
            Toast t = Toast.makeText(this, "No GPS permission", Toast.LENGTH_SHORT);
            t.show();
        }
    }

    private void LogPhoneCalls()
    {
        //Determine the last time we checked for calls
        //if(mState.LastPhoneCall == null)
        //{
        //    mState.LastPhoneCall = Calendar.getInstance();

            //TODO: Load phone log and find most recent entry

        //    mState.Save();
        //}

        try
        {
            Cursor c = this.getContentResolver().query(CallLog.Calls.CONTENT_URI, null, null, null, CallLog.Calls.DATE + " ASC");

            int numberIndex = c.getColumnIndex(CallLog.Calls.NUMBER);
            int typeIndex = c.getColumnIndex(CallLog.Calls.TYPE);
            int dateIndex = c.getColumnIndex(CallLog.Calls.DATE);
            int nameIndex = c.getColumnIndex(CallLog.Calls.CACHED_NAME);
            int durationIndex = c.getColumnIndex(CallLog.Calls.DURATION);

            Calendar curDate = Calendar.getInstance();
            int newEntries = 0;
            while(c.moveToNext())
            {
                long dateInSeconds = Long.parseLong(c.getString(dateIndex));

                curDate.setTimeInMillis(dateInSeconds);

                if(mState.LastPhoneCall == null || mState.LastPhoneCall.getTimeInMillis()-curDate.getTimeInMillis() < -1000)
                {
                    String num = c.getString(numberIndex);
                    String name = c.getString(nameIndex);
                    String duration = c.getString(durationIndex);
                    int type = Integer.parseInt(c.getString(typeIndex));
                    String oldDate = mState.LastPhoneCall == null ? "" : String.format(Locale.getDefault(), " (%d, %s)", mState.LastPhoneCall.getTimeInMillis()-curDate.getTimeInMillis(), DateStrings.GetDateTimeString(mState.LastPhoneCall));
                    Debug("Logger", String.format(Locale.getDefault(), "Writing entry for date %s%s", DateStrings.GetDateTimeString(curDate),oldDate), false);

                    mPhoneLog.AddLogEntry(curDate, type, num, name, duration);
                    newEntries++;
                }

                //Debug("Logger", String.format("%s: %s (%s)", date, num, duration), false);
            }
            c.close();

            mState.LastPhoneCall = curDate;
            mState.Save();

            String message = String.format(Locale.getDefault(), "Logged %d phone calls", newEntries);
            Debug("Logger", message, false);
            if(newEntries > 0)
            {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            }
        }
        catch(SecurityException ex)
        {
            Debug("Logger", "Phone permission denied", true);
            Error(ex);
        }
        catch(NullPointerException ex)
        {
            Debug("Logger", "Phone cursor error", false);
            Error(ex);
        }
    }

    private void LoadStateFile()
    {
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
        else if (mState.NumItems() != mConfig.Items.size())
        {
            Debug("Logger", "State does not match config, creating new", false);
            //Internal storage version
            //mState = LoggerState.Create(getPreferences(0), mLog.GetLogEntries(), mConfig, mStorageDirectory);
            //File version
            mState = LoggerStateFile.Create(stateFilePath, mLog.GetLogEntries(), mConfig);
        }
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

            mStorageDirectory = path;
            SaveAppStorage();

            Debug("Logger", "Storage directory set to " + path, false);

            InitApp(mViewAsGrid);
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

    private void UpdateToggle(LogItem item, boolean state)
    {
        mToggleButtonMap.get(item.Name).setChecked(state);

        //Need to clean this up, duplicated elsewhere in this file
        mToggleButtonMap.get(item.Name).setTextColor(state ? Color.CYAN : Color.WHITE);
    }

    //Called to update the displayed text
    private void UpdateSummaries()
    {
        if(!mHadUpdateError)
        {
            try
            {
                if(mStatsSelector > 4)
                    mStatsSelector = 0;

                Calendar now = Calendar.getInstance();
                mIntroText.setText(DateStrings.GetPresentableDateTimeString(now));

                for(LogItem item : mConfig.Items)
                {
                    LoggerStateItem stateItem = mState.GetEntryByName(item.Name);
                    String summaryText = GetLogItemLabel(item, now);

                    if(item.IsToggle)
                    {
                        UpdateToggle(item, stateItem.ToggleState);

                        mToggleLabelMap.get(item.Name).setText(summaryText);
                    }
                    else
                    {
                        mEventLabelMap.get(item.Name).setText(summaryText);
                    }
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

    private String GetLogItemLabel(LogItem item, Calendar time)
    {
        LoggerStateItem stateItem = mState.GetEntryByName(item.Name);
        String summaryText;
        if(item.IsToggle)
        {
            //For toggles, need to add current "on time" if the toggle is on
            double elapsedToday = stateItem.RecentCounts[0];
            if(stateItem.ToggleState)
            {
                Calendar onTime = stateItem.RecentHistory[0];
                if(DateStrings.SameDay(time, onTime, mConfig.MidnightHour))
                {
                    //Toggle turned on today
                    elapsedToday += (time.getTimeInMillis() - onTime.getTimeInMillis()) / (float)3600000;
                }
                else
                {
                    //Toggle was already on at start of day
                    elapsedToday += (time.getTimeInMillis() - DateStrings.GetStartOfActiveDay(time, mConfig.MidnightHour).getTimeInMillis()) / (float)3600000;
                }
            }
            summaryText = String.format(Locale.getDefault(), " %s today\n", DateStrings.GetElapsedTimeString((long)(elapsedToday * 3600000), 1));
        }
        else
        {
            summaryText = String.format(Locale.getDefault(), " %d today\n", (int)stateItem.RecentCounts[0]);
        }

        int selector = mStatsSelector % (mViewAsGrid ? 2 : 1000);

        switch(selector)
        {
            case 0:
            {
                if(mViewAsGrid)
                {
                    summaryText = DateStrings.GetElapsedTimeString(stateItem.RecentHistory[0], time, 2);
                }
                else
                {
                    summaryText = String.format(Locale.getDefault(), "%s     Last (1): %s, %s",
                            summaryText,
                            DateStrings.GetElapsedTimeString(stateItem.RecentHistory[0], time, 2),
                            DateStrings.GetPrintableDateTimeString(stateItem.RecentHistory[0], mConfig.MidnightHour));
                }

                break;
            }
            case 1:
            {
                if(mViewAsGrid)
                {
                    if(item.IsToggle)
                    {
                        summaryText = DateStrings.GetElapsedTimeString((long)(stateItem.RecentCounts[0] * 3600000), 2);
                    }
                    else
                    {
                        summaryText = String.format(Locale.getDefault(), " %d", (int)stateItem.RecentCounts[0]);
                    }
                }
                else
                {
                    summaryText = String.format(Locale.getDefault(), "%s     Last (2): %s, %s",
                            summaryText,
                            DateStrings.GetElapsedTimeString(stateItem.RecentHistory[1], time, 2),
                            DateStrings.GetPrintableDateTimeString(stateItem.RecentHistory[1], mConfig.MidnightHour));
                }

                break;
            }
            case 2:
            {
                summaryText = String.format(Locale.getDefault(), "%s     Last (3): %s, %s",
                        summaryText,
                        DateStrings.GetElapsedTimeString(stateItem.RecentHistory[2], time, 2),
                        DateStrings.GetPrintableDateTimeString(stateItem.RecentHistory[2], mConfig.MidnightHour));

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

                    if(item.IsToggle)
                    {
                        totalsString += stateItem.RecentCounts[j] > 0 ? String.format(Locale.getDefault(), "%.01f", stateItem.RecentCounts[j]) : "0";
                    }
                    else
                    {
                        totalsString += String.format(Locale.getDefault(), "%d", (int)stateItem.RecentCounts[j]);
                    }
                }

                summaryText = String.format(Locale.getDefault(), "%s     Week: %s",
                        summaryText,
                        totalsString);

                break;
            }
            case 4:
            {
                summaryText = String.format(Locale.getDefault(), "%s     All-time: %.02f/day",
                        summaryText,
                        stateItem.AllTimeAverage);

                break;
            }
            default:
            {
                summaryText = "ERROR";
                break;
            }
        }

        return summaryText;
    }

    //Shows the LogViewer Activity
    private void ShowViewer()
    {
        Debug("Logger", "Starting LogViewer", false);
        Intent i = new Intent(getApplicationContext(), LogViewer.class);

        //For testing the AndroidPlot_Demo
        //Intent i = new Intent(getApplicationContext(), AndroidPlot_Demo.class);

        i.putExtra("safe", mState.Safe);
        i.putExtra("directory", mStorageDirectory);
        i.putExtra("configfile", mConfigFile);
        i.putExtra("logfile", mLogFile);
        i.putExtra("phonelogfile", mPhoneLogFile);
        startActivity(i);
    }

    //Shows the Settings Activity
    private void ShowSettings()
    {
        Debug("Logger", "Starting Config Activity", false);
        Intent i = new Intent(getApplicationContext(), LoggerConfigActivity.class);
        i.putExtra("directory", mStorageDirectory);
        i.putExtra("configfile", mConfigFile);
        startActivity(i);
        //startActivityForResult(i, 0);
    }

    private void EnterSafeMode()
    {
        try
        {
            mState.Safe = true;
            mState.Save();

            InitApp(mViewAsGrid);
        }
        catch(Exception e)
        {
            Error(e);
        }
    }


    //Button Listeners
    private class ToggleListener implements View.OnClickListener
    {
        private String mName = null;

        private ToggleListener(String name)
        {
            mName = name;
        }
        public void onClick(View v)
        {
            try
            {
                Debug("Logger", "Logging toggle", false);
                LogItem item = mConfig.GetEntryByName(mName);

                Calendar gc = Calendar.getInstance();
                boolean checked =mToggleButtonMap.get(mName).isChecked();

                LoggerStateItem stateItem = mState.GetEntryByName(item.Name);
                stateItem.ToggleState = checked;
                mState.UpdateItem(stateItem, gc, mConfig.MidnightHour);
                String state = checked ? "on" : "off";

                Location location = mConfig.LogGPS || item.IsLocation ? mLocation : null;

                mLog.AddLogEntry(gc, item.Name, state, null, location);

                UpdateToggle(item, checked);

                Debug("Logger", "Added toggle entry", false);
                Toast t = Toast.makeText(getApplicationContext(), "Added toggle entry", Toast.LENGTH_SHORT);
                t.show();

                if((item.IsBackup || (item.IsBackupOn && checked) || (item.IsBackupOff && !checked)))
                {
                    backupLogFile();
                }

//                //OLD WAY
//                String toggleString = mConfig.Toggles.get(mIndex) + " " + (checked ? "on" : "off");
//                for(int i=0; i<mConfig.BackupTriggers.size(); i++)
//                {
//                    //Look for the backup trigger in the fully-built toggle string
//                    //i.e. so toggle triggers "Sleep" and "Sleep on" will both work
//                    if(toggleString.contains(mConfig.BackupTriggers.get(i)))
//                    {
//                        backupLogFile();
//                    }
//                }
            }
            catch(Exception e)
            {
                Error(e);
            }
        }
    }

    private class AddListener implements View.OnClickListener
    {
        private String mName = null;
        private boolean mIsValue = false;

        private AddListener(String name, boolean isValue)
        {
            mName = name;
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

                mAdder.PromptAndSave(mName, setDate, addComment);
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
        private String Name = null;
        private Calendar Date = null;
        private String Comment = null;
        private boolean mDate = false;
        private boolean mComment = false;

        private void PromptAndSave(String name, boolean customDate, boolean getComment)
        {
            Name = name;
            Date = Calendar.getInstance();
            mDate = customDate;
            mComment = getComment;
            if(mDate)
                showDialog(0);
            else
                GetCommentAndSave();
        }

        private void GetCommentAndSave()
        {
            if(mComment)
                showDialog(1);
            else
                SaveLog();
        }

        private void SaveLog()
        {
            try
            {
                LogItem item = mConfig.GetEntryByName(Name);
                LoggerStateItem stateItem = mState.GetEntryByName(item.Name);
                Location location = mConfig.LogGPS || item.IsLocation ? mLocation : null;
                mLog.AddLogEntry(Date, item.Name, null, Comment, location);
                Comment = null;
                mState.UpdateItem(stateItem, Date, mConfig.MidnightHour);
                UpdateSummaries();

                Toast t = Toast.makeText(getApplicationContext(), "Added log entry", Toast.LENGTH_SHORT);
                t.show();

                if(item.IsBackup)
                {
                    backupLogFile();
                }

//                for(int i=0; i<mConfig.BackupTriggers.size(); i++)
//                {
//                    if(mConfig.BackupTriggers.get(i).equals(mConfig.Buttons.get(Index)))
//                    {
//
//                    }
//                }
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
                mLog.EmailLog(this, mConfig, mStorageDirectory);
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
            mLog.ExportLogToDirectory(mStorageDirectory);
            return true;
        }
        if (id == R.id.mainmenu_directory) {
            Debug("Logger", "Changing directory", false);
            ShowStorageDirectoryPicker();
            return true;
        }
        if (id == R.id.mainmenu_email) {
            Debug("Logger", "Emailing log", false);
            mLog.EmailLog(this, mConfig, mStorageDirectory);
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
            catch (Exception e)
            {
                Log.e("LoggerActivity", "Error doing thread sleep");
            }
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
        if(mUpdateThread != null)
        {
            Debug("Logger", "Shutting down", false);
            mQuitThread = true;
            try
            {
                mUpdateThread.join();
            }
            catch(Exception e)
            {
                Log.e("LoggerActivity", "Error joining update thread");
            }
            mQuitThread = false;
            mUpdateThread = null;
        }
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
        FirebaseCrash.report(e);
        String entry = ErrorFile.WriteException(e, getApplicationContext());
        Debug("Error", entry, false);
    }
}
