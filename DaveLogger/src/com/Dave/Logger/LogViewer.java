package com.Dave.Logger;

import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import com.Dave.DateStrings.DateStrings;
import com.Dave.Graph.GraphPlot;
import com.Dave.Files.ErrorFile;
import com.Dave.Files.LogEntry;
import com.Dave.Files.LogFile;
import com.Dave.Files.LoggerConfig;
import com.Dave.Math.ArrayMath;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

public class LogViewer extends Activity implements Runnable
{
	private static Spinner mTypeSpinner = null;
	private static Spinner mDataSpinner = null;
	private static com.Dave.Graph.GraphView mGraph = null;
	private static ScrollView mTextScroller = null;
	private static TextView mTextView = null;
	private static CharSequence[] mGraphTypes = {"Daily Totals", "Daily Timing", "Distribution", "Weekly Histogram", "Intervals", "Stats", "Recent History"};
	private static CharSequence[] mCategoryStrings = null;
	private static LoggerConfig mConfig = null;
	private static String mConfigFile = "/sdcard/00Logs/DaveConfig.txt";
	private static LogFile mLog = null;    
    private static String mLogFile = "/sdcard/00Logs/DaveLog.txt";
	
    private Context mContext = null;
    private ProgressDialog mDialog = null;
    private static String mStatsText = null;
    
    private static boolean mInitialized = false;
    private boolean mThreadRunning = false;
	
