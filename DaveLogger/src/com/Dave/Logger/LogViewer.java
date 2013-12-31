package com.Dave.Logger;

import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import com.Dave.DateStrings.DateStrings;
import com.Dave.Graph.FloatRectangle;
import com.Dave.Graph.GraphPlot;
import com.Dave.Files.DebugFile;
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
	private Spinner mTypeSpinner = null;
	private Spinner mDataSpinner = null;
	private Spinner mTimeSpinner = null;
	private com.Dave.Graph.GraphView mGraph = null;
	private ScrollView mTextScroller = null;
	private TextView mTextView = null;
	private CharSequence[] mGraphTypes = {"Daily Totals", "Daily Timing", "Distribution", "Weekly Histogram", "Hourly Histogram", "Intervals", "Values", "Stats", "Comments", "Recent History"};
	private CharSequence[] mCategoryStrings = null;
	private boolean[] mCategoryTypes = null;
	private CharSequence[] mTimeOptions = { "All-time", "1 Year", "6 Months", "3 Months", "1 Month", "2 Weeks", "1 Week" };
	private LoggerConfig mConfig = null;
	private LogFile mLog = null;    
	
    private boolean mSafe = false;
    private String mRootDirectory = null;
    private String mLogFile = null;
    
    private Context mContext = null;
    private ProgressDialog mDialog = null;
    private String mStatsText = null;
    
    private boolean mInitialized = false;
    private boolean mThreadRunning = false;
	
	@Override
    public void onCreate(Bundle savedInstanceState)
    {
		try
		{
			super.onCreate(savedInstanceState);
			setContentView(R.layout.logview);
       
			Intent intent = getIntent();
			mSafe = intent.getBooleanExtra("safe", false);
			mRootDirectory = intent.getStringExtra("directory");
			String configFile = intent.getStringExtra("configfile");
			mLogFile = intent.getStringExtra("logfile");
			
			Debug("LogViewer", "Loading activity", false);
			mContext = this;
			
			//Find GUI Views
			mTypeSpinner = (Spinner) findViewById(R.id.graphSelector);
			mDataSpinner = (Spinner) findViewById(R.id.dataSelector);
			mTimeSpinner = (Spinner) findViewById(R.id.timeSelector);
			mGraph = (com.Dave.Graph.GraphView) findViewById(R.id.graphView);
			mTextScroller = (ScrollView) findViewById(R.id.scrollView);
			mTextView = (TextView) findViewById(R.id.textView);
			
			//Setup the graph types Spinner
			ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(this, android.R.layout.simple_spinner_item, mGraphTypes);
			adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			mTypeSpinner.setAdapter(adapter);
			
			//Setup the graph times Spinner
			adapter = new ArrayAdapter<CharSequence>(this, android.R.layout.simple_spinner_item, mTimeOptions);
			adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			mTimeSpinner.setAdapter(adapter);
			mTimeSpinner.setSelection(6); //Default to this 1-week history

			//Get the graph categories from the LoggerConfig
			List<String> categories = new ArrayList<String>();
			List<Boolean> categoryTypes = new ArrayList<Boolean>();
   			mConfig = LoggerConfig.FromFile(mRootDirectory + "/" + configFile, getApplicationContext());
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
   		
   			//Setup the graph categories Spinner
   			mCategoryStrings = new CharSequence[categories.size() + 1];
   			mCategoryTypes = new boolean[categories.size() + 1];
   			mCategoryStrings[0] = "All";
   			mCategoryTypes[0] = false;
   			for(int i=0; i<categories.size(); i++)
   			{
   				mCategoryStrings[i + 1] = categories.get(i);
   				mCategoryTypes[i + 1] = categoryTypes.get(i);
   			}
   			
			ArrayAdapter<CharSequence> adapter2 = new ArrayAdapter<CharSequence>(mContext, android.R.layout.simple_spinner_item, mCategoryStrings);
		   	adapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			mDataSpinner.setAdapter(adapter2);
   		
			//Prepare the Spinner listeners
   			mTypeSpinner.setOnItemSelectedListener(new myOnItemSelectedListener());
        	mDataSpinner.setOnItemSelectedListener(new myOnItemSelectedListener());
        	mTimeSpinner.setOnItemSelectedListener(new myOnItemSelectedListener());
        	
        	//Launch a wait dialog and a thread to load the data and draw the graph
        	LaunchThreadWithWaitDialog();
		}
		catch(Exception e)
		{
			Debug("LogViewer", "Error loading activity", false);
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
			Debug("LogViewer", "Tried to launch thread twice", false);
		}
	}
	
	@Override
	public void onBackPressed()
	{
		Debug("LogViewer", "Back button pressed", false);
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
	
	public void UpdateGraph(boolean prepare)
	{
		if(!mInitialized)
		{
			Debug("LogViewer", "UpdateGraph called before initialized", false);
			return;
		}
		try
		{
			int typeIndex = mTypeSpinner.getSelectedItemPosition();
			int dataIndex = mDataSpinner.getSelectedItemPosition();
			int timeIndex = mTimeSpinner.getSelectedItemPosition();
			String category = mCategoryStrings[dataIndex].toString();
			boolean categoryType = mCategoryTypes[dataIndex];
			String timeRange = mTimeOptions[timeIndex].toString();

			String action = prepare ? "Preparing" : "Drawing";
			Debug("LogViewer", action + " graph: " + category + ", " + typeIndex, false);
			
			if(prepare)
				mGraph.ClearGraph();
			
			switch(typeIndex)
			{
			case 0:
				DrawDailyCountsGraph(category, timeRange, prepare);
				break;
			case 1:
				DrawDailyTimingGraph(category, timeRange, prepare);
				break;
			case 2:
				DrawDistribution(category, prepare);
				break;
			case 3:
				DrawDailyHistogramGraph(category, prepare);
				break;
			case 4:
				DrawHourlyHistogramGraph(category, prepare);
				break;
			case 5:
				DrawIntervalsGraph(category, timeRange, prepare);
				break;
			case 6:
				if(categoryType)
				{
					DrawValuesGraph(category, timeRange, prepare);
				}
				else
				{
					DrawDailyCountsGraph(category, timeRange, prepare);
				}
				break;
			case 7:
				DrawStats(category, prepare);
				break;
			case 8:
				DrawComments(category, prepare);
				break;
			case 9:
				DrawRecentHistory(category, timeRange, prepare);
				break;
			default:
					break;
			}
			
			Debug("LogViewer", "Done " + action + " graph", false);
		}
		catch(Exception e)
		{
			Log.e("LogViewer", "Error: " + e.toString());
			ErrorFile.WriteException(e, null);
		}
	}
	
	private int GetDesiredLengthInDays(String timeRange)
	{
		//{ "All-time", "1 Year", "6 Months", "3 Months", "1 Month", "2 Weeks", "1 Week" }
		int desiredLength = 0;
		
		if(timeRange == "1 Year")
			desiredLength = 365;
		else if(timeRange == "6 Months")
			desiredLength = 180;
		else if(timeRange == "3 Months")
			desiredLength = 90;
		else if(timeRange == "1 Month")
			desiredLength = 30;
		else if(timeRange == "2 Weeks")
			desiredLength = 14;
		else if(timeRange == "1 Week")
			desiredLength = 7;
		
		return desiredLength;
	}
	
	private void DrawDailyCountsGraph(String category, String timeRange, boolean prepare)
	{
		if(!prepare)
		{
			Debug("LogViewer", "Drawing daily counts graph", false);
			//Set the graph visible and text invisible
			mGraph.setVisibility(View.VISIBLE);
			mTextScroller.setVisibility(View.GONE);
			
			mGraph.invalidate();
		}
		else
		{
			Debug("LogViewer", "Preparing daily counts graph", false);
			Calendar startDate = Calendar.getInstance();
	
			//Extract the data specified by "category"
			float[] data = mLog.ExtractDailyTotals(category, startDate, mConfig);
			boolean isToggle = mConfig.Toggles.indexOf(category) >= 0;
	
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
	
			//Filter to the desired time range
			int desiredLength = GetDesiredLengthInDays(timeRange);
			
			if(desiredLength > 0)
			{
				if(desiredLength > data.length)
					desiredLength = data.length;
				
				float[] tempData = new float[desiredLength];
				float[] tempAve = new float[desiredLength];
				float[] tempAllAve = new float[desiredLength];
				for(int i=0; i<desiredLength; i++)
				{
					int grabIndex = data.length - desiredLength + i;
					tempData[i] = data[grabIndex];
					tempAve[i] = ave[grabIndex];
					tempAllAve[i] = allAve[grabIndex];
				}
				data = tempData;
				ave = tempAve;
				allAve = tempAllAve;
				startDate.setTimeInMillis(Calendar.getInstance().getTimeInMillis() - ((long)(desiredLength - 1) * 24 * 3600 * 1000));
			}
			
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
			
			mGraph.RightAxis.GenerateLabels(mGraph.Plots);
			mGraph.LeftAxis.GenerateLabels(mGraph.Plots);
			
			//Setup the title
			mGraph.Title.Text = String.format("All-time: %.02f%s/day, currently: %.02f%s/day\nFirst entry: %s",
										average, units, ave[ave.length - 1], units, DateStrings.GetDateString(startDate));
			
			//Add the weekend shading and start-of-month indicators
			mGraph.AddDateInfo(startDate, true);
	
			//Turn off labels for the bottom axis since they are drawn with the date info
			//mGraph.BottomAxis.DrawLabels = false;
		}
	}

	private void DrawDailyTimingGraph(String category, String timeRange, boolean prepare)
	{
		if(!prepare)
		{
			Debug("LogViewer", "Drawing daily timing graph", false);
			//Set the graph visible and text invisible
			mGraph.setVisibility(View.VISIBLE);
			mTextScroller.setVisibility(View.GONE);
			
			mGraph.invalidate();
		}
		else
		{
			Debug("LogViewer", "Preparing daily timing graph", false);
	
			//Extract the data specified by "category"
			List<LogEntry> data = mLog.ExtractLog(category, mConfig);
			boolean isToggle = mConfig.Toggles.indexOf(category) >= 0;
	
			int historyDays = GetDesiredLengthInDays(timeRange);
			
			Calendar firstDate = Calendar.getInstance();
			if(historyDays > 0)
			{
				if(historyDays > data.size())
					historyDays = data.size();
				
				Calendar filterDate = Calendar.getInstance();
				filterDate.add(Calendar.HOUR, (historyDays - 1) * -24);
				filterDate.set(Calendar.HOUR, 0);
				filterDate.set(Calendar.MINUTE, 0);
				filterDate.set(Calendar.SECOND, 0);
				firstDate = filterDate;
				
				List<LogEntry> tempData = new ArrayList<LogEntry>();
				
				for(int i=0; i<data.size(); i++)
					if(data.get(i).GetDate().after(filterDate))
						tempData.add(data.get(i));
				
				if(isToggle && tempData.size() > 0 && tempData.get(0).ToggleState.equals("off"))
				{
					tempData.remove(0);
				}
				
				data = tempData;
			}
			else
			{
				if(data.size() > 0)
					firstDate = data.get(0).GetDate();
			}
			
			List<Calendar> dates = new ArrayList<Calendar>();
			List<Float> xValues = new ArrayList<Float>();
			List<Float> yValues = new ArrayList<Float>();
			if(data.size() > 0)
			{
				if(isToggle)
				{
					LogEntry lastEntry = data.get(data.size() - 1);
					if(lastEntry.ToggleState.equals("on"))
					{
						LogEntry newLast = new LogEntry(DateStrings.GetDateTimeString(Calendar.getInstance()), lastEntry.GetType(), "off", null);
						data.add(newLast);
					}
				}
				
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
					dates.add(date);
				}
			}
			float[] x = new float[xValues.size()];
			float[] y = new float[yValues.size()];
			for(int i = 0; i < xValues.size(); i++)
			{
				x[i] = xValues.get(i);
				y[i] = yValues.get(i);
			}
			
			mGraph.EasyScatterPlot(x, y, isToggle, new FloatRectangle(0, 24, (float)DateStrings.GetActiveDiffInDays(firstDate, Calendar.getInstance(), 0), 0));
			mGraph.Plots.get(0).PointColor = Color.YELLOW;
			
			//Setup the title
			//mGraph.Title.Text = String.format("All-time: %.02f%s/day, currently: %.02f%s/day\nFirst entry: %s",
			//							average, units, ave[ave.length - 1], units, DateStrings.GetDateString(startDate));
			
			//Add the weekend shading and start-of-month indicators
			mGraph.AddDateInfo(firstDate, Calendar.getInstance(), true);
	
			//Turn off labels for the bottom axis since they are drawn with the date info
			//mGraph.BottomAxis.DrawLabels = false;
		}
	}
	
	private void DrawDistribution(String category, boolean prepare)
	{
		if(!prepare)
		{
			Debug("LogViewer", "Drawing distribution graph", false);
			//Set the graph visible and text invisible
			mGraph.setVisibility(View.VISIBLE);
			mTextScroller.setVisibility(View.GONE);
			
			mGraph.invalidate();
		}
		else
		{
			Debug("LogViewer", "Preparing distribution graph", false);
			Calendar startDate = Calendar.getInstance();
	
			//Extract the data specified by "category"
			float[] data = mLog.ExtractDailyTotals(category, startDate, mConfig);
			boolean isToggle = mConfig.Toggles.indexOf(category) >= 0;
	
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
	
			mGraph.LeftAxis.GenerateLabels(0, ceiling, 1, false);
			mGraph.RightAxis.GenerateLabels(0, ceiling, 1, false);
			
			mGraph.BottomAxis.GenerateLabels(0, numBins - 1, axisMultiplier, false);
			mGraph.BottomAxis.DrawLabels = true;
		}
	}
	
	private void DrawDailyHistogramGraph(String category, boolean prepare)
	{
		if(!prepare)
		{
			Debug("LogViewer", "Drawing daily histogram graph", false);
			mGraph.setVisibility(View.VISIBLE);
			mTextScroller.setVisibility(View.GONE);
			
			mGraph.invalidate();
		}
		else
		{
			Debug("LogViewer", "Preparing daily histogram graph", false);
			//Extract the specified data
			Calendar startDate = Calendar.getInstance();
			float[] allData = mLog.ExtractDailyTotals(category, startDate, mConfig);
			boolean isToggle = mConfig.Toggles.indexOf(category) >= 0;
	
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
			float[] averages = mLog.GetDailyHistogram(allData, startDate);
			int[] x = new int[allData.length];
			for(int i=0; i<allData.length; i++)
			{
				x[i] = startDate.get(Calendar.DAY_OF_WEEK) % 7;				
				startDate.add(Calendar.HOUR, 24);
			}
			
			mGraph.EasyGraph(averages);
			mGraph.Plots.get(0).SetColor(Color.WHITE);
			mGraph.Plots.get(0).DrawPoints = false;
	
			for(int i=0; i<allData.length; i++)
			{
				mGraph.AddDataPoint(x[i], allData[i], Color.GREEN);
			}
			
			mGraph.RightAxis.GenerateLabels(0, Math.round(mGraph.GetDataRange().GetHeight()), 1, false);
			mGraph.LeftAxis.GenerateLabels(0, Math.round(mGraph.GetDataRange().GetHeight()), 1, false);
			
			mGraph.Title.Text = String.format("Histogram" + units);
			
			startDate = Calendar.getInstance();
			int day = startDate.get(Calendar.DAY_OF_WEEK) % 7;
			if(day > 0)
				startDate.add(Calendar.HOUR, -24 * day);				
			mGraph.AddDateInfo(startDate, false);
			
			String[] labels = {"S", "S", "M", "T", "W", "T", "F"};
			mGraph.BottomAxis.SetLabels(labels);
			mGraph.BottomAxis.DrawLabels = true;
		}
	}
	
	private void DrawHourlyHistogramGraph(String category, boolean prepare)
	{
		if(!prepare)
		{
			Debug("LogViewer", "Drawing hourly histogram graph", false);
			mGraph.setVisibility(View.VISIBLE);
			mTextScroller.setVisibility(View.GONE);
			
			mGraph.invalidate();
		}
		else
		{
			Debug("LogViewer", "Preparing hourly histogram graph", false);
			
			List<LogEntry> entries = mLog.ExtractLog(category, mConfig);
			boolean isToggle = mConfig.Toggles.indexOf(category) >= 0;
			
			float[] hist = mLog.GetHourlyHistogram(entries);
			float yMax = ArrayMath.GetCeiling(hist);
			
			mGraph.EasyGraph(hist, new FloatRectangle(0, yMax, 24, 0));
			mGraph.Plots.get(0).SetColor(Color.WHITE);
			mGraph.Plots.get(0).DrawPoints = false;
			
			mGraph.BottomAxis.GenerateLabels(0, 24, 1, false);
			mGraph.BottomAxis.DrawLabels = true;
			mGraph.Title.Text = "Hourly histogram";
			
			//Add a data point for every entry showing the exact time-of-day
			for(int i=0; i<entries.size(); i++)
			{
				float hour = entries.get(i).GetDate().get(Calendar.HOUR_OF_DAY);
				float minute = entries.get(i).GetDate().get(Calendar.MINUTE);
				float second = entries.get(i).GetDate().get(Calendar.SECOND);
				
				float x = hour + minute / 60 + second / 3600;
				
				int color = Color.GREEN;
				if(isToggle && entries.get(i).ToggleState.equals("off"))
					color = Color.RED;
				
				mGraph.AddDataPoint(x, yMax / 2, color);
			}
		}
	}
	
	private void DrawIntervalsGraph(String category, String timeRange, boolean prepare)
	{
		if(!prepare)
		{
			Debug("LogViewer", "Drawing intervals graph", false);
			mGraph.setVisibility(View.VISIBLE);
			mTextScroller.setVisibility(View.GONE);
			
			mGraph.invalidate();
		}
		else
		{
			Debug("LogViewer", "Preparing intervals graph", false);
			Calendar startDate = Calendar.getInstance();
	
			//Extract the specified data
			int multiplier = 1;
			int numEntries = 0;
			List<LogEntry> entries = mLog.ExtractLog(category, mConfig);
			boolean isToggle = mConfig.Toggles.indexOf(category) >= 0;
			if(isToggle)
			{
				if(entries.size() % 2 != 0)
					entries.add(new LogEntry(DateStrings.GetDateTimeString(Calendar.getInstance()),
							entries.get(entries.size() - 1).GetType(), "off", null));
				multiplier = 2;
				numEntries = entries.size() / 2;
			}
			else
			{
				entries.add(new LogEntry(DateStrings.GetDateTimeString(Calendar.getInstance()),
						entries.get(entries.size() - 1).GetType(), null, null));
				numEntries = entries.size();
			}
			
			int historyDays = GetDesiredLengthInDays(timeRange);
			
			if(historyDays > 0)
			{
				if(historyDays > entries.size())
					historyDays = entries.size();
				
				Calendar filterDate = Calendar.getInstance();
				filterDate.add(Calendar.HOUR, (historyDays - 1) * -24);
				filterDate.set(Calendar.HOUR, 0);
				filterDate.set(Calendar.MINUTE, 0);
				filterDate.set(Calendar.SECOND, 0);
				
				List<LogEntry> tempData = new ArrayList<LogEntry>();
				
				for(int i=0; i<entries.size(); i++)
					if(entries.get(i).GetDate().after(filterDate))
						tempData.add(entries.get(i));
				
				if(isToggle && tempData.get(0).ToggleState == "off")
					tempData.remove(0);
				
				entries = tempData;
				
				if(isToggle)
					numEntries = entries.size() / 2;
				else
					numEntries = entries.size();
			}
			
			//Calculate the intervals
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
	
	private void DrawValuesGraph(String category, String timeRange, boolean prepare)
	{
		if(!prepare)
		{
			Debug("LogViewer", "Drawing values graph", false);
			//Set the graph visible and text invisible
			mGraph.setVisibility(View.VISIBLE);
			mTextScroller.setVisibility(View.GONE);
			
			mGraph.invalidate();
		}
		else
		{
			Debug("LogViewer", "Preparing values graph", false);
	
			//Extract the data specified by "category"
			List<LogEntry> data = mLog.ExtractLog(category, mConfig);
			Calendar[] dates = new Calendar[data.size()];;
			float[] values = new float[data.size()];
			for(int i=0; i<data.size(); i++)
			{
				dates[i] = data.get(i).GetDate();
				try
				{
					values[i] = Float.parseFloat(data.get(i).GetComment());
				}
				catch(Exception e)
				{ }
			}
			float[] allAves = ArrayMath.GetAllTimeRunningAverageCurve(values);
			float[] runningAves = ArrayMath.GetRunningAverageCurve(values, 30);
			
			Calendar firstDate = Calendar.getInstance();
			int historyDays = GetDesiredLengthInDays(timeRange);
			if(historyDays > 0)
			{
				if(historyDays > data.size())
					historyDays = data.size();
				
				Calendar filterDate = Calendar.getInstance();
				filterDate.add(Calendar.HOUR, (historyDays - 1) * -24);
				filterDate.set(Calendar.HOUR, 0);
				filterDate.set(Calendar.MINUTE, 0);
				filterDate.set(Calendar.SECOND, 0);
				firstDate = filterDate;
				
				List<Calendar> dateSubset = new ArrayList<Calendar>();
				List<Float> subset = new ArrayList<Float>();
				List<Float> allAveSubset = new ArrayList<Float>();
				List<Float> runningAveSubset = new ArrayList<Float>();
				for(int i=0; i<data.size(); i++)
					if(data.get(i).GetDate().after(filterDate))
					{
						dateSubset.add(dates[i]);
						subset.add(values[i]);
						allAveSubset.add(allAves[i]);
						runningAveSubset.add(runningAves[i]);
					}
				
				int subsetSize = subset.size();
				dates = new Calendar[subsetSize];
				values = new float[subsetSize];
				allAves = new float[subsetSize];
				runningAves = new float[subsetSize];
				
				for(int i=0; i<subsetSize; i++)
				{
					dates[i] = dateSubset.get(i);
					values[i] = subset.get(i);
					allAves[i] = allAveSubset.get(i);
					runningAves[i] = runningAveSubset.get(i);
				}
			}
			else
			{
				if(dates.length > 0)
					firstDate = dates[0];
			}
			
			float[] x = new float[dates.length];
			for(int i=0; i<dates.length; i++)
			{
				x[i] = (float)DateStrings.GetActiveDiffInDays(firstDate, dates[i], 0);
			}
			
			float min = ArrayMath.GetMin(values);
			float max = ArrayMath.GetMax(values);
			mGraph.EasyLineGraph(x, values, new FloatRectangle(0, max, (float)DateStrings.GetActiveDiffInDays(firstDate, Calendar.getInstance(), 0), min));
			mGraph.Plots.get(0).PointColor = Color.YELLOW;
			mGraph.Plots.get(0).SetColor(Color.WHITE);
			
			//Setup the all-time average plot
			mGraph.Plots.add(new GraphPlot(x, allAves));
			mGraph.Plots.get(1).SetColor(Color.RED);
			mGraph.Plots.get(1).DrawPoints = false;
		        
			//Setup the running average plot
			mGraph.Plots.add(new GraphPlot(x, runningAves));
			mGraph.Plots.get(2).SetColor(Color.GREEN);
			mGraph.Plots.get(2).DrawPoints = false;
			
			//Setup the title
			mGraph.Title.Text = String.format("All-time: %.02f, currently: %.02f",
					allAves[allAves.length - 1], runningAves[runningAves.length - 1]);
			
			//Add the weekend shading and start-of-month indicators
			mGraph.AddDateInfo(firstDate, Calendar.getInstance(), true);
	
			//Turn off labels for the bottom axis since they are drawn with the date info
			//mGraph.BottomAxis.DrawLabels = false;
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

	private void DrawStats(String category, boolean prepare)
	{
		if(!prepare)
		{
			Debug("LogViewer", "Drawing stats", false);
			mGraph.setVisibility(View.GONE);
			mTextScroller.setVisibility(View.VISIBLE);
			
			mTextView.setText(mStatsText);
		}
		else
		{
			Debug("LogViewer", "Preparing stats", false);
			mStatsText = mLog.GetStats(category, mConfig);
			
		}
	}
	
	private void DrawComments(String category, boolean prepare)
    {
		if(!prepare)
        {
			Debug("LogViewer", "Drawing comments", false);
			mGraph.setVisibility(View.GONE);
            mTextScroller.setVisibility(View.VISIBLE);
                        
            mTextView.setText(mStatsText);
        }
        else
        {
			Debug("LogViewer", "Preparing comments", false);
			mStatsText = mLog.GetCommentSummary(category, mConfig);
        }
    }
	
	private void DrawRecentHistory(String category, String timeRange, boolean prepare)
	{
		if(!prepare)
		{
			Debug("LogViewer", "Drawing recent history", false);
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
			Debug("LogViewer", "Preparing recent history", false);
			List<LogEntry> entries = mLog.ExtractLog(category, mConfig);
			
			int historyDays = GetDesiredLengthInDays(timeRange);
			
			if(historyDays > 0)
			{
				if(historyDays > entries.size())
					historyDays = entries.size();
				
				Calendar filterDate = Calendar.getInstance();
				filterDate.add(Calendar.HOUR, (historyDays - 1) * -24);
				filterDate.set(Calendar.HOUR, 0);
				filterDate.set(Calendar.MINUTE, 0);
				filterDate.set(Calendar.SECOND, 0);
				
				List<LogEntry> tempData = new ArrayList<LogEntry>();
				
				for(int i=0; i<entries.size(); i++)
					if(entries.get(i).GetDate().after(filterDate))
						tempData.add(entries.get(i));
				
				entries = tempData;
			}
			
			int numEntries = entries.size();
			int historyLength = 200;
			if(numEntries < historyLength)
				historyLength = numEntries;
			mStatsText = "";
			LogEntry lastLogEntry = null;
			for(int i=numEntries - historyLength; i<numEntries; i++)
			{
				LogEntry logEntry = entries.get(i);
				String lineEnd = "\n";
				if(lastLogEntry != null)
					lineEnd = String.format(" (%s)\n", DateStrings.GetElapsedTimeString(lastLogEntry.GetDate(), logEntry.GetDate(), 2));
				mStatsText += lineEnd + logEntry.GetEntryString();
				lastLogEntry = logEntry;
			}
			if(lastLogEntry != null)
				mStatsText += String.format(" (%s)\n", DateStrings.GetElapsedTimeString(lastLogEntry.GetDate(), Calendar.getInstance(), 2));
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
