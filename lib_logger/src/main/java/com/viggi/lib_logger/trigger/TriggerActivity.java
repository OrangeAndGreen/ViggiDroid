package com.viggi.lib_logger.trigger;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.example.dave.lib_logger.R;
import com.viggi.lib_datestring.DateStrings;
import com.viggi.lib_file.DebugFile;
import com.viggi.lib_file.DirectoryPicker;
import com.viggi.lib_file.ErrorFile;
import com.viggi.lib_file.LogFile;
import com.viggi.lib_file.LogItem;
import com.viggi.lib_file.LoggerConfig;
import com.viggi.lib_file.LoggerStateFile;
import com.viggi.lib_file.LoggerStateItem;

import java.io.File;
import java.util.Calendar;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Created by Dave on 2/24/2015.
 */
public class TriggerActivity extends Activity
{
    private int mTriggerNumber = -1;
    private String mStorageDirectory = null;
    private final String mConfigFile = "Config.txt";
    private final String mLogFile = "Log.txt";
    private LoggerConfig mConfig = null;
    private final String mStateFile = "Temp.txt";

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        try
        {
            Intent intent = getIntent();
            mTriggerNumber = intent.getIntExtra("trigger", -1);

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

    private void InitApp()
    {
        String toastMessage;
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
            LogFile log = new LogFile(logPath, false);

            Debug("Logger", "Loading temp file", false);
            LoggerStateFile state;
            //Internal storage version
            //state = LoggerState.Load(getPreferences(0), mConfig);
            //File version
            String stateFilePath = mStorageDirectory + "/" + mStateFile;
            state = LoggerStateFile.FromFile(stateFilePath, mConfig);

            if(state == null)
            {
                Debug("Logger", "Temp file not found, creating new", false);

                //Internal storage version
                //state = LoggerState.Create(getPreferences(0), log.GetLogEntries(), mConfig, mStorageDirectory);
                //File version
                state = LoggerStateFile.Create(stateFilePath, log.GetLogEntries(), mConfig);
            }

            LogItem item = null;
            for(LogItem searchItem : mConfig.Items)
            {
                if(searchItem.TriggerID == mTriggerNumber)
                {
                    item = searchItem;
                    break;
                }
            }

            if(item != null)
            {
                String type;
                String logState = null;
                Calendar date = Calendar.getInstance();

                LoggerStateItem stateItem = state.GetEntryByName(item.Name);

                if(item.IsToggle)
                {
                    //Button
                    type = "button";
                    toastMessage = String.format("Logged %s", item.Name);
                }
                else
                {
                    //Toggle
                    type = "toggle";
                    stateItem.ToggleState = !stateItem.ToggleState;
                    logState = stateItem.ToggleState ? "on" : "off";
                    toastMessage = String.format("Logged %s %s", item.Name, state);
                }

                state.UpdateItem(stateItem, date, mConfig.MidnightHour);

                Debug("Logger", String.format("Logging %s", type), false);
                //TODO: Get location for entries configured to record it
                log.AddLogEntry(date, item.Name, logState, null, null);

            }
            else
            {
                toastMessage = String.format(Locale.getDefault(), "Trigger %d is not configured", mTriggerNumber);
            }
        }
        catch(Exception e)
        {
            toastMessage = "Error encountered during startup";
            Debug("Logger", toastMessage, true);
            Error(e);
        }

        Toast t = Toast.makeText(this, toastMessage, Toast.LENGTH_LONG);
        t.show();

        finish();
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
            Log.e("TriggerActivity", "Exception getting build date");
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
            editor.apply();

            mStorageDirectory = path;
            Debug("Logger", "Storage directory set to " + path, false);

            InitApp();
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
        String entry = ErrorFile.WriteException(e, getApplicationContext());
        Debug("Error", entry, false);
    }
}