	@Override
    public void onCreate(Bundle savedInstanceState)
    {
		try
		{
			super.onCreate(savedInstanceState);
			setContentView(R.layout.logview);
       
			Log.i("LogViewer", "Loading activity");
			mContext = this;
			
			//Find GUI Views
			mTypeSpinner = (Spinner) findViewById(R.id.graphSelector);
			mDataSpinner = (Spinner) findViewById(R.id.dataSelector);
			mGraph = (com.Dave.Graph.GraphView) findViewById(R.id.graphView);
			mTextScroller = (ScrollView) findViewById(R.id.scrollView);
			mTextView = (TextView) findViewById(R.id.textView);
			
			//Setup the graph types Spinner
			ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(this, android.R.layout.simple_spinner_item, mGraphTypes);
			adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			mTypeSpinner.setAdapter(adapter);

			//Get the graph categories from the LoggerConfig
			List<String> categories = new ArrayList<String>();
   			mConfig = LoggerConfig.FromFile(mConfigFile);
   			for(int i=0; i<mConfig.Buttons.size(); i++)
   			{
   				
   				String button = mConfig.Buttons.get(i);
   				if((!DaveLogger.Safe || !button.equals("Smoke"))   )// && typeCounts.get(button) > 0)
   					categories.add(button);
   			}
   			for(int i=0; i<mConfig.Toggles.size(); i++)
   			{
   				String button = mConfig.Toggles.get(i);
   				//if(typeCounts.get(button) > 0)
   					categories.add(button);
   			}
   		
   			//Setup the graph categories Spinner
   			mCategoryStrings = new CharSequence[categories.size()];
   			for(int i=0; i<categories.size(); i++)
   				mCategoryStrings[i] = categories.get(i);
   			
			ArrayAdapter<CharSequence> adapter2 = new ArrayAdapter<CharSequence>(mContext, android.R.layout.simple_spinner_item, mCategoryStrings);
		   	adapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			mDataSpinner.setAdapter(adapter2);
   		
			//Prepare the Spinner listeners
   			mTypeSpinner.setOnItemSelectedListener(new myOnItemSelectedListener());
        	mDataSpinner.setOnItemSelectedListener(new myOnItemSelectedListener());
        	
        	//Launch a wait dialog and a thread to load the data and draw the graph
        	LaunchThreadWithWaitDialog();
		}
		catch(Exception e)
		{
			Log.e("LogViewer", "Error loading activity");
			ErrorFile.WriteException(e, this);
		}
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
			Log.w("LogViewer.LaunchThreadWithWaitDialog", "Tried to launch thread twice");
		}
	}
	
	@Override
	public void onBackPressed()
	{
		Log.i("LogViewer", "Back button pressed");
		finish();
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
        // Handle item selection
        switch (item.getItemId())
        {
        case R.id.viewermenu_export:
        	SaveBitmap();
            return true;
        case R.id.viewermenu_email:
        	EmailBitmap(this);
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
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
	
	public static void UpdateGraph(boolean prepare)
	{
		if(!mInitialized)
		{
			Log.i("LogViewer.UpdateGraph", "UpdateGraph called before initialized");
			return;
		}
		try
		{
			int typeIndex = mTypeSpinner.getSelectedItemPosition();
			int dataIndex = mDataSpinner.getSelectedItemPosition();
			String category = mCategoryStrings[dataIndex].toString();

			String action = prepare ? "Preparing" : "Drawing";
			Log.i("LogViewer.UpdateGraph", action + " graph: " + category + ", " + typeIndex);
			
			switch(typeIndex)
			{
			case 0:
				DrawDailyCountsGraph(category, prepare);
				break;
			case 1:
				DrawDailyTimingGraph(category, prepare);
				break;
			case 2:
				DrawDistribution(category, prepare);
				break;
			case 3:
				DrawDailyHistogramGraph(category, prepare);
				break;
			case 4:
				DrawIntervalsGraph(category, prepare);
				break;
			case 5:
				DrawStats(category, prepare);
				break;
			case 6:
				DrawRecentHistory(category, prepare);
				break;
			default:
					break;
			}
			
			Log.i("LogViewer.UpdateGraph", "Done " + action + " graph");
		}
		catch(Exception e)
		{
			ErrorFile.WriteException(e, null);
		}
	}
	
	private static void DrawDailyCountsGraph(String category, boolean prepare)
	{
		if(!prepare)
		{
			Log.i("LogViewer.DrawDailyCountsGraph", "Drawing daily counts graph");
			//Set the graph visible and text invisible
			mGraph.setVisibility(View.VISIBLE);
			mTextScroller.setVisibility(View.GONE);
			
			mGraph.invalidate();
		}
		else
		{
			Log.i("LogViewer.DrawDailyCountsGraph", "Preparing daily counts graph");
			Calendar startDate = Calendar.getInstance();
	
			//Extract the data specified by "category"
			float[] data = null;
			int catIndex = mConfig.Buttons.indexOf(category);
			boolean isToggle = false;
			if(catIndex < 0)
			{
				isToggle = true;
				catIndex = mConfig.Toggles.indexOf(category);
				data = mLog.ExtractDailyToggleTotals(catIndex, startDate, mConfig);
			}
			else
			{
				data = mLog.ExtractDailyEventTotals(catIndex, startDate, mConfig);
			}
	
			//If this is a toggle, possibly convert from hours to minutes
			String units = "";
			if(isToggle)
			{
				units = " hours";
				if(ArrayMath.GetMax(data) < 3)
				{
					units = " minutes";
					for(int i=0; i<data.length; i++)
						data[i] *= 60;
				}
			}
			
			//Calculate the average curves
			float[] ave = ArrayMath.GetRunningAverageCurve(data, 30);
			float[] allAve = ArrayMath.GetAllTimeRunningAverageCurve(data);
			float average = ArrayMath.GetAverage(data);
	
			mGraph.EasyGraph(data);
			
			//Setup the all-data plot
			mGraph.Plots.get(0).SetColor(Color.WHITE);
			mGraph.Plots.get(0).DrawPoints = true;
			mGraph.Plots.get(0).PointColor = Color.YELLOW;
	
			//Setup the all-time average plot
			mGraph.Plots.add(new GraphPlot(allAve, 1));		
			mGraph.Plots.get(1).SetColor(Color.RED);
			mGraph.Plots.get(1).DrawPoints = false;
	
			//Setup the running average plot
			mGraph.Plots.add(new GraphPlot(ave, 1));
			mGraph.Plots.get(2).SetColor(Color.GREEN);
			mGraph.Plots.get(2).DrawPoints = false;
			
			//Setup the title
			mGraph.Title.Text = String.format("All-time: %.02f%s/day, currently: %.02f%s/day\nFirst entry: %s",
										average, units, ave[ave.length - 1], units, DateStrings.GetDateString(startDate));
			
			//Add the weekend shading and start-of-month indicators
			mGraph.AddDateInfo(startDate);
	
			//Turn off labels for the bottom axis since they are drawn with the date info
			mGraph.BottomAxis.DrawLabels = false;
		}
	}

	private static void DrawDailyTimingGraph(String category, boolean prepare)
	{
		if(!prepare)
		{
			//Set the graph visible and text invisible
			mGraph.setVisibility(View.VISIBLE);
			mTextScroller.setVisibility(View.GONE);
			
			mGraph.invalidate();
		}
		else
		{
			Calendar startDate = Calendar.getInstance();
	
			//Extract the data specified by "category"
			List<LogEntry> data = null;
			int catIndex = mConfig.Buttons.indexOf(category);
			boolean isToggle = false;
			if(catIndex < 0)
			{
				isToggle = true;
				catIndex = mConfig.Toggles.indexOf(category);
				data = mLog.ExtractToggleLog(catIndex, mConfig);
			}
			else
			{
				data = mLog.ExtractEventLog(catIndex, mConfig);
			}
	
			Calendar firstDate = data.get(0).GetDate();
			
			List<Float> xValues = new ArrayList<Float>();
			List<Float> yValues = new ArrayList<Float>();
			LogEntry lastEntry = null;
			int lastX = 0;
			for(int i=0; i<data.size(); i++)
			{
				LogEntry curEntry = data.get(i);
				Calendar date = curEntry.GetDate();
				
				float hour = date.get(Calendar.HOUR_OF_DAY);
				float minute = date.get(Calendar.MINUTE);
				float second = date.get(Calendar.SECOND);
				
				if(isToggle)
				{
					int curX = DateStrings.GetActiveDiffInDays(firstDate, date, 0);
					
					if(lastEntry != null)
					{
						//Code for "off" entries
						for(int j=0; j<curX - lastX; j++)
						{
							xValues.add((float)(lastX + j));
							yValues.add((float)24);
							
							xValues.add((float)(lastX + j + 1));
							yValues.add((float)0);
						}
						
						lastEntry = null;
					}
					else
					{
						//Code for "on" entries
						lastEntry = curEntry;
					}
					
					lastX = curX;
				}
				
				xValues.add((float)DateStrings.GetActiveDiffInDays(firstDate, date, 0));
				yValues.add((float)hour + (minute / 60) + (second / 3600));
			}
			
			float[] x = new float[xValues.size()];
			float[] y = new float[yValues.size()];
			for(int i = 0; i < xValues.size(); i++)
			{
				x[i] = xValues.get(i);
				y[i] = yValues.get(i);
			}
			
			mGraph.EasyScatterPlot(x, y, isToggle);
			mGraph.Plots.get(0).PointColor = Color.YELLOW;
			
			//Setup the title
			//mGraph.Title.Text = String.format("All-time: %.02f%s/day, currently: %.02f%s/day\nFirst entry: %s",
			//							average, units, ave[ave.length - 1], units, DateStrings.GetDateString(startDate));
			
			//Add the weekend shading and start-of-month indicators
			mGraph.AddDateInfo(startDate);
	
			//Turn off labels for the bottom axis since they are drawn with the date info
			mGraph.BottomAxis.DrawLabels = false;
		}
	}
	
	private static void DrawDistribution(String category, boolean prepare)
	{
		if(!prepare)
		{
			//Set the graph visible and text invisible
			mGraph.setVisibility(View.VISIBLE);
			mTextScroller.setVisibility(View.GONE);
			
			mGraph.invalidate();
		}
		else
		{
			Calendar startDate = Calendar.getInstance();
	
			//Extract the data specified by "category"
			float[] data = null;
			boolean isToggle = false;
			int catIndex = mConfig.Buttons.indexOf(category);
			if(catIndex < 0)
			{
				isToggle = true;
				catIndex = mConfig.Toggles.indexOf(category);
				data = mLog.ExtractDailyToggleTotals(catIndex, startDate, mConfig);
			}
			else
			{
				data = mLog.ExtractDailyEventTotals(catIndex, startDate, mConfig);
			}
	
			//If this is a toggle, possibly convert from hours to minutes
			int binMultiplier = 1;
			String units = "";
			int axisMultiplier = 1;
			int dataMultiplier = 1;
			if(isToggle)
			{
				units = " hours";
				if(ArrayMath.GetMax(data) < 2)
				{
					units = " minutes";
					binMultiplier = 60;
					axisMultiplier = 60 / binMultiplier;
					dataMultiplier = 60;
					for(int i=0; i<data.length; i++)
						data[i] *= dataMultiplier;
				}
			}
			
			//Generate the distribution curve
			int numEntries = data.length;
			int numBins = (Math.round(ArrayMath.GetCeiling(data) / dataMultiplier) + 1) * binMultiplier + 1;
			float[] histogram = new float[numBins];
			for(int i=0; i<numEntries; i++)
			{
				
				int bin = Math.round(data[i] / dataMultiplier * binMultiplier);
				if(bin < 0)
					bin = 0;
				if(bin >= numBins)
					bin = numBins - 1;
				histogram[bin] += 1.f / numEntries * 100;
			}
	
			int gaussLength = 101;
			float average = ArrayMath.GetAverage(data);
			float stddev = ArrayMath.GetStandardDeviation(data);
			float[] gaussian = ArrayMath.GetGaussianEstimate(data, 0, (histogram.length - 1) * axisMultiplier, gaussLength);
			int ceiling = (int)ArrayMath.GetCeiling(histogram);
			
			mGraph.EasyGraph(histogram);
			mGraph.Plots.get(0).SetColor(Color.WHITE);
			mGraph.Plots.get(0).DrawPoints = true;
			mGraph.Plots.get(0).PointColor = Color.YELLOW;
	
			mGraph.Plots.add(new GraphPlot(gaussian, (float)1 / (gaussLength - 1)));
			mGraph.Plots.get(1).SetColor(Color.RED);
			mGraph.Plots.get(1).DrawPoints = false;
			
			mGraph.Title.Text = String.format("Average: %.02f%s/day\nStd. Dev.: %.02f%s/day", average, units, stddev, units);
	
			mGraph.LeftAxis.GenerateLabels(ceiling, 1, false);
			mGraph.RightAxis.GenerateLabels(ceiling, 1, false);
			
			mGraph.BottomAxis.GenerateLabels(numBins - 1, axisMultiplier, false);
			mGraph.BottomAxis.DrawLabels = true;
		}
	}
	
	private static void DrawDailyHistogramGraph(String category, boolean prepare)
	{
		if(!prepare)
		{
			mGraph.setVisibility(View.VISIBLE);
			mTextScroller.setVisibility(View.GONE);
			
			mGraph.invalidate();
		}
		else
		{
			//Extract the specified data
			float[] allData = null;
			int catIndex = mConfig.Buttons.indexOf(category);
			Calendar startDate = Calendar.getInstance();
			boolean isToggle = false;
			if(catIndex < 0)
			{
				isToggle = true;
				catIndex = mConfig.Toggles.indexOf(category);
				allData = mLog.ExtractDailyToggleTotals(catIndex, startDate, mConfig);
			}
			else
			{
				allData = mLog.ExtractDailyEventTotals(catIndex, startDate, mConfig);
			}
	
			String units = "";
			if(isToggle)
			{
				units = " (in hours)";
				if(ArrayMath.GetMax(allData) < 3)
				{
					units = " (in minutes)";
					for(int i=0; i<allData.length; i++)
						allData[i] *= 60;
				}
			}
			
			mGraph.EmptyGraph();
	
			//Calculate daily histograms
			//float max = ArrayMath.GetCeiling(allData);
			float[] averages = new float[7];
			int[] dayCounts = new int[7];
			int[] x = new int[allData.length];
			for(int i=0; i<allData.length; i++)
			{
				x[i] = startDate.get(Calendar.DAY_OF_WEEK) % 7;
				averages[x[i]] += allData[i];
				dayCounts[x[i]]++;
				
				startDate.add(Calendar.HOUR, 24);
			}
			
			for(int j=0; j<7; j++)
				averages[j] /= dayCounts[j];
			
			mGraph.EasyGraph(averages);
			mGraph.Plots.get(0).SetColor(Color.WHITE);
			mGraph.Plots.get(0).DrawPoints = false;
	
			for(int i=0; i<allData.length; i++)
			{
				mGraph.AddDataPoint(x[i], allData[i], Color.GREEN);
			}
			
			mGraph.RightAxis.GenerateLabels(Math.round(mGraph.GetDataRange().GetHeight()), 1, false);
			mGraph.LeftAxis.GenerateLabels(Math.round(mGraph.GetDataRange().GetHeight()), 1, false);
			
			String[] labels = {"S", "S", "M", "T", "W", "T", "F"};
			mGraph.BottomAxis.SetLabels(labels);
			mGraph.BottomAxis.DrawLabels = true;
			
			mGraph.Title.Text = String.format("Histogram" + units);
			
			startDate = Calendar.getInstance();
			int day = startDate.get(Calendar.DAY_OF_WEEK) % 7;
			if(day > 0)
				startDate.add(Calendar.HOUR, -24 * day);				
			mGraph.AddWeekendShading(startDate);
		}
	}
	
	private static void DrawIntervalsGraph(String category, boolean prepare)
	{
		if(!prepare)
		{
			mGraph.setVisibility(View.VISIBLE);
			mTextScroller.setVisibility(View.GONE);
			
			mGraph.invalidate();
		}
		else
		{
			Calendar startDate = Calendar.getInstance();
	
			//Extract the specified data
			int multiplier = 1;
			int numEntries = 0;
			List<LogEntry> entries = null;
			int catIndex = mConfig.Buttons.indexOf(category);
			boolean isToggle = false;
			if(catIndex < 0)
			{
				isToggle = true;
				catIndex = mConfig.Toggles.indexOf(category);
				entries = mLog.ExtractToggleLog(catIndex, mConfig);
				if(entries.size() % 2 != 0)
					entries.add(new LogEntry(DateStrings.GetDateTimeString(Calendar.getInstance()),
							entries.get(entries.size() - 1).GetType(), "off", null));
				multiplier = 2;
				numEntries = entries.size() / 2;
			}
			else
			{
				entries = mLog.ExtractEventLog(catIndex, mConfig);
				entries.add(new LogEntry(DateStrings.GetDateTimeString(Calendar.getInstance()),
						entries.get(entries.size() - 1).GetType(), null, null));
				numEntries = entries.size();
			}
			
			float[] intervals = new float[numEntries];
			Calendar lastDate = null;
			for(int i=0; i<numEntries*multiplier; i++)
			{
				Calendar curDate = entries.get(i).GetDate();
				if(i%multiplier != 0 || multiplier == 1 && i > 0)
					intervals[i/multiplier] = (curDate.getTimeInMillis() - lastDate.getTimeInMillis()) / (float) 3600000;
				lastDate = curDate;
			}
	
			String units = "hours";
			if(ArrayMath.GetMax(intervals) >= 96)
			{
				units = "days";
				for(int i=0; i<intervals.length; i++)
					intervals[i] /= 24;
			}
			else if(isToggle & ArrayMath.GetMax(intervals) < 3)
			{
				units = "minutes";
				for(int i=0; i<intervals.length; i++)
					intervals[i] *= 60;
			}
			
			float[] ave = ArrayMath.GetRunningAverageCurve(intervals, 30);
			float[] allAve = ArrayMath.GetAllTimeRunningAverageCurve(intervals);
			float average = ArrayMath.GetAverage(intervals);
	
			mGraph.EasyGraph(intervals);
			mGraph.Plots.get(0).SetColor(Color.WHITE);
			mGraph.Plots.get(0).DrawPoints = false;
	
			mGraph.Plots.add(new GraphPlot(allAve, 1));		
			mGraph.Plots.get(1).SetColor(Color.RED);
			mGraph.Plots.get(1).DrawPoints = false;
			
			mGraph.Plots.add(new GraphPlot(ave, 1));
			mGraph.Plots.get(2).SetColor(Color.GREEN);
			mGraph.Plots.get(2).DrawPoints = false;
	
			mGraph.Title.Text = String.format("All-time: %.02f %s, currently: %.02f %s\nFirst entry: %s",
										average, units, ave[ave.length - 1], units, DateStrings.GetDateString(startDate));
			//mGraph.AddDateInfo(startDate);
	
			mGraph.BottomAxis.DrawLabels = false;
		}
	}
	
	/*
	private void DrawIntervalsHistogram(String category)
	{
		mGraph.setVisibility(View.VISIBLE);
		mTextScroller.setVisibility(View.GONE);

		//Extract the specified data
		int multiplier = 1;
		int numEntries = 0;
		List<LogEntry> entries = null;
		int catIndex = ConfigFile.Buttons.indexOf(category);
		if(catIndex < 0)
		{
			catIndex = ConfigFile.Toggles.indexOf(category);
			entries = DaveLogger.Log.ExtractToggleLog(catIndex);
			if(entries.size() % 2 != 0)
				entries.add(new LogEntry(DateStrings.GetDateTimeString(Calendar.getInstance()),
						entries.get(entries.size() - 1).GetType(), "off", null));
			multiplier = 2;
			numEntries = entries.size() / 2;
		}
		else
		{
			entries = DaveLogger.Log.ExtractEventLog(catIndex);
			entries.add(new LogEntry(DateStrings.GetDateTimeString(Calendar.getInstance()),
					entries.get(entries.size() - 1).GetType(), null, null));
			numEntries = entries.size();
		}
		
		float[] intervals = new float[numEntries];
		Calendar lastDate = null;
		for(int i=0; i<numEntries*multiplier; i++)
		{
			Calendar curDate = entries.get(i).GetDate();
			if(i%multiplier != 0 || multiplier == 1 && i > 0)
				intervals[i/multiplier] = (curDate.getTimeInMillis() - lastDate.getTimeInMillis()) / (float) 3600000;
			lastDate = curDate;
		}

		if(ArrayMath.GetMax(intervals) >= 96)
		{
			for(int i=0; i<intervals.length; i++)
				intervals[i] /= 24;
		}
		
		int numIntervals = intervals.length;
		int numBins = Math.round(ArrayMath.GetMax(intervals));
		float[] histogram = new float[numBins];
		int[] x = new int[numIntervals];
		for(int i=0; i<intervals.length; i++)
		{
			int bin = Math.round(intervals[i]);
			if(bin < 0)
				bin = 0;
			if(bin >= numBins)
				bin = numBins - 1;
			x[i] = bin;
			histogram[bin] += 1 / numIntervals;
		}
		
		mGraph.EasyGraph(histogram);
		mGraph.Plots.get(0).SetColor(Color.WHITE);
		mGraph.Plots.get(0).DrawPoints = false;
		
		for(int i=0; i<intervals.length; i++)
		{
			mGraph.AddDataPoint(x[i], intervals[i], Color.GREEN);
		}

		mGraph.Title.Text = "";

		mGraph.BottomAxis.GenerateLabels(histogram.length);
		mGraph.BottomAxis.DrawLabels = true;

		mGraph.invalidate();
	}
	*/

	private static void DrawStats(String category, boolean prepare)
	{
		if(!prepare)
		{
			mGraph.setVisibility(View.GONE);
			mTextScroller.setVisibility(View.VISIBLE);
			
			mTextView.setText(mStatsText);
		}
		else
		{
			int catIndex = mConfig.Buttons.indexOf(category);
			if(catIndex < 0)
			{
				catIndex = mConfig.Toggles.indexOf(category);
				mStatsText = mLog.GetToggleStats(catIndex, mConfig);
			}
			else
			{
				mStatsText = mLog.GetEventStats(catIndex, mConfig);
			}
		}
	}
	
	private static void DrawRecentHistory(String category, boolean prepare)
	{
		if(!prepare)
		{
			mGraph.setVisibility(View.GONE);
			mTextScroller.setVisibility(View.VISIBLE);
			
			mTextView.setText(mStatsText);

			//Scroll the view to the bottom
			mTextScroller.post(new Runnable()
			{
		        @Override
		        public void run()
		        {
		        	mTextScroller.fullScroll(ScrollView.FOCUS_DOWN);
		        }
		    });
		}
		else
		{
			List<LogEntry> entries = null;
			int catIndex = mConfig.Buttons.indexOf(category);
			if(catIndex < 0)
			{
				catIndex = mConfig.Toggles.indexOf(category);
				entries = mLog.ExtractToggleLog(catIndex, mConfig);
			}
			else
			{
				entries = mLog.ExtractEventLog(catIndex, mConfig);
			}
			
			int numEntries = entries.size();
			int historyLength = 100;
			if(numEntries < historyLength)
				historyLength = numEntries;
			mStatsText = "";
			for(int i=numEntries - historyLength; i<numEntries; i++)
				mStatsText += entries.get(i).GetEntryString() + "\n";
		}
	}
	
    public class myOnItemSelectedListener implements OnItemSelectedListener
    {
        public myOnItemSelectedListener()
        {
        }

        public void onItemSelected(AdapterView<?> parent, View v, int pos, long row)
        {
        	//UpdateGraph();
        	LaunchThreadWithWaitDialog();
        	//pos = position in adapter
        	//row = position in View
        }

        public void onNothingSelected(AdapterView<?> parent)
        {
            // do nothing
        }
    }
    

	@Override
	public void run()
	{
		mThreadRunning = true;
		
		if(!mInitialized)
		{
			Log.i("LogViewer.run", "Loading data");
			mLog = new LogFile(mLogFile, true);
			mInitialized = true;
		}
		
		Log.i("LogViewer.run", "Preparing graph");
		UpdateGraph(true);
		
		handler.sendEmptyMessage(0);
    	mDialog.dismiss();
    	
    	mThreadRunning = false;
	}
	
	private static Handler handler = new Handler()
    {
        @Override
        public void handleMessage(Message msg)
        {
        	UpdateGraph(false);
        }
    };
}
