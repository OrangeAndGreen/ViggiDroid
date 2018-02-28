package com.viggi.lib_logger;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.androidplot.xy.XYPlot;
import com.crashlytics.android.Crashlytics;
import com.example.dave.lib_logger.R;
import com.viggi.lib_datestring.DateStrings;
import com.viggi.lib_file.DebugFile;
import com.viggi.lib_file.ErrorFile;
import com.viggi.lib_file.LogFile;
import com.viggi.lib_file.LogItem;
import com.viggi.lib_file.LoggerConfig;
import com.viggi.lib_file.PhoneLogFile;

import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import io.fabric.sdk.android.Fabric;

/**
 * Created by Dave on 2/22/2015.
 * This is the Activity for viewing graphs and info from the log
 */
public class LogViewer extends Activity implements Runnable
{
    //Graph view controls
    private com.viggi.lib_graph.GraphView mGraph = null;
    private XYPlot mXYPlot = null;
    private ScrollView mTextScroller = null;
    private TextView mTextView = null;

    //Settings view controls
    private CheckBox mNewGraphInput = null;
    private Spinner mTypeSpinner = null;
    private Spinner mDataSpinner = null;
    private Spinner mTimeSpinner = null;
    private TextView mSecondaryLabel = null;
    private Spinner mSecondarySpinner = null;
    private TextView mAllTimeAverageLabel = null;
    private CheckBox mAlltimeAverageInput = null;
    private TextView mValueLabel = null;
    private CheckBox mValueInput = null;
    private TextView mDrawPointsLabel = null;
    private CheckBox mDrawPointsInput = null;
    private TextView mDrawLinesLabel = null;
    private CheckBox mDrawLinesInput = null;

    private ToggleButton mMondayInput = null;
    private ToggleButton mTuesdayInput = null;
    private ToggleButton mWednesdayInput = null;
    private ToggleButton mThursdayInput = null;
    private ToggleButton mFridayInput = null;
    private ToggleButton mSaturdayInput = null;
    private ToggleButton mSundayInput = null;
    private List<Boolean> mDayFilters = null;

    //private CharSequence[] mGraphTypes = {"Daily Totals", "Daily Timing", "Distribution", "Weekly Histogram", "Hourly Histogram", "Intervals", "Values", "Stats", "Comments", "Recent History"};
    private CharSequence[] mGraphTypes = {"Totals", "Values", "Daily Timing", "Distribution", "Intervals", "Histogram", "Info"};
    private CharSequence[] mTimeScales = {"Daily", "Monthly", "Yearly"};
    private CharSequence[] mHistTimeScales = {"Hour of day", "Day of week", "Day of month", "Day of year", "Week of year", "Month of year"};
    private CharSequence[] mInfoTypes = {"Stats", "Comments", "Recent History"};
    private CharSequence[] mCategoryStrings = null;
    private boolean[] mCategoryTypes = null;
    private CharSequence[] mTimeOptions = { "Days", "Weeks", "Months", "Years" };
    private LoggerConfig mConfig = null;
    private LogFile mLog = null;
    private PhoneLogFile mPhoneLog = null;

    private boolean mShowingSettings = false;

    private boolean mSafe = false;
    private String mRootDirectory = null;
    private String mLogFile = null;
    private String mPhoneLogFile = null;

    private Context mContext = null;
    private ProgressDialog mDialog = null;
    private String mStatsText = null;

    private boolean mInitialized = false;
    private boolean mThreadRunning = false;

