package com.viggi.lib_logger;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
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

import com.example.dave.lib_logger.R;
import com.viggi.lib_datestring.DateStrings;
import com.viggi.lib_file.DebugFile;
import com.viggi.lib_file.ErrorFile;
import com.viggi.lib_file.LogEntry;
import com.viggi.lib_file.LogFile;
import com.viggi.lib_file.LoggerConfig;

import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

/**
 * Created by Dave on 2/22/2015.
 * This is the Activity for viewing graphs and info from the log
 */
public class LogViewer extends Activity implements Runnable
{
    //Graph view controls
    private Button mSettingsButton = null;
    private com.viggi.lib_graph.GraphView mGraph = null;
    private ScrollView mTextScroller = null;
    private TextView mTextView = null;

    //Settings view controls
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

    private CharSequence[] mGraphTypes = {"Daily Totals", "Daily Timing", "Distribution", "Weekly Histogram", "Hourly Histogram", "Intervals", "Values", "Stats", "Comments", "Recent History"};
    private CharSequence[] mGraphTypesNew = {"Totals", "Values", "Daily Timing", "Distribution", "Intervals", "Histogram", "Info"};
    private CharSequence[] mTimeScales = {"Daily", "Monthly", "Yearly"};
    private CharSequence[] mHistTimeScales = {"Hour of day", "Day of week", "Day of month", "Day of year", "Week of year", "Month of year"};
    private CharSequence[] mInfoTypes = {"Stats", "Comments", "Recent History"};
    private CharSequence[] mCategoryStrings = null;
    private boolean[] mCategoryTypes = null;
    private CharSequence[] mTimeOptions = { "Days", "Weeks", "Months", "Years" };
    private LoggerConfig mConfig = null;
    private LogFile mLog = null;

    private boolean mShowingSettings = false;