    //Settings Holders
    private int mDataIndex = -1;
    private String mHistoryLength = null;
    private int mHistoryIndex = -1;
    private int mGraphTypeIndex = -1;
    private int mSecondaryIndex = -1;
    private boolean mAllTimeAverageChecked = false;
    private boolean mValueChecked = false;
    private boolean mDrawPointsChecked = false;
    private boolean mDrawLinesChecked = false;
    private String mFilter = null;
    private boolean mNewGraphChecked = false;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        try
        {
            super.onCreate(savedInstanceState);

            //Crashlytics
            Fabric.with(this, new Crashlytics());

            Intent intent = getIntent();
            mSafe = intent.getBooleanExtra("safe", false);
            mRootDirectory = intent.getStringExtra("directory");
            String configPath = intent.getStringExtra("configfile");
            mLogFile = intent.getStringExtra("logfile");
            mPhoneLogFile = intent.getStringExtra("phonelogfile");

            mDayFilters = new ArrayList<>();
            for(int i=0; i<7; i++)
            {
                mDayFilters.add(false);
            }

            Debug("LogViewer", "Loading activity", false);
            mContext = this;

            mConfig = LoggerConfig.FromFile(mRootDirectory + "/" + configPath);

            IdentifyDataTypesAndCategories();

            LoadAppStorage();

            ShowSettings();
        }
        catch(Exception e)
        {
            Debug("LogViewer", "Error loading activity", false);
            ErrorFile.WriteException(e, this);
        }
    }

    public void LoadAppStorage()
    {
        SharedPreferences prefs = getPreferences(0);
        mDataIndex = prefs.getInt("dataIndex", mDataIndex);
        mHistoryIndex = prefs.getInt("historyIndex", mHistoryIndex);
        mGraphTypeIndex = prefs.getInt("graphTypeIndex", mGraphTypeIndex);
        mSecondaryIndex = prefs.getInt("secondaryIndex", mSecondaryIndex);
        mAllTimeAverageChecked = prefs.getBoolean("aveChecked", mAllTimeAverageChecked);
        mValueChecked = prefs.getBoolean("valueChecked", mValueChecked);
        mDrawPointsChecked = prefs.getBoolean("pointsChecked", mDrawPointsChecked);
        mDrawLinesChecked = prefs.getBoolean("linesChecked", mDrawLinesChecked);
        mNewGraphChecked = prefs.getBoolean("newGraph", mNewGraphChecked);
        mFilter = prefs.getString("filter", mFilter);
        mHistoryLength = prefs.getString("historyLength", mHistoryLength);
        mDayFilters.set(0, prefs.getBoolean("day0Checked", mDayFilters.get(0)));
        mDayFilters.set(1, prefs.getBoolean("day1Checked", mDayFilters.get(1)));
        mDayFilters.set(2, prefs.getBoolean("day2Checked", mDayFilters.get(2)));
        mDayFilters.set(3, prefs.getBoolean("day3Checked", mDayFilters.get(3)));
        mDayFilters.set(4, prefs.getBoolean("day4Checked", mDayFilters.get(4)));
        mDayFilters.set(5, prefs.getBoolean("day5Checked", mDayFilters.get(5)));
        mDayFilters.set(6, prefs.getBoolean("day6Checked", mDayFilters.get(6)));
    }

    public void SaveAppStorage()
    {
        SharedPreferences settings = getPreferences(0);
        SharedPreferences.Editor editor = settings.edit();

        editor.putInt("dataIndex", mDataIndex);
        editor.putInt("historyIndex", mHistoryIndex);
        editor.putInt("graphTypeIndex", mGraphTypeIndex);
        editor.putInt("secondaryIndex", mSecondaryIndex);
        editor.putBoolean("aveChecked", mAllTimeAverageChecked);
        editor.putBoolean("valueChecked", mValueChecked);
        editor.putBoolean("pointsChecked", mDrawPointsChecked);
        editor.putBoolean("linesChecked", mDrawLinesChecked);
        editor.putBoolean("newGraph", mNewGraphChecked);
        editor.putString("filter", mFilter);
        editor.putString("historyLength", mHistoryLength);
        editor.putBoolean("day0Checked", mDayFilters.get(0));
        editor.putBoolean("day1Checked", mDayFilters.get(1));
        editor.putBoolean("day2Checked", mDayFilters.get(2));
        editor.putBoolean("day3Checked", mDayFilters.get(3));
        editor.putBoolean("day4Checked", mDayFilters.get(4));
        editor.putBoolean("day5Checked", mDayFilters.get(5));
        editor.putBoolean("day6Checked", mDayFilters.get(6));


        editor.apply();
    }

    private void ShowGraph()
    {
        try
        {
            setContentView(R.layout.logview);

            mShowingSettings = false;

            //Find GUI Views
            Button settingsButton = (Button) findViewById(R.id.settingsButton);
            settingsButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ShowSettings();
                }
            });

            mGraph = (com.viggi.lib_graph.GraphView) findViewById(R.id.graphView);
            mXYPlot= (XYPlot) findViewById(R.id.APPlot);
            mTextScroller = (ScrollView) findViewById(R.id.scrollView);
            mTextView = (TextView) findViewById(R.id.textView);

            //Launch a wait dialog and a thread to load the data and draw the graph
            LaunchThreadWithWaitDialog();
        }
        catch(Exception e)
        {
            Debug("LogViewer", "Error loading graph view", false);
            ErrorFile.WriteException(e, this);
        }
    }

    private void ShowSettings()
    {
        try
        {
            setContentView(R.layout.logview_settings);

            mShowingSettings = true;

            //Find GUI Views
            final EditText filterText = (EditText) findViewById(R.id.filterText);
            Button doneButton = (Button) findViewById(R.id.doneButton);
            mDataSpinner = (Spinner) findViewById(R.id.dataSpinner);
            final EditText timeLengthText = (EditText) findViewById(R.id.timeLengthText);
            mTimeSpinner = (Spinner) findViewById(R.id.lengthSpinner);
            mTypeSpinner = (Spinner) findViewById(R.id.typeSpinner);
            mSecondaryLabel = (TextView) findViewById(R.id.secondarySelectorLabel);
            mSecondarySpinner = (Spinner) findViewById(R.id.secondarySpinner);
            mAllTimeAverageLabel = (TextView) findViewById(R.id.allHistoryLabel);
            mAlltimeAverageInput = (CheckBox) findViewById(R.id.allHistoryInput);
            mValueLabel = (TextView) findViewById(R.id.valueLabel);
            mValueInput = (CheckBox) findViewById(R.id.valueInput);
            mDrawPointsLabel = (TextView) findViewById(R.id.drawPointsLabel);
            mDrawPointsInput = (CheckBox) findViewById(R.id.drawPointsInput);
            mDrawLinesLabel = (TextView) findViewById(R.id.drawLinesLabel);
            mDrawLinesInput = (CheckBox) findViewById(R.id.drawLinesInput);
            mNewGraphInput = (CheckBox) findViewById(R.id.newGraphInput);

            mMondayInput = (ToggleButton) findViewById(R.id.mondayInput);
            mTuesdayInput = (ToggleButton) findViewById(R.id.tuesdayInput);
            mWednesdayInput = (ToggleButton) findViewById(R.id.wednesdayInput);
            mThursdayInput = (ToggleButton) findViewById(R.id.thursdayInput);
            mFridayInput = (ToggleButton) findViewById(R.id.fridayInput);
            mSaturdayInput = (ToggleButton) findViewById(R.id.saturdayInput);
            mSundayInput = (ToggleButton) findViewById(R.id.sundayInput);

            doneButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //Scrape the settings
                    mDataIndex = mDataSpinner.getSelectedItemPosition();
                    mHistoryIndex = mTimeSpinner.getSelectedItemPosition();
                    mGraphTypeIndex = mTypeSpinner.getSelectedItemPosition();
                    mSecondaryIndex = mSecondarySpinner.getSelectedItemPosition();
                    mAllTimeAverageChecked = mAlltimeAverageInput.isChecked();
                    mValueChecked = mValueInput.isChecked();
                    mDrawPointsChecked = mDrawPointsInput.isChecked();
                    mDrawLinesChecked = mDrawLinesInput.isChecked();
                    mNewGraphChecked = mNewGraphInput.isChecked();
                    mFilter = filterText.getText().toString().trim();
                    if(mFilter.length() == 0)
                    {
                        mFilter = null;
                    }
                    mHistoryLength = timeLengthText.getText().toString().trim();
                    if(mHistoryLength.length() == 0)
                    {
                        mHistoryLength = null;
                    }

                    mDayFilters.set(0, mSundayInput.isChecked());
                    mDayFilters.set(1, mMondayInput.isChecked());
                    mDayFilters.set(2, mTuesdayInput.isChecked());
                    mDayFilters.set(3, mWednesdayInput.isChecked());
                    mDayFilters.set(4, mThursdayInput.isChecked());
                    mDayFilters.set(5, mFridayInput.isChecked());
                    mDayFilters.set(6, mSaturdayInput.isChecked());

                    SaveAppStorage();

                    View view = getCurrentFocus();
                    if (view != null) {
                        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                    }

                    ShowGraph();
                }
            });

            //Setup the data types Spinner
            ArrayAdapter<CharSequence> dataAdapter = new ArrayAdapter<>(mContext, R.layout.spinner_item, mCategoryStrings);
            dataAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
            mDataSpinner.setAdapter(dataAdapter);

            //Setup the graph times Spinner
            ArrayAdapter<CharSequence> timeAdapter = new ArrayAdapter<>(this, R.layout.spinner_item, mTimeOptions);
            timeAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
            mTimeSpinner.setAdapter(timeAdapter);

            //Setup the graph types Spinner
            ArrayAdapter<CharSequence> typeAdapter = new ArrayAdapter<>(this, R.layout.spinner_item, mGraphTypes);
            typeAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
            mTypeSpinner.setAdapter(typeAdapter);

            //Prepare the Spinner listeners
            mTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    SetControlsForActiveType();
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });
            mDataSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    SetControlsForActiveType();
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });

            //mTimeSpinner.setOnItemSelectedListener(new myOnItemSelectedListener());

            mSundayInput.setChecked(mDayFilters.get(0));
            mMondayInput.setChecked(mDayFilters.get(1));
            mTuesdayInput.setChecked(mDayFilters.get(2));
            mWednesdayInput.setChecked(mDayFilters.get(3));
            mThursdayInput.setChecked(mDayFilters.get(4));
            mFridayInput.setChecked(mDayFilters.get(5));
            mSaturdayInput.setChecked(mDayFilters.get(6));

            if(mFilter == null)
            {
                mFilter = "";
            }
            filterText.setText(mFilter);

            if(mDataIndex >= 0)
            {
                mDataSpinner.setSelection(mDataIndex);
            }

            if(mHistoryLength == null)
            {
                mHistoryLength = "";
            }
            timeLengthText.setText(mHistoryLength);

            if(mHistoryIndex >= 0)
            {
                mTimeSpinner.setSelection(mHistoryIndex);
            }

            if(mGraphTypeIndex >= 0)
            {
                mTypeSpinner.setSelection(mGraphTypeIndex);
            }

            SetControlsForActiveType();



            mAlltimeAverageInput.setChecked(mAllTimeAverageChecked);
            mValueInput.setChecked(mValueChecked);
            mDrawPointsInput.setChecked(mDrawPointsChecked);
            mDrawLinesInput.setChecked(mDrawLinesChecked);
            mNewGraphInput.setChecked(mNewGraphChecked);

            //Debug code
            //mLog = new LogFile(mRootDirectory + "/" + mLogFile, true);
            //int catIndex = mConfig.Toggles.indexOf("Sleep");
            //List<LogEntry> subset = LogEntry.ExtractToggleLog(mLog.GetLogEntries(), catIndex, mConfig, "Home");
            //Debug("LogViewer", String.format("%d entries", subset.size()), true);
        }
        catch(Exception e)
        {
            Debug("LogViewer", "Error loading graph view", false);
            ErrorFile.WriteException(e, this);
        }
    }

    private void IdentifyDataTypesAndCategories()
    {
        //Get the graph categories from the LoggerConfig
        List<String> categories = new ArrayList<>();
        List<Boolean> categoryTypes = new ArrayList<>();
        for(LogItem item : mConfig.Items)
        {
            if(!mSafe || !item.IsSafe)
            {
                categories.add(item.Name);
                categoryTypes.add(item.IsValue);
            }
        }

        //Sort the categories in alhpabetical order
        List<String> sortedCategories = new ArrayList<>();
        for (String category : categories)
        {
            sortedCategories.add(category);
        }
        Collections.sort(sortedCategories);

        //Match the category types with the sorted categories
        List<Boolean> sortedCategoryTypes = new ArrayList<>();
        for (String category : sortedCategories)
        {
            int index = categories.indexOf(category);

            sortedCategoryTypes.add(categoryTypes.get(index));
        }

        //Setup the graph categories Spinner
        int numCategories = sortedCategories.size() + 2;
        mCategoryStrings = new CharSequence[numCategories];
        mCategoryTypes = new boolean[numCategories];
        mCategoryStrings[0] = "All";
        mCategoryTypes[0] = false;
        for(int i=0; i<sortedCategories.size(); i++)
        {
            mCategoryStrings[i + 1] = sortedCategories.get(i);
            mCategoryTypes[i + 1] = sortedCategoryTypes.get(i);
        }

        mCategoryStrings[numCategories-1] = "Phone calls";
        mCategoryTypes[numCategories-1] = false;
    }

    private void SetControlsForActiveType()
    {
        int index = mTypeSpinner.getSelectedItemPosition();

        CharSequence[] secondaryOptions = null;
        boolean showAllTimeAverage = false;
        boolean showUseValue = false;
        boolean showDrawPoints = false;
        boolean showDrawLines = false;

        switch(index)
        {
            case 0: //Totals
                secondaryOptions = mTimeScales;
                showAllTimeAverage = true;
                break;
            case 1: //Values
                showAllTimeAverage = true;
                break;
            case 2: //Daily Timing
                break;
            case 3: //Distribution
                secondaryOptions = mTimeScales;
                showUseValue = true;
                break;
            case 4: //Intervals
                break;
            case 5: //Histogram
                secondaryOptions = mHistTimeScales;
                showUseValue = true;
                break;
            case 6: //Info
                secondaryOptions = mInfoTypes;
                break;
            default:
                break;
        }

        //Setup the secondary Spinner
        mSecondaryLabel.setVisibility(secondaryOptions != null ? View.VISIBLE : View.INVISIBLE);
        mSecondarySpinner.setVisibility(secondaryOptions != null ? View.VISIBLE : View.INVISIBLE);
        if(secondaryOptions != null)
        {
            ArrayAdapter<CharSequence> adapter = new ArrayAdapter<>(this, R.layout.spinner_item, secondaryOptions);
            adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
            mSecondarySpinner.setAdapter(adapter);
        }

        if(mSecondaryIndex >= 0)
        {
            mSecondarySpinner.setSelection(mSecondaryIndex);
        }

        mAllTimeAverageLabel.setVisibility(showAllTimeAverage ? View.VISIBLE : View.INVISIBLE);
        mAlltimeAverageInput.setVisibility(showAllTimeAverage ? View.VISIBLE : View.INVISIBLE);
        mValueLabel.setVisibility(showUseValue ? View.VISIBLE : View.INVISIBLE);
        mValueInput.setVisibility(showUseValue ? View.VISIBLE : View.INVISIBLE);
        mDrawPointsLabel.setVisibility(showDrawPoints ? View.VISIBLE : View.INVISIBLE);
        mDrawPointsInput.setVisibility(showDrawPoints ? View.VISIBLE : View.INVISIBLE);
        mDrawLinesLabel.setVisibility(showDrawLines ? View.VISIBLE : View.INVISIBLE);
        mDrawLinesInput.setVisibility(showDrawLines ? View.VISIBLE : View.INVISIBLE);
    }

    private void LaunchThreadWithWaitDialog()
    {
        if(!mThreadRunning)
        {
            mDialog = ProgressDialog.show(this, "Loading", "Please wait...", true);
            new Thread(this).start();
        }
        else
        {
            Debug("LogViewer", "Tried to launch thread twice", false);
        }
    }

    @Override
    public void onBackPressed()
    {
        Debug("LogViewer", "Back button pressed", false);
        if(mShowingSettings)
        {
            finish();
        }
        else
        {
            ShowSettings();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.viewer_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        //Can't use a switch here because we're in a library
        //http://stackoverflow.com/questions/8476912/menu-item-ids-in-an-android-library-project

        int id = item.getItemId();

        // Handle item selection
        if(id == R.id.viewermenu_settings) {
            ShowSettings();
            return true;
        }
        if(id == R.id.viewermenu_export) {
            SaveBitmap();
            return true;
        }
        if(id == R.id.viewermenu_email) {
            EmailBitmap(this);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void SaveBitmap()
    {
        int typeIndex = mTypeSpinner.getSelectedItemPosition();
        int dataIndex = mDataSpinner.getSelectedItemPosition();
        Bitmap bitmap = mGraph.GetBitmap();
        String filename = String.format("%s%s_%s_%s.png", mRootDirectory,
                DateStrings.GetDateTimeString(Calendar.getInstance()),
                mCategoryStrings[dataIndex], mGraphTypes[typeIndex]);
        try
        {
            FileOutputStream out = new FileOutputStream(filename);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
        }
        catch(Exception e)
        {
            ErrorFile.WriteException(e, this);
        }
    }

    public void EmailBitmap(Context context)
    {
        int typeIndex = mTypeSpinner.getSelectedItemPosition();
        int dataIndex = mDataSpinner.getSelectedItemPosition();
        Bitmap bitmap = mGraph.GetBitmap();
        String filename = mRootDirectory + mCategoryStrings[dataIndex] + "_" + mGraphTypes[typeIndex] + ".png";
        try
        {
            FileOutputStream out = new FileOutputStream(filename);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
        }
        catch(Exception e)
        {
            ErrorFile.WriteException(e, this);
        }

        String body = "Image attached";

        final Intent emailIntent = new Intent(Intent.ACTION_SEND);
        emailIntent.setType("plain/text");
        emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{ mConfig.EmailAddress });
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, mConfig.EmailAutoSubject);
        emailIntent.putExtra(Intent.EXTRA_TEXT, body);
        emailIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://" + filename));

        context.startActivity(Intent.createChooser(emailIntent, "Send mail..."));
    }

    public void UpdateGraph(boolean prepare)
    {
        if(!mInitialized)
        {
            Debug("LogViewer", "UpdateGraph called before initialized", false);
            return;
        }
        try
        {
            int typeIndex = mGraphTypeIndex;
            String graphType = mGraphTypes[mGraphTypeIndex].toString();

            int dataIndex = mDataIndex;
            String dataType = mCategoryStrings[dataIndex].toString();

            String action = prepare ? "Preparing" : "Drawing";
            Debug("LogViewer", action + " graph: " + dataType + ", " + typeIndex, false);

            boolean showGraph = typeIndex != 6; //6 is Info

            if(!prepare)
            {
                Debug("LogViewer", "Drawing " + graphType + " graph", false);
                //Set the graph visible and text invisible
                mXYPlot.setVisibility(showGraph && mNewGraphChecked ? View.VISIBLE : View.GONE);
                mGraph.setVisibility(showGraph && !mNewGraphChecked ? View.VISIBLE : View.GONE);
                mTextScroller.setVisibility(showGraph ? View.GONE : View.VISIBLE);

                if(showGraph)
                    mGraph.invalidate();
                else
                    mTextView.setText(mStatsText);
            }
            else
            {
                LogItem item;
                if(dataType.equals("Phone calls"))
                {
                    item = new LogItem();
                    item.Name = "Phone calls";
                }
                else
                {
                    item = mConfig.GetEntryByName(dataType);
                }

                boolean dataCategory = mCategoryTypes[dataIndex];

                int timeLength = -1;
                if(mHistoryLength != null && mHistoryLength.length() > 0)
                {
                    timeLength = Integer.parseInt(mHistoryLength);
                }
                String timeRange = mTimeOptions[mHistoryIndex].toString();
                int numDays = calculateDays(timeLength, timeRange);

                List<Boolean> days = new ArrayList<>();
                days.add(mMondayInput.isChecked());
                days.add(mTuesdayInput.isChecked());
                days.add(mWednesdayInput.isChecked());
                days.add(mThursdayInput.isChecked());
                days.add(mFridayInput.isChecked());
                days.add(mSaturdayInput.isChecked());
                days.add(mSundayInput.isChecked());

                GraphSettings settings = new GraphSettings();
                settings.dataType = dataType;
                settings.numDays = numDays;
                settings.filter = mFilter;
                settings.dayFilters = days;

                mGraph.ClearGraph();

                XYPlot xyPlot = mNewGraphChecked ? mXYPlot : null;

                //{"Totals", "Values", "Daily Timing", "Distribution", "Intervals", "Histogram", "Info"};
                switch (typeIndex)
                {
                    case 0: //Totals
                        //TODO: mTimeScales = {"Daily", "Monthly", "Yearly"};
                        GraphHelpers.DrawDailyCountsGraph(this, mGraph, xyPlot, mLog, mPhoneLog, item, mConfig.MidnightHour, settings);
                        break;
                    case 1: //Values
                        if (dataCategory)
                        {
                            GraphHelpers.DrawValuesGraph(this, mGraph, xyPlot, mLog, mPhoneLog, mConfig, settings);
                        }
                        else
                        {
                            GraphHelpers.DrawDailyCountsGraph(this, mGraph, xyPlot, mLog, mPhoneLog, item, mConfig.MidnightHour, settings);
                        }
                        break;
                    case 2: //Daily Timing
                        GraphHelpers.DrawDailyTimingGraph(this, mGraph, xyPlot, mLog, mPhoneLog, mConfig, settings);
                        break;
                    case 3: //Distribution
                        //TODO: mTimeScales = {"Daily", "Monthly", "Yearly"};
                        GraphHelpers.DrawDistribution(this, mGraph, xyPlot, mLog, mPhoneLog, item, mConfig.MidnightHour, settings);
                        break;
                    case 4: //Intervals
                        GraphHelpers.DrawIntervalsGraph(this, mGraph, xyPlot, mLog, mPhoneLog, mConfig, settings);
                        break;
                    case 5: //Histogram
                        //mHistTimeScales = {"Hour of day", "Day of week", "Day of month", "Day of year",
                        //                   "Week of year", "Month of year"};

                        settings.timeScale = mHistTimeScales[mSecondaryIndex].toString();
                        GraphHelpers.DrawHistogramGraph(this, mGraph, xyPlot, mLog, mPhoneLog, item, mConfig.MidnightHour, settings);
                        break;
                    case 6: //Info
                        //mInfoTypes = {"Stats", "Comments", "Recent History"};
                        String infoType = mInfoTypes[mSecondaryIndex].toString();
                        switch(infoType)
                        {
                            case "Stats":
                                mStatsText = GraphHelpers.DrawStats(mLog, mPhoneLog, item, mConfig.MidnightHour, settings);
                                break;
                            case "Comments":
                                mStatsText = GraphHelpers.DrawComments(mLog, mPhoneLog, mConfig, settings);
                                break;
                            default:
                                mStatsText = GraphHelpers.DrawRecentHistory(mLog, mPhoneLog, mConfig, settings);
                                break;
                        }
                        break;
                    default:
                        break;
                }
            }

            Debug("LogViewer", "Done " + action + " graph", false);
        }
        catch(Exception e)
        {
            Log.e("LogViewer", "Error: " + e.toString());
            ErrorFile.WriteException(e, this);
        }
    }

    public int calculateDays(int timeLength, String units)
    {
        int numDays = -1;

        if(timeLength <= 0)
            return numDays;

        try
        {
            switch(units)
            {
                case "Days":
                    numDays = timeLength;
                    break;
                case "Weeks":
                    numDays = timeLength * 7;
                    break;
                case "Months":
                    numDays = timeLength * 30;
                    break;
                case "Years":
                    numDays = timeLength * 365;
                    break;
                default:
                    Log.e("LogViewer", "Unexpected units: " + units);
                    numDays = timeLength;
                    break;
            }
        }
        catch(Exception e)
        {
            ErrorFile.WriteException(e, this);
        }

        return numDays;
    }

    @Override
    public void run()
    {
        mThreadRunning = true;

        if(!mInitialized)
        {
            Debug("LogViewer", "Loading data", false);
            mLog = new LogFile(mRootDirectory + "/" + mLogFile, true);
            mPhoneLog = new PhoneLogFile(mRootDirectory + "/" + mPhoneLogFile, true);
            mInitialized = true;
        }

        Debug("LogViewer", "Preparing display", false);
        UpdateGraph(true);

        handler.sendEmptyMessage(0);
        mDialog.dismiss();

        mThreadRunning = false;
    }

    private Handler handler = new Handler()
    {
        @Override
        public void handleMessage(Message msg)
        {
            UpdateGraph(false);
        }
    };

    //Debug helper
    private void Debug(String tag, String message, boolean showToast)
    {
        Log.i(tag, message);
        if(mConfig != null && mConfig.Debug)
        {
            Context context = null;
            if(showToast)
                context = getApplicationContext();
            DebugFile.Write(mRootDirectory, tag, message, context);
        }
    }
}