    private boolean mSafe = false;
    private String mRootDirectory = null;
    private String mLogFile = null;

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


    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        try
        {
            super.onCreate(savedInstanceState);

            Intent intent = getIntent();
            mSafe = intent.getBooleanExtra("safe", false);
            mRootDirectory = intent.getStringExtra("directory");
            String configPath = intent.getStringExtra("configfile");
            mLogFile = intent.getStringExtra("logfile");

            mDayFilters = new ArrayList<Boolean>();
            for(int i=0; i<7; i++)
            {
                mDayFilters.add(false);
            }

            Debug("LogViewer", "Loading activity", false);
            mContext = this;

            mConfig = LoggerConfig.FromFile(mRootDirectory + "/" + configPath, getApplicationContext());

            IdentifyDataTypesAndCategories();

            ShowSettings();
        }
        catch(Exception e)
        {
            Debug("LogViewer", "Error loading activity", false);
            ErrorFile.WriteException(e, this);
        }
    }

    private void ShowGraph()
    {
        try
        {
            setContentView(R.layout.logview);

            mShowingSettings = false;

            //Find GUI Views
            mSettingsButton = (Button) findViewById(R.id.settingsButton);
            mSettingsButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ShowSettings();
                }
            });

            mGraph = (com.viggi.lib_graph.GraphView) findViewById(R.id.graphView);
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

                    View view = getCurrentFocus();
                    if (view != null) {
                        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                    }

                    ShowGraph();
                }
            });

            //Setup the data types Spinner
            ArrayAdapter<CharSequence> dataAdapter = new ArrayAdapter<>(mContext, android.R.layout.simple_spinner_item, mCategoryStrings);
            dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            mDataSpinner.setAdapter(dataAdapter);

            //Setup the graph times Spinner
            ArrayAdapter<CharSequence> timeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, mTimeOptions);
            timeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            mTimeSpinner.setAdapter(timeAdapter);

            //Setup the graph types Spinner
            ArrayAdapter<CharSequence> typeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, mGraphTypesNew);
            typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
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
        for(int i=0; i<mConfig.Buttons.size(); i++)
        {
            String button = mConfig.Buttons.get(i);
            boolean isValue = mConfig.ButtonValues.get(i);
            if((!mSafe || !button.equals("Smoke")))// && typeCounts.get(button) > 0)
            {
                categories.add(button);
                categoryTypes.add(isValue);
            }
        }
        for(int i=0; i<mConfig.Toggles.size(); i++)
        {
            String button = mConfig.Toggles.get(i);
            //if(typeCounts.get(button) > 0)
            categories.add(button);
            categoryTypes.add(false);
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
        mCategoryStrings = new CharSequence[sortedCategories.size() + 1];
        mCategoryTypes = new boolean[sortedCategories.size() + 1];
        mCategoryStrings[0] = "All";
        mCategoryTypes[0] = false;
        for(int i=0; i<sortedCategories.size(); i++)
        {
            mCategoryStrings[i + 1] = sortedCategories.get(i);
            mCategoryTypes[i + 1] = sortedCategoryTypes.get(i);
        }
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
            ArrayAdapter<CharSequence> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, secondaryOptions);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
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
        String filename = String.format("%s%s_%s_%s.png", mConfig.ExportDirectory,
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
        String filename = mConfig.ExportDirectory + mCategoryStrings[dataIndex] + "_" + mGraphTypes[typeIndex] + ".png";
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
            String graphType = mGraphTypesNew[mGraphTypeIndex].toString();

            int dataIndex = mDataIndex;
            String dataType = mCategoryStrings[dataIndex].toString();

            String action = prepare ? "Preparing" : "Drawing";
            Debug("LogViewer", action + " graph: " + dataType + ", " + typeIndex, false);

            boolean showGraph = typeIndex != 6; //6 is Info

            if(!prepare)
            {
                Debug("LogViewer", "Drawing " + graphType + " graph", false);
                //Set the graph visible and text invisible
                mGraph.setVisibility(showGraph ? View.VISIBLE : View.GONE);
                mTextScroller.setVisibility(showGraph ? View.GONE : View.VISIBLE);

                if(showGraph)
                    mGraph.invalidate();
                else
                    mTextView.setText(mStatsText);
            }
            else
            {
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

                //{"Totals", "Values", "Daily Timing", "Distribution", "Intervals", "Histogram", "Info"};
                switch (typeIndex)
                {
                    //mTimeScales = {"Daily", "Monthly", "Yearly"};
                    case 0: //Totals
                        //TODO: mTimeScales = {"Daily", "Monthly", "Yearly"};
                        GraphHelpers.DrawDailyCountsGraph(mGraph, mLog, mConfig, settings);
                        break;
                    case 1: //Values
                        if (dataCategory)
                        {
                            GraphHelpers.DrawValuesGraph(mGraph, mLog, mConfig, settings);
                        }
                        else
                        {
                            GraphHelpers.DrawDailyCountsGraph(mGraph, mLog, mConfig, settings);
                        }
                        break;
                    case 2: //Daily Timing
                        GraphHelpers.DrawDailyTimingGraph(mGraph, mLog, mConfig, settings);
                        break;
                    case 3: //Distribution
                        //TODO: mTimeScales = {"Daily", "Monthly", "Yearly"};
                        GraphHelpers.DrawDistribution(mGraph, mLog, mConfig, settings);
                        break;
                    case 4: //Intervals
                        GraphHelpers.DrawIntervalsGraph(mGraph, mLog, mConfig, settings);
                        break;
                    case 5: //Histogram
                        //mHistTimeScales = {"Hour of day", "Day of week", "Day of month", "Day of year",
                        //                   "Week of year", "Month of year"};

                        settings.timeScale = mHistTimeScales[mSecondaryIndex].toString();
                        GraphHelpers.DrawHistogramGraph(mGraph, mLog, mConfig, settings);
                        break;
                    case 6: //Info
                        //mInfoTypes = {"Stats", "Comments", "Recent History"};
                        String infoType = mInfoTypes[mSecondaryIndex].toString();
                        switch(infoType)
                        {
                            case "Stats":
                                mStatsText = GraphHelpers.DrawStats(mLog, mConfig, settings);
                                break;
                            case "Comments":
                                mStatsText = GraphHelpers.DrawComments(mLog, mConfig, settings);
                                break;
                            default:
                                mStatsText = GraphHelpers.DrawRecentHistory(mLog, mConfig, settings);
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
            if (units.equals("Days"))
            {
                numDays = timeLength;
            } else if (units.equals("Weeks"))
            {
                numDays = timeLength * 7;
            } else if (units.equals("Months"))
            {
                numDays = timeLength * 30;
            } else if (units.equals("Years"))
            {
                numDays = timeLength * 365;
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
