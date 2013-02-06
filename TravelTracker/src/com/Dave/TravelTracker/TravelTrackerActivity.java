package com.Dave.TravelTracker;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import com.Dave.DateStrings.DateStrings;
import com.Dave.Files.CoordFile;
import com.Dave.Files.ErrorFile;
import com.Dave.Files.GPSUnits;
import com.Dave.Graph.GraphPlot;
import com.Dave.Graph.GraphView;
import com.Dave.Math.ArrayMath;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.Projection;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;

import android.os.Bundle;
import android.os.Environment;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Toast;
import android.widget.ToggleButton;

/*
 * 
 * TravelTracker Functions:
 * -Log GPS coords and timestamps
 * -Display real-time stats about travel
 * -Speed, bearing, recent averages, movement, elapsed time
 * 		Think in terms of what you would want to see while driving
 * 
 * Next things to do:
 * 	-Add more graphs
 * 		-Current vector (bearing gives direction, speed gives magnitude, accuracy gives color)
 *  -Bundle new values
 *  	Graph history
 *  	Running Average Points
 *  -Map menu options
 *  	-Clear track overlays
 *  	-Set track color
 *  
 *  -Option to label files
 *  -Enable negative values for graphs
 *  -Create a better Record button
 *  -Menu option to export/email current file as GPX
 *  -Implement "time to location" option when a point on the map is clicked
 *  	Based on recent speed average and straight-line path
 *  -Display/export a summary of recent drives (start/end, travel time, efficiency, stops, etc.)
 *  -Save data to /tracks subdirectory
 *  -Better status: enabled/disabled, lock/searching, acquiring/streaming/stopped
 *  -More circle-axes for polar plots
 *  -Use a settings file to remember long-term settings
 *  
 *  -Big addition:
 *  	Remember user start and end points
 *  	First time, ask user for name of place (i.e. home, work, Sascha)
 *  	Then auto-identify start/end locations and include them somewhere in file or filename 
 *  
 */

public class TravelTrackerActivity extends MapActivity
{
	//Things to remember when saving/restoring state
	private int LogInterval = 1000;
	private GPSUnits mUnits = null;
	private int mGraphHistory = 5 * 60000;
	private int mRunningAveragePoints = 60;
	private float mStoppedThreshold = 1;
	private boolean mShowSatelliteView = true;
	private boolean mShowTraffic = false;
	private String mCurrentPage = null;
	private String mFilename = null;
	private boolean mRecording = false;
	private boolean mStopped = true;	
	
	public String LogDirectory = "01Tracks";
	private CoordFile mFile = null;
	private LocationManager mLocMgr = null;
	private TelephonyManager mTelephonyManager = null;
	private PhoneStateListener mSignalListener = null;
	private int mCurrentSignal = -1;
	private int mDefaultSignal = -10000;
	private MyLocationListener mListener = null;
	private MapController mMapController = null;
	private TrackOverlay mTrack = null;
	private StopOverlay mStops = null;
	private Context mContext = null;
	
	//GUI Components
	private TextView mStatusText = null;
	private Spinner mPageSelector = null;
	private ToggleButton mRecordButton = null;
	private TextView mMainText = null;
	private GraphView mGraph = null;
	private MapView mMapView = null;
	private LinearLayout mSettingsLayout = null;
	private Spinner mHistorySelector = null;
	private Spinner mAverageSelector = null;
	private Spinner mAltitudeSelector = null;
	private Spinner mAccuracySelector = null;
	private Spinner mDistanceSelector = null;
	private Spinner mSpeedDistanceSelector = null;
	private Spinner mSpeedTimeSelector = null;
	private CheckBox mShowSatelliteCheck = null;
	private CheckBox mShowTrafficCheck = null;
	
	//Strings
	private static String FILE_START = "TravelLog";
	private static String IDLE = "Idle";
	private static String INITIALIZING = "Initializing";
	private static String ACQUIRING = "Acquiring";
	private static CharSequence OVERVIEW = "Overview";
	private static CharSequence MAP = "Map";
	private static CharSequence SPEED_GRAPH = "Speed";
	private static CharSequence ACCELERATION_GRAPH = "Acceleration";
	private static CharSequence ALTITUDE_GRAPH = "Altitude";
	private static CharSequence BEARING_GRAPH = "Bearing";
	private static CharSequence ACCURACY_GRAPH = "Accuracy";
	private static CharSequence SIGNAL_GRAPH = "Signal";
	private static CharSequence SUMMARY = "Track Summary";
	private static CharSequence SETTINGS = "Settings";

	private static CharSequence[] mPages = { OVERVIEW, MAP, SPEED_GRAPH, ACCELERATION_GRAPH, ALTITUDE_GRAPH, ACCURACY_GRAPH, SIGNAL_GRAPH, BEARING_GRAPH, SUMMARY, SETTINGS };
	private static CharSequence[] mHistoryLengths = { "30s", "1m", "5m", "10m", "15m", "30m", "1h", "2h", "5h", "10h" };
	private static CharSequence[] mDistanceUnits = { GPSUnits.FEET, GPSUnits.METERS, GPSUnits.KILOMETERS, GPSUnits.MILES };
	private static CharSequence[] mTimeUnits = { GPSUnits.SECONDS, GPSUnits.MINUTES, GPSUnits.HOURS };
	
	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        
        mContext = this;
        
        mUnits = new GPSUnits(GPSUnits.IMPERIAL);
        
        if(savedInstanceState != null)
        {
        	mFilename = savedInstanceState.getString("filename");
        	mCurrentPage = savedInstanceState.getString("curpage");
        	mRecording = savedInstanceState.getBoolean("recording");

        	LogInterval = savedInstanceState.getInt("loginterval");
        	mGraphHistory = savedInstanceState.getInt("graphhistory");
        	mRunningAveragePoints = savedInstanceState.getInt("runningaverage");
        	mStoppedThreshold = savedInstanceState.getFloat("stoppedthreshold");
        	mShowSatelliteView = savedInstanceState.getBoolean("satelliteview");
        	mShowTraffic = savedInstanceState.getBoolean("traffic");
        	mStopped = savedInstanceState.getBoolean("stopped");
	    	
        	mUnits.UnitSystem = savedInstanceState.getString("");
        	mUnits.AltitudeUnits = savedInstanceState.getString("");
        	mUnits.SpeedDistanceUnits = savedInstanceState.getString("");
        	mUnits.SpeedTimeUnits = savedInstanceState.getString("");
        	mUnits.AccuracyUnits = savedInstanceState.getString("");
        	mUnits.DistanceUnits = savedInstanceState.getString("");
        	
        }
        else
        {
        	mFilename = null;
        	mCurrentPage = OVERVIEW.toString();
        	mRecording = false;
        }
        
        try
        {
	        setContentView(R.layout.main);
	        InitializeInterface();
	        
	        if(mRecording)
	        {
	        	mRecordButton.setChecked(mRecording);
	        	
	        	StartGPS();
	        }
	        else if(mFilename != null)
	        {
	        	mFile = new CoordFile(mContext, mFilename);
	        }
	        
	        LoadPage(null, 0);
        }
        catch(Exception e)
        {
        	ErrorFile.WriteException(e, this);
        }
    }
    
    private void InitializeInterface()
    {
    	try
    	{
	    	mPageSelector = (Spinner) findViewById(R.id.pageSelector);
	        mRecordButton = (ToggleButton) findViewById(R.id.recordButton);
	        mMainText = (TextView) findViewById(R.id.textView);
	        mGraph = (GraphView) findViewById(R.id.graphView);
	        mStatusText = (TextView) findViewById(R.id.statusText);
	        mSettingsLayout = (LinearLayout) findViewById(R.id.settings_layout);
	        mHistorySelector = (Spinner) findViewById(R.id.history_selector);
	        mAverageSelector = (Spinner) findViewById(R.id.average_selector);
	        mAltitudeSelector = (Spinner) findViewById(R.id.altitude_selector);
	        mAccuracySelector = (Spinner) findViewById(R.id.accuracy_selector);
	        mDistanceSelector = (Spinner) findViewById(R.id.distance_selector);
	        mSpeedDistanceSelector = (Spinner) findViewById(R.id.speed_distance_selector);
	        mSpeedTimeSelector = (Spinner) findViewById(R.id.speed_time_selector);
	        mMapView = (MapView) findViewById(R.id.mapview);
	        mShowSatelliteCheck = (CheckBox) findViewById(R.id.check_satellite);
	        mShowTrafficCheck = (CheckBox) findViewById(R.id.check_traffic);
	        
	        mStatusText.setText(IDLE);        
	        
	        mMapController = mMapView.getController();
	        mMapView.setBuiltInZoomControls(true);
	        mMapView.setSatellite(mShowSatelliteView);
	        mMapView.setTraffic(mShowTraffic);
	        
	        mRecordButton.setOnClickListener(new RecordListener());
	        
	        mShowSatelliteCheck.setChecked(mShowSatelliteView);
	        mShowSatelliteCheck.setOnCheckedChangeListener(new OnCheckedChangeListener()
	        {
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
				{
					try
					{
						mShowSatelliteView = isChecked;
						mMapView.setSatellite(mShowSatelliteView);
					}
			    	catch(Exception e)
			    	{
			    		ErrorFile.WriteException(e, getApplicationContext());
			    	}
				}
	        });
	        
	        mShowTrafficCheck.setChecked(mShowTraffic);
	        mShowTrafficCheck.setOnCheckedChangeListener(new OnCheckedChangeListener()
	        {
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
				{
					try
					{
						mShowTraffic = isChecked;
						mMapView.setTraffic(mShowTraffic);
					}
			    	catch(Exception e)
			    	{
			    		ErrorFile.WriteException(e, getApplicationContext());
			    	}
				}
	        });
	        
	        ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(this, android.R.layout.simple_spinner_item, mPages);
			adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			mPageSelector.setAdapter(adapter);
			mPageSelector.setOnItemSelectedListener(new OnItemSelectedListener()
			{
				@Override
				public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3)
				{
					try
					{
						mCurrentPage = mPages[mPageSelector.getSelectedItemPosition()].toString();
		        	
		        		LoadPage(null, 0);
					}
			    	catch(Exception e)
			    	{
			    		ErrorFile.WriteException(e, getApplicationContext());
			    	}
				}
	
				@Override
				public void onNothingSelected(AdapterView<?> arg0)
				{ }
			});
			
			adapter = new ArrayAdapter<CharSequence>(this, android.R.layout.simple_spinner_item, mHistoryLengths);
			adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			mHistorySelector.setAdapter(adapter);
			mHistorySelector.setSelection(2);
			mHistorySelector.setOnItemSelectedListener(new OnItemSelectedListener()
			{
				@Override
				public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3)
				{
					try
					{
						String value = mHistoryLengths[mHistorySelector.getSelectedItemPosition()].toString();
					
						mGraphHistory = GetGraphHistory(value);
					}
			    	catch(Exception e)
			    	{
			    		ErrorFile.WriteException(e, getApplicationContext());
			    	}
				}
	
				@Override
				public void onNothingSelected(AdapterView<?> arg0)
				{ }
			});
			
			adapter = new ArrayAdapter<CharSequence>(this, android.R.layout.simple_spinner_item, mHistoryLengths);
			adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			mAverageSelector.setAdapter(adapter);
			mAverageSelector.setSelection(6);
			mAverageSelector.setOnItemSelectedListener(new OnItemSelectedListener()
			{
				@Override
				public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3)
				{
					try
					{
						String value = mHistoryLengths[mAverageSelector.getSelectedItemPosition()].toString();
					
						mRunningAveragePoints = GetGraphHistory(value) / 1000;
					}
			    	catch(Exception e)
			    	{
			    		ErrorFile.WriteException(e, getApplicationContext());
			    	}
				}
	
				@Override
				public void onNothingSelected(AdapterView<?> arg0)
				{ }
			});
			
			adapter = new ArrayAdapter<CharSequence>(this, android.R.layout.simple_spinner_item, mDistanceUnits);
			adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			mAltitudeSelector.setAdapter(adapter);
			mAltitudeSelector.setSelection(0);
			mAltitudeSelector.setOnItemSelectedListener(new OnItemSelectedListener()
			{
				@Override
				public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3)
				{
					try
					{
						mUnits.AltitudeUnits = mDistanceUnits[mAltitudeSelector.getSelectedItemPosition()].toString();
					}
			    	catch(Exception e)
			    	{
			    		ErrorFile.WriteException(e, getApplicationContext());
			    	}
				}
	
				@Override
				public void onNothingSelected(AdapterView<?> arg0)
				{ }
			});
			
			adapter = new ArrayAdapter<CharSequence>(this, android.R.layout.simple_spinner_item, mDistanceUnits);
			adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			mAccuracySelector.setAdapter(adapter);
			mAccuracySelector.setSelection(0);
			mAccuracySelector.setOnItemSelectedListener(new OnItemSelectedListener()
			{
				@Override
				public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3)
				{
					try
					{
						mUnits.AccuracyUnits = mDistanceUnits[mAccuracySelector.getSelectedItemPosition()].toString();
					}
			    	catch(Exception e)
			    	{
			    		ErrorFile.WriteException(e, getApplicationContext());
			    	}
				}
	
				@Override
				public void onNothingSelected(AdapterView<?> arg0)
				{ }
			});
			
			adapter = new ArrayAdapter<CharSequence>(this, android.R.layout.simple_spinner_item, mDistanceUnits);
			adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			mDistanceSelector.setAdapter(adapter);
			mDistanceSelector.setSelection(3);
			mDistanceSelector.setOnItemSelectedListener(new OnItemSelectedListener()
			{
				@Override
				public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3)
				{
					try
					{
						mUnits.DistanceUnits = mDistanceUnits[mDistanceSelector.getSelectedItemPosition()].toString();
					}
			    	catch(Exception e)
			    	{
			    		ErrorFile.WriteException(e, getApplicationContext());
			    	}
				}
	
				@Override
				public void onNothingSelected(AdapterView<?> arg0)
				{ }
			});
			
			adapter = new ArrayAdapter<CharSequence>(this, android.R.layout.simple_spinner_item, mDistanceUnits);
			adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			mSpeedDistanceSelector.setAdapter(adapter);
			mSpeedDistanceSelector.setSelection(3);
			mSpeedDistanceSelector.setOnItemSelectedListener(new OnItemSelectedListener()
			{
				@Override
				public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3)
				{
					try
					{
						mUnits.SpeedDistanceUnits = mDistanceUnits[mSpeedDistanceSelector.getSelectedItemPosition()].toString();
					}
			    	catch(Exception e)
			    	{
			    		ErrorFile.WriteException(e, getApplicationContext());
			    	}
				}
	
				@Override
				public void onNothingSelected(AdapterView<?> arg0)
				{ }
			});
			
			adapter = new ArrayAdapter<CharSequence>(this, android.R.layout.simple_spinner_item, mTimeUnits);
			adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			mSpeedTimeSelector.setAdapter(adapter);
			mSpeedTimeSelector.setSelection(2);
			mSpeedTimeSelector.setOnItemSelectedListener(new OnItemSelectedListener()
			{
				@Override
				public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3)
				{
					try
					{
						mUnits.SpeedTimeUnits = mTimeUnits[mSpeedTimeSelector.getSelectedItemPosition()].toString();
					}
			    	catch(Exception e)
			    	{
			    		ErrorFile.WriteException(e, getApplicationContext());
			    	}
				}
	
				@Override
				public void onNothingSelected(AdapterView<?> arg0)
				{ }
			});
    	}
    	catch(Exception e)
    	{
    		ErrorFile.WriteException(e, getApplicationContext());
    	}
		
    }
    
    @Override
    protected void onSaveInstanceState (Bundle outState)
    {
    	try
    	{
	    	if(mFilename != null)
	    		outState.putString("filename", mFilename);
	    	if(mCurrentPage != null)
	    		outState.putString("curpage", mCurrentPage);
	    	outState.putInt("loginterval", LogInterval);
	    	outState.putInt("graphhistory", mGraphHistory);
	    	outState.putInt("runningaverage", mRunningAveragePoints);
	    	outState.putFloat("stoppedthreshold", mStoppedThreshold);
	    	outState.putBoolean("satelliteview", mShowSatelliteView);
	    	outState.putBoolean("traffic", mShowTraffic);
	    	outState.putBoolean("recording", mRecording);
	    	outState.putBoolean("stopped", mStopped);
	    	
	    	outState.putString("", mUnits.UnitSystem);
	    	outState.putString("", mUnits.AltitudeUnits);
	    	outState.putString("", mUnits.SpeedDistanceUnits);
	    	outState.putString("", mUnits.SpeedTimeUnits);
	    	outState.putString("", mUnits.AccuracyUnits);
	    	outState.putString("", mUnits.DistanceUnits);
	    }
    	catch(Exception e)
    	{
    		ErrorFile.WriteException(e, this);
    	}
    	super.onSaveInstanceState(outState);
    }
    
    //Main-menu handling
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
    	try
    	{
    		MenuInflater inflater = getMenuInflater();
    		inflater.inflate(R.menu.main_menu, menu);
    	}
    	catch(Exception e)
    	{
    		ErrorFile.WriteException(e, this);
    	}
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
    	try
    	{
	        // Handle item selection
	        switch (item.getItemId())
	        {
	        case R.id.mainmenu_load:
	        	//If recording, prompt to stop recording
	        	if(mRecording)
	        	{
	        		//Ask whether to continue previous log or start new?
	    			AlertDialog.Builder builder =  new AlertDialog.Builder(mContext);
	    	    	builder.setMessage("Stop current recording?");
	    	    	builder.setPositiveButton("Stop", new DialogInterface.OnClickListener()
	    	    	{
	    	            @Override
	    	            public void onClick(DialogInterface dialog, int which)
	    	            {
	    	            	try
	    	            	{
		    	            	StopGPS();
		    	            	
		    	            	mRecording = !mRecording;
		    	            	mRecordButton.setChecked(false);
		    	            	
		    	            	Toast t = Toast.makeText(mContext, "Stopped GPS", Toast.LENGTH_SHORT);
		    	        		t.show();
		    	        		
		    	        		LoadFile();
	    	            	}
	    	            	catch(Exception e)
	    	            	{
	    	            		ErrorFile.WriteException(e, getApplicationContext());
	    	            	}
	    	            }
	    	        });
	    	    	builder.setNegativeButton("Continue", new DialogInterface.OnClickListener()
	    	    	{
	    	            @Override
	    	            public void onClick(DialogInterface dialog, int which)
	    	            {
	    	            }
	    	        });
	    	    	builder.setOnCancelListener(new OnCancelListener()
	    	    	{
						@Override
						public void onCancel(DialogInterface dialog)
						{
						}
	    	    	});
	    	    	builder.show();
	        	}
	        	else
	        	{
	        		LoadFile();
	        	}
	            return true;
	        default:
	            return super.onOptionsItemSelected(item);
	        }
    	}
    	catch(Exception e)
    	{
    		ErrorFile.WriteException(e, getApplicationContext());
    	}
    	return false;
    }
    
    private void LoadFile()
    {
    	try
    	{
	    	//Find the available log files
	    	File files[] = Environment.getExternalStorageDirectory().listFiles();
	    	
	    	mTravelFiles = new ArrayList<File>();
	    	for(int i=0; i<files.length; i++)
	    		if(files[i].getName().startsWith(FILE_START))
	    			mTravelFiles.add(files[i]);
	    	
	    	String[] selections = new String[mTravelFiles.size()];
	    	for(int i=0; i<mTravelFiles.size(); i++)
	    	{
	    		selections[i] = mTravelFiles.get(i).getName();
	    	}
	    	
	    	//Prompt for which file to load
			AlertDialog.Builder builder =  new AlertDialog.Builder(mContext);
	    	builder.setTitle("Select file");
	    	builder.setItems(selections, new DialogInterface.OnClickListener()
	    	{
	    		public void onClick(DialogInterface dialog, int which)
	    	    {
	    			try
	    			{
		    			//Load the file
		    			File file = mTravelFiles.get(which);
		    			
		    			if(mFile != null && file.getAbsolutePath() != mFile.Filename)
		    			{
		    				CloseFile();
		    			}
		    			
		    			mFile = new CoordFile(mContext, file.getAbsolutePath());
		    			mFilename = mFile.Filename;
		    			
		    			List<Overlay> mapOverlays = mMapView.getOverlays();
		    			mTrack = new TrackOverlay();
		    			mStops = new StopOverlay();
		    			boolean stopped = true;
		    			for(int i=0; i<mFile.Size(); i++)
		    			{
		    				GeoPoint p = new GeoPoint(
		        					(int) (mFile.GetLatitude(i) * 1E6),
		        					(int) (mFile.GetLongitude(i) * 1E6));
		    				mTrack.Add(p);
		    				
		    				boolean stoppedNow = mFile.IsStopped(i - 1, i, mStoppedThreshold, mUnits);
		    				
		    				if(!stopped && stoppedNow)
		    				{
		    					mStops.Add(p);
		    				}
		    				
		    				stopped = stoppedNow;
		    			}
		    			
		    			GeoPoint p = new GeoPoint(
		    					(int) (mFile.GetLatitude(mFile.Size() - 1) * 1E6),
		    					(int) (mFile.GetLongitude(mFile.Size() - 1) * 1E6));
		    			mMapController.animateTo(p);
		    			mMapController.setZoom(16);
		    			
		    			mapOverlays.clear();
		    			mapOverlays.add(mTrack);
		    			mapOverlays.add(mStops);
		    					
		    					
		    			LoadPage(null, 0);
	    			}
	    	    	catch(Exception e)
	    	    	{
	    	    		ErrorFile.WriteException(e, getApplicationContext());
	    	    	}
	    	    }
	    	});
	    	builder.setOnCancelListener(new OnCancelListener()
	    	{
				@Override
				public void onCancel(DialogInterface dialog)
				{
				}
	    	});
	    	builder.show();
    	}
    	catch(Exception e)
    	{
    		ErrorFile.WriteException(e, getApplicationContext());
    	}
    }
    private List<File> mTravelFiles = null;
    
    private void LoadPage(Location location, int strength)
    {
    	try
    	{
	    	if(mCurrentPage.equals(OVERVIEW))
	    		LoadOverviewPage(location, strength);
	    	else if(mCurrentPage.equals(MAP))
	    		LoadMapPage(location);
	    	else if(mCurrentPage.equals(SPEED_GRAPH))
	    		LoadSpeedGraphPage();
	    	else if(mCurrentPage.equals(ACCELERATION_GRAPH))
	    		LoadAccelerationGraphPage();
	    	else if(mCurrentPage.equals(ALTITUDE_GRAPH))
	    		LoadAltitudeGraphPage();
	    	else if(mCurrentPage.equals(BEARING_GRAPH))
	    		LoadBearingGraphPage();
	    	else if(mCurrentPage.equals(ACCURACY_GRAPH))
	    		LoadAccuracyGraphPage();
	    	else if(mCurrentPage.equals(SIGNAL_GRAPH))
	    		LoadSignalGraphPage();
	    	else if(mCurrentPage.equals(SUMMARY))
	    		LoadSummaryPage();
	    	else if(mCurrentPage.equals(SETTINGS))
	    		LoadSettingsPage();
    	}
    	catch(Exception e)
    	{
    		ErrorFile.WriteException(e, this);
    	}
    }
    
    private void LoadMapPage(Location location)
    {
    	mGraph.setVisibility(View.GONE);
    	mMainText.setVisibility(View.GONE);
    	mSettingsLayout.setVisibility(View.GONE);
    	mMapView.setVisibility(View.VISIBLE);
    	
    	if(location != null)
    	{
    		GeoPoint p = new GeoPoint(
    			(int) (location.getLatitude() * 1E6),
    			(int) (location.getLongitude() * 1E6));
    	
    		mMapController.animateTo(p);
    		//mMapController.setZoom(16);
    	}
    	else if(mFile != null)
    	{
    		GeoPoint p = new GeoPoint(
        			(int) (mFile.GetLatitude(mFile.Size() - 1) * 1E6),
        			(int) (mFile.GetLongitude(mFile.Size() - 1) * 1E6));
    		mMapController.animateTo(p);
    	}
    	
    	mMapView.invalidate();
    }
    
    private void LoadOverviewPage(Location location, int strength)
    {
    	mGraph.setVisibility(View.GONE);
    	mMainText.setVisibility(View.VISIBLE);
    	mSettingsLayout.setVisibility(View.GONE);
    	mMapView.setVisibility(View.GONE);
    	
    	String status = "";
    	
    	if(location != null)
    	{
    		double longitude = location.getLongitude();
        	double latitude = location.getLatitude();
        	float accuracy = location.getAccuracy();
        	double altitude = location.getAltitude();
        	float bearing = location.getBearing();
        	float speed = location.getSpeed();
        
        	status += String.format("Longitude: %f\n", longitude);
        	status += String.format("Latitude: %f\n", latitude);
        	status += String.format("Altitude: %.02f %s\n", altitude, GPSUnits.GetDistanceAbbreviation(mUnits.AltitudeUnits));
        	status += "\n";
        	status += String.format("Bearing: %.02f\n", bearing);
        	status += String.format("Speed: %.02f %s\n", speed, GPSUnits.GetSpeedAbbreviation(mUnits.SpeedDistanceUnits, mUnits.SpeedTimeUnits));
        	status += String.format("Accuracy: %.02f %s\n", accuracy, GPSUnits.GetDistanceAbbreviation(mUnits.AccuracyUnits));
    	}
    	
    	if(mFilename != null)
    	{
    		status += String.format("\nFile:%s\n\n", mFilename);
    	}
    	
    	if(mFile != null)
    	{
    		float distance = mFile.GetDistanceTravelled(mUnits);
    		status += String.format("Distance travelled: %.02f %s\n", distance, mUnits.DistanceUnits.toLowerCase());
    		
    		status += String.format("Start date: %s\n", DateStrings.GetDateTimeString(mFile.GetStartDate()));
    		status += String.format("End date: %s\n", DateStrings.GetDateTimeString(mFile.GetEndDate()));
    		status += String.format("Elapsed time: %s\n\n", DateStrings.GetElapsedTimeString(mFile.GetRuntime(), 3));
    		
    		float speed = mFile.GetAverageSpeed(mUnits);
    		status += String.format("Average speed: %.02f %s\n", speed, GPSUnits.GetSpeedAbbreviation(mUnits.SpeedDistanceUnits, mUnits.SpeedTimeUnits));
    		status += String.format("Stopped time: %s\n", DateStrings.GetElapsedTimeString(mFile.GetStopTime(mStoppedThreshold, mUnits), 3));
    		status += String.format("Number of stops: %d\n", mFile.GetNumberOfStops(mStoppedThreshold, mUnits));
    		
    		status += String.format("Drive efficiency: %.02f%%\n", mFile.GetEfficiency(mStoppedThreshold, mUnits));
    	}
    	
    	if(strength != mDefaultSignal)
    		status += String.format("\nSignal: %d\n", strength);
    	
        mMainText.setText(status);
    }
    
    private void LoadSpeedGraphPage()
    {
    	if(mFile == null || mFile.Size() == 0)
    	{
        	mGraph.setVisibility(View.GONE);
        	mMainText.setVisibility(View.VISIBLE);
        	mSettingsLayout.setVisibility(View.GONE);
        	mMapView.setVisibility(View.GONE);
        	
        	mMainText.setText("No data to display");
        	
        	return;
    	}
    	
    	//Set the graph visible and text invisible
    	mGraph.setVisibility(View.VISIBLE);
    	mMainText.setVisibility(View.GONE);
    	mSettingsLayout.setVisibility(View.GONE);
    	mMapView.setVisibility(View.GONE);
    	
		Calendar startDate = Calendar.getInstance();

		//Extract the data
		float[] data = new float[mFile.Size()];
		int numValues = 0;
		for(int i=0; i<mFile.Size(); i++)
		{
			if(i == 0)
				startDate.setTimeInMillis(mFile.GetTime(i));
			data[i] = mFile.GetSpeed(i, mUnits);
			if(mFile.GetTime(mFile.Size() - 1) - mFile.GetTime(i) < mGraphHistory)
			{
				numValues++;
			}
		}
		
		//Calculate the average curves
		float[] ave = ArrayMath.GetRunningAverageCurve(data, mRunningAveragePoints);
		float[] allAve = ArrayMath.GetAllTimeRunningAverageCurve(data);
		float average = ArrayMath.GetAverage(data);

		
		//Crop all the data arrays down to what will be displayed
		float[] tempData = new float[numValues];
		float[] tempAve = new float[numValues];
		float[] tempAllAve = new float[numValues];
		int curValue = 0;
		for(int i=0; i<mFile.Size(); i++)
		{
			if(mFile.GetTime(mFile.Size() - 1) - mFile.GetTime(i) < mGraphHistory)
			{
				tempData[curValue] = data[i];
				tempAve[curValue] = ave[i];
				tempAllAve[curValue] = allAve[i];
				curValue++;
			}
		}
		data = tempData;
		ave = tempAve;
		allAve = tempAllAve;
		
		
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
		String units = GPSUnits.GetSpeedAbbreviation(mUnits.SpeedDistanceUnits, mUnits.SpeedTimeUnits);
		mGraph.Title.Text = String.format("%s (%s)\nAverage: %.02f, recent: %.02f\nFirst entry: %s",
				SPEED_GRAPH.toString(), units, average, ave[ave.length - 1], DateStrings.GetDateTimeString(startDate));
		
		//Add the weekend shading and start-of-month indicators
		//mGraph.AddDateInfo(startDate);

		//Turn off labels for the bottom axis since they are drawn with the date info
		mGraph.BottomAxis.DrawLabels = false;
		mGraph.invalidate();
    }
    
    private void LoadAccelerationGraphPage()
    {
    	if(mFile == null || mFile.Size() == 0)
    	{
        	mGraph.setVisibility(View.GONE);
        	mMainText.setVisibility(View.VISIBLE);
        	mSettingsLayout.setVisibility(View.GONE);
        	mMapView.setVisibility(View.GONE);
        	
        	mMainText.setText("No data to display");
        	
        	return;
    	}
    	
    	//Set the graph visible and text invisible
    	mGraph.setVisibility(View.VISIBLE);
    	mMainText.setVisibility(View.GONE);
    	mSettingsLayout.setVisibility(View.GONE);
    	mMapView.setVisibility(View.GONE);
    	
		Calendar startDate = Calendar.getInstance();

		//Extract the data
		float[] data = new float[mFile.Size()];
		float last = 0;
		int numValues = 0;
		for(int i=0; i<mFile.Size(); i++)
		{
			if(i == 0)
				startDate.setTimeInMillis(mFile.GetTime(i));
			float newVal = mFile.GetSpeed(i, mUnits);
			data[i] = newVal - last;
			last = newVal;
			if(mFile.GetTime(mFile.Size() - 1) - mFile.GetTime(i) < mGraphHistory)
			{
				numValues++;
			}
		}
		
		//Calculate the average curves
		float[] ave = ArrayMath.GetRunningAverageCurve(data, mRunningAveragePoints);
		float[] allAve = ArrayMath.GetAllTimeRunningAverageCurve(data);
		float average = ArrayMath.GetAverage(data);

		//Crop all the data arrays down to what will be displayed
		float[] tempData = new float[numValues];
		float[] tempAve = new float[numValues];
		float[] tempAllAve = new float[numValues];
		int curValue = 0;
		for(int i=0; i<mFile.Size(); i++)
		{
			if(mFile.GetTime(mFile.Size() - 1) - mFile.GetTime(i) < mGraphHistory)
			{
				tempData[curValue] = data[i];
				tempAve[curValue] = ave[i];
				tempAllAve[curValue] = allAve[i];
				curValue++;
			}
		}
		data = tempData;
		ave = tempAve;
		allAve = tempAllAve;
		
		
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
		String units = GPSUnits.GetSpeedAbbreviation(mUnits.SpeedDistanceUnits, mUnits.SpeedTimeUnits) + "/s";
		mGraph.Title.Text = String.format("%s (%s)\nAverage: %.02f, recent: %.02f\nFirst entry: %s",
				ACCELERATION_GRAPH.toString(), units, average, ave[ave.length - 1], DateStrings.GetDateTimeString(startDate));
		
		//Add the weekend shading and start-of-month indicators
		//mGraph.AddDateInfo(startDate);

		//Turn off labels for the bottom axis since they are drawn with the date info
		mGraph.BottomAxis.DrawLabels = false;
		mGraph.invalidate();
    }
    
    private void LoadAltitudeGraphPage()
    {
    	if(mFile == null || mFile.Size() == 0)
    	{
        	mGraph.setVisibility(View.GONE);
        	mMainText.setVisibility(View.VISIBLE);
        	mSettingsLayout.setVisibility(View.GONE);
        	mMapView.setVisibility(View.GONE);
        	
        	mMainText.setText("No data to display");
        	
        	return;
    	}
    	
    	//Set the graph visible and text invisible
    	mGraph.setVisibility(View.VISIBLE);
    	mMainText.setVisibility(View.GONE);
    	mSettingsLayout.setVisibility(View.GONE);
    	mMapView.setVisibility(View.GONE);
    	
		Calendar startDate = Calendar.getInstance();

		//Extract the data
		float[] data = new float[mFile.Size()];
		int numValues = 0;
		for(int i=0; i<mFile.Size(); i++)
		{
			if(i == 0)
				startDate.setTimeInMillis(mFile.GetTime(i));
			data[i] = (float) mFile.GetAltitude(i, mUnits);
			if(mFile.GetTime(mFile.Size() - 1) - mFile.GetTime(i) < mGraphHistory)
			{
				numValues++;
			}
		}
		
		//Calculate the average curves
		float[] ave = ArrayMath.GetRunningAverageCurve(data, mRunningAveragePoints);
		float[] allAve = ArrayMath.GetAllTimeRunningAverageCurve(data);
		float average = ArrayMath.GetAverage(data);

		//Crop all the data arrays down to what will be displayed
		float[] tempData = new float[numValues];
		float[] tempAve = new float[numValues];
		float[] tempAllAve = new float[numValues];
		int curValue = 0;
		for(int i=0; i<mFile.Size(); i++)
		{
			if(mFile.GetTime(mFile.Size() - 1) - mFile.GetTime(i) < mGraphHistory)
			{
				tempData[curValue] = data[i];
				tempAve[curValue] = ave[i];
				tempAllAve[curValue] = allAve[i];
				curValue++;
			}
		}
		data = tempData;
		ave = tempAve;
		allAve = tempAllAve;
		
		
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
		String units = GPSUnits.GetDistanceAbbreviation(mUnits.AltitudeUnits);
		mGraph.Title.Text = String.format("%s (%s)\nAverage: %.02f, recent: %.02f\nFirst entry: %s",
				ALTITUDE_GRAPH.toString(), units, average, ave[ave.length - 1], DateStrings.GetDateTimeString(startDate));
		
		//Add the weekend shading and start-of-month indicators
		//mGraph.AddDateInfo(startDate);

		//Turn off labels for the bottom axis since they are drawn with the date info
		mGraph.BottomAxis.DrawLabels = false;
		mGraph.invalidate();
    }
    
    private void LoadBearingGraphPage()
    {
    	if(mFile == null || mFile.Size() == 0)
    	{
        	mGraph.setVisibility(View.GONE);
        	mMainText.setVisibility(View.VISIBLE);
        	mSettingsLayout.setVisibility(View.GONE);
        	mMapView.setVisibility(View.GONE);
        	
        	mMainText.setText("No data to display");
        	
        	return;
    	}
    	
    	//Set the graph visible and text invisible
    	mGraph.setVisibility(View.VISIBLE);
    	mMainText.setVisibility(View.GONE);
    	mSettingsLayout.setVisibility(View.GONE);
    	mMapView.setVisibility(View.GONE);
    	
		Calendar startDate = Calendar.getInstance();

		//Extract the data
		float[] data = new float[mFile.Size()];
		int numValues = 0;
		for(int i=0; i<mFile.Size(); i++)
		{
			if(i == 0)
				startDate.setTimeInMillis(mFile.GetTime(i));
			data[i] = mFile.GetBearing(i);
			if(mFile.GetTime(mFile.Size() - 1) - mFile.GetTime(i) < mGraphHistory)
			{
				numValues++;
			}
		}
		
		//Calculate the average curves
		float[] ave = ArrayMath.GetRunningAverageCurve(data, mRunningAveragePoints);
		float[] allAve = ArrayMath.GetAllTimeRunningAverageCurve(data);
		float average = ArrayMath.GetAverage(data);

		//Crop all the data arrays down to what will be displayed
		float[] tempData = new float[numValues];
		float[] tempAve = new float[numValues];
		float[] tempAllAve = new float[numValues];
		int curValue = 0;
		for(int i=0; i<mFile.Size(); i++)
		{
			if(mFile.GetTime(mFile.Size() - 1) - mFile.GetTime(i) < mGraphHistory)
			{
				tempData[curValue] = data[i];
				tempAve[curValue] = ave[i];
				tempAllAve[curValue] = allAve[i];
				curValue++;
			}
		}
		data = tempData;
		ave = tempAve;
		allAve = tempAllAve;
		
		
		mGraph.PolarPlot(data);
		
		//Setup the all-data plot
		mGraph.Plots.get(0).SetColor(Color.WHITE);
		mGraph.Plots.get(0).DrawPoints = false;
		mGraph.Plots.get(0).PointColor = Color.YELLOW;

		//Setup the all-time average plot
		//mGraph.Plots.add(new GraphPlot(allAve, 1));
		//mGraph.Plots.get(1).SetColor(Color.RED);
		//mGraph.Plots.get(1).DrawPoints = false;

		//Setup the running average plot
		//mGraph.Plots.add(new GraphPlot(ave, 1));
		//mGraph.Plots.get(2).SetColor(Color.GREEN);
		//mGraph.Plots.get(2).DrawPoints = false;
		
		//Setup the title
		mGraph.Title.Text = String.format("%s (degrees)\nAverage: %.02f, recent: %.02f\nFirst entry: %s",
				BEARING_GRAPH.toString(), average, ave[ave.length - 1], DateStrings.GetDateTimeString(startDate));
		
		//Add the weekend shading and start-of-month indicators
		//mGraph.AddDateInfo(startDate);

		//Turn off labels for the bottom axis since they are drawn with the date info
		//mGraph.BottomAxis.DrawLabels = false;
		mGraph.invalidate();
    }
    
    private void LoadAccuracyGraphPage()
    {
    	if(mFile == null || mFile.Size() == 0)
    	{
        	mGraph.setVisibility(View.GONE);
        	mMainText.setVisibility(View.VISIBLE);
        	mSettingsLayout.setVisibility(View.GONE);
        	mMapView.setVisibility(View.GONE);
        	
        	mMainText.setText("No data to display");
        	
        	return;
    	}
    	
    	//Set the graph visible and text invisible
    	mGraph.setVisibility(View.VISIBLE);
    	mMainText.setVisibility(View.GONE);
    	mSettingsLayout.setVisibility(View.GONE);
    	mMapView.setVisibility(View.GONE);
    	
		Calendar startDate = Calendar.getInstance();

		//Extract the data
		float[] data = new float[mFile.Size()];
		int numValues = 0;
		for(int i=0; i<mFile.Size(); i++)
		{
			if(i == 0)
				startDate.setTimeInMillis(mFile.GetTime(i));
			data[i] = mFile.GetAccuracy(i, mUnits);
			if(mFile.GetTime(mFile.Size() - 1) - mFile.GetTime(i) < mGraphHistory)
			{
				numValues++;
			}
		}
		
		//Calculate the average curves
		float[] ave = ArrayMath.GetRunningAverageCurve(data, mRunningAveragePoints);
		float[] allAve = ArrayMath.GetAllTimeRunningAverageCurve(data);
		float average = ArrayMath.GetAverage(data);

		//Crop all the data arrays down to what will be displayed
		float[] tempData = new float[numValues];
		float[] tempAve = new float[numValues];
		float[] tempAllAve = new float[numValues];
		int curValue = 0;
		for(int i=0; i<mFile.Size(); i++)
		{
			if(mFile.GetTime(mFile.Size() - 1) - mFile.GetTime(i) < mGraphHistory)
			{
				tempData[curValue] = data[i];
				tempAve[curValue] = ave[i];
				tempAllAve[curValue] = allAve[i];
				curValue++;
			}
		}
		data = tempData;
		ave = tempAve;
		allAve = tempAllAve;
		
		
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
		String units = GPSUnits.GetDistanceAbbreviation(mUnits.AccuracyUnits);
		mGraph.Title.Text = String.format("%s (%s)\nAverage: %.02f, recent: %.02f\nFirst entry: %s",
				ACCURACY_GRAPH.toString(), units, average, ave[ave.length - 1], DateStrings.GetDateTimeString(startDate));		
		//Add the weekend shading and start-of-month indicators
		//mGraph.AddDateInfo(startDate);

		//Turn off labels for the bottom axis since they are drawn with the date info
		mGraph.BottomAxis.DrawLabels = false;
		mGraph.invalidate();
    }
    
    private void LoadSignalGraphPage()
    {
    	if(mFile == null || mFile.Size() == 0)
    	{
        	mGraph.setVisibility(View.GONE);
        	mMainText.setVisibility(View.VISIBLE);
        	mSettingsLayout.setVisibility(View.GONE);
        	mMapView.setVisibility(View.GONE);
        	
        	mMainText.setText("No data to display");
        	
        	return;
    	}
    	
    	//Set the graph visible and text invisible
    	mGraph.setVisibility(View.VISIBLE);
    	mMainText.setVisibility(View.GONE);
    	mSettingsLayout.setVisibility(View.GONE);
    	mMapView.setVisibility(View.GONE);
    	
		Calendar startDate = Calendar.getInstance();

		//Extract the data
		float[] data = new float[mFile.Size()];
		int numValues = 0;
		for(int i=0; i<mFile.Size(); i++)
		{
			if(i == 0)
				startDate.setTimeInMillis(mFile.GetTime(i));
			data[i] = mFile.GetStrength(i);
			if(mFile.GetTime(mFile.Size() - 1) - mFile.GetTime(i) < mGraphHistory)
			{
				numValues++;
			}
		}
		
		//Calculate the average curves
		float[] ave = ArrayMath.GetRunningAverageCurve(data, mRunningAveragePoints);
		float[] allAve = ArrayMath.GetAllTimeRunningAverageCurve(data);
		float average = ArrayMath.GetAverage(data);

		//Crop all the data arrays down to what will be displayed
		float[] tempData = new float[numValues];
		float[] tempAve = new float[numValues];
		float[] tempAllAve = new float[numValues];
		int curValue = 0;
		for(int i=0; i<mFile.Size(); i++)
		{
			if(mFile.GetTime(mFile.Size() - 1) - mFile.GetTime(i) < mGraphHistory)
			{
				tempData[curValue] = data[i];
				tempAve[curValue] = ave[i];
				tempAllAve[curValue] = allAve[i];
				curValue++;
			}
		}
		data = tempData;
		ave = tempAve;
		allAve = tempAllAve;
		
		
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
		String units = GPSUnits.GetDistanceAbbreviation(mUnits.AccuracyUnits);
		mGraph.Title.Text = String.format("%s (%s)\nAverage: %.02f, recent: %.02f\nFirst entry: %s",
				ACCURACY_GRAPH.toString(), units, average, ave[ave.length - 1], DateStrings.GetDateTimeString(startDate));		
		//Add the weekend shading and start-of-month indicators
		//mGraph.AddDateInfo(startDate);

		//Turn off labels for the bottom axis since they are drawn with the date info
		mGraph.BottomAxis.DrawLabels = false;
		mGraph.invalidate();
    }
    
    private void LoadSummaryPage()
    {
    	mGraph.setVisibility(View.GONE);
    	mMainText.setVisibility(View.VISIBLE);
    	mSettingsLayout.setVisibility(View.GONE);
    	mMapView.setVisibility(View.GONE);
    	
    	String status = "";
    	
    	//Find the available log files
    	File files[] = Environment.getExternalStorageDirectory().listFiles();
    	
    	mTravelFiles = new ArrayList<File>();
    	for(int i=0; i<files.length; i++)
    		if(files[i].getName().startsWith(FILE_START))
    			mTravelFiles.add(files[i]);
    	
    	for(int i=0; i<mTravelFiles.size(); i++)
    	{
    		String curFilename = mTravelFiles.get(i).getAbsolutePath();
    		
    		CoordFile tempFile = new CoordFile(mContext, curFilename);
    		
    		status += String.format("%s,%.02f,%s,%.02f,%d,%s,%.02f%%\n",
    				  DateStrings.GetDateTimeString(tempFile.GetStartDate()),
    				  tempFile.GetDistanceTravelled(mUnits),
    				  DateStrings.GetElapsedTimeString(tempFile.GetRuntime(), 3),
    				  tempFile.GetAverageSpeed(mUnits),
    				  tempFile.GetNumberOfStops(mStoppedThreshold, mUnits),
    				  DateStrings.GetElapsedTimeString(tempFile.GetStopTime(mStoppedThreshold, mUnits), 3),
    				  tempFile.GetEfficiency(mStoppedThreshold, mUnits)
    				  );
    		tempFile.Close();
    	}
    	
    	mMainText.setText(status);
    }
    
    private void LoadSettingsPage()
    {
    	//Set the settings visible
    	mGraph.setVisibility(View.GONE);
    	mMainText.setVisibility(View.GONE);
    	mSettingsLayout.setVisibility(View.VISIBLE);
    	mMapView.setVisibility(View.GONE);
    }
    
    private class MyLocationListener implements LocationListener
    {

        @Override
        public void onLocationChanged(Location location)
        {
        	try
        	{
	        	mStatusText.setText(ACQUIRING);
	        	
	        	mFile.WriteEntry(location, mCurrentSignal);
	            
	        	mTrack.Add(new GeoPoint((int)(location.getLatitude() * 1E6), (int)(location.getLongitude() * 1E6)));
	        	
	        	boolean stoppedNow = mFile.IsStopped(mStoppedThreshold, mUnits);
	        	
	        	if(!mStopped && stoppedNow)
	        	{
	        		mStops.Add(new GeoPoint((int)(location.getLatitude() * 1E6), (int)(location.getLongitude() * 1E6)));
	        	}
	        	mStopped = stoppedNow;
	        	
	            LoadPage(location, mCurrentSignal);
        	}
        	catch(Exception e)
        	{
        		ErrorFile.WriteException(e, getApplicationContext());
        	}
        }

		@Override
		public void onProviderDisabled(String provider)
		{
		}

		@Override
		public void onProviderEnabled(String provider)
		{
			//Toast t = Toast.makeText(getApplicationContext(), String.format("Provider enabled"), Toast.LENGTH_SHORT);
            //t.show();
		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras)
		{
		}
    }
    
    private class RecordListener implements View.OnClickListener
    {
    	public RecordListener()
    	{
    	}
    	
    	public void onClick(View v)
    	{
    		try
    		{
    			if(mRecording)
    			{
    				StopGPS();
    				
    				mRecording = !mRecording;
    				
    				Toast t = Toast.makeText(mContext, "Stopped GPS", Toast.LENGTH_SHORT);
    				t.show();
    			}
    			else
    			{
    				//Ask whether to continue previous log or start new?
        			AlertDialog.Builder builder =  new AlertDialog.Builder(mContext);
        	    	builder.setMessage("Continue last file or start new?");
        	    	builder.setPositiveButton("Continue", new DialogInterface.OnClickListener()
        	    	{
        	            @Override
        	            public void onClick(DialogInterface dialog, int which)
        	            {
        	            	if(mFilename == null)
        	            	{
        	            		//Search for the most recent log
        	            	}
        	            	StartGPS();
        	            	
        	            	mRecording = !mRecording;
        	            	
        	            	Toast t = Toast.makeText(mContext, "Started GPS", Toast.LENGTH_SHORT);
        	        		t.show();
        	            }
        	        });
        	    	builder.setNegativeButton("New", new DialogInterface.OnClickListener()
        	    	{
        	            @Override
        	            public void onClick(DialogInterface dialog, int which)
        	            {
        	            	mTrack = new TrackOverlay();
        	            	mStops = new StopOverlay();
        	            	
        	            	String rootDirectory = Environment.getExternalStorageDirectory().getPath() + '/' + LogDirectory + "/";
        	            	
        	            	File dir = new File(rootDirectory);
        	            	if(!dir.exists() && !dir.mkdirs())
        	            	{
        	            		Toast t = Toast.makeText(getApplicationContext(), "Could not create directory " + rootDirectory, Toast.LENGTH_LONG);
        	            		t.show();
        	            	}
        	            	
        	            	//Create a new file
        	            	mFilename = String.format(rootDirectory + "TravelLog_%s.txt", DateStrings.GetDateTimeString(Calendar.getInstance()));
        	            	StartGPS();
        	            	
        	            	mRecording = !mRecording;
        	            	
        	            	Toast t = Toast.makeText(mContext, "Started GPS", Toast.LENGTH_SHORT);
        	        		t.show();
        	            }
        	        });
        	    	builder.setOnCancelListener(new OnCancelListener()
        	    	{
						@Override
						public void onCancel(DialogInterface dialog)
						{
							mRecordButton.setChecked(false);
						}
        	    	});
        	    	builder.show();
    			}
    		}
        	catch(Exception e)
        	{
        		ErrorFile.WriteException(e, getApplicationContext());
        	}
    	}
    }
    
    private void StartGPS()
    {
    	try
    	{
	    	if(mFile != null && mFile.Filename != mFilename)
	    	{
	    		//Close the current file if we're starting a different one
	    		CloseFile();
	    		mFile = null;
	    	}
	    	if(mFile == null)
	    	{
	    		//Setup new overlays if we're starting a new file
	    		mTrack = new TrackOverlay();
	    		mStops = new StopOverlay();
	    		List<Overlay> mapOverlays = mMapView.getOverlays();
	    		mapOverlays.clear();
	    		mapOverlays.add(mTrack);
	    		mapOverlays.add(mStops);
	    	}
	    	
	    	mFile = new CoordFile(this, mFilename);
	    	mFilename = mFile.Filename;
	    	
	    	mLocMgr = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
	        
	        final boolean gpsEnabled = mLocMgr.isProviderEnabled(LocationManager.GPS_PROVIDER);
	
	        if (!gpsEnabled)
	        {
	            // Build an alert dialog here that requests that the user enable
	            // the location services, then when the user clicks the "OK" button:
	        	
	        	//Intent settingsIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
	            //startActivity(settingsIntent);
	        }
	        
	        //LocationProvider provider = locationManager.getProvider(LocationManager.GPS_PROVIDER);
	        
	        mListener = new MyLocationListener();
	        mLocMgr.requestLocationUpdates(LocationManager.GPS_PROVIDER, LogInterval, 0, mListener);
	        
	        mSignalListener = new PhoneStateListener()
	        {
	        	@Override
	        	public void onSignalStrengthsChanged(SignalStrength signalStrength)
	        	{
	        		mCurrentSignal = Math.abs(signalStrength.getCdmaDbm());
	        		//String strengths = String.format("%d, %d, %d, %d", signalStrength.getCdmaDbm(), signalStrength.getEvdoDbm(), signalStrength.getEvdoSnr(), signalStrength.getGsmSignalStrength());
	        		//ErrorFile.Write("TravelTracker", String.format("Signal strength: %s", strengths), getApplicationContext());
	        	}
	        };
	        
	        TelephonyManager mTelephonyManager = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
	        mTelephonyManager.listen(mSignalListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
	        
	        mStatusText.setText(INITIALIZING);
    	}
    	catch(Exception e)
    	{
    		ErrorFile.WriteException(e, getApplicationContext());
    	}
    }
    
    private void StopGPS()
    {
    	try
    	{
	    	if(mLocMgr != null && mListener != null)
	    	{
	    		mLocMgr.removeUpdates(mListener);
	    		
	    		mListener = null;
	    		mLocMgr = null;
	    	}
    	}
    	catch(Exception e)
    	{
    		ErrorFile.WriteException(e, getApplicationContext());
    	}
    	try
    	{
	    	if(mTelephonyManager != null && mSignalListener != null)
	    	{
	    		mTelephonyManager.listen(mSignalListener, PhoneStateListener.LISTEN_NONE);
	    		
	    		mSignalListener = null;
	    		mTelephonyManager = null;
	    	}
    	}
    	catch(Exception e)
    	{
    		ErrorFile.WriteException(e, getApplicationContext());
    	}
    	
    	mStatusText.setText(IDLE);
    }
    
    private void CloseFile()
    {
    	if(mFile != null)
    	{
    		mFile.Close();
    		mFile = null;
    	}
    }
    
    //Called when the activity exits
    @Override
    public void onDestroy()
    {
    	StopGPS();
        super.onDestroy();
    }

    
    private int GetGraphHistory(String input)
    {
    	int strLength = input.length();
    	String numStr = input.substring(0, strLength - 1);
    	String unitStr = input.substring(strLength - 1);
    	
    	int ret = Integer.parseInt(numStr);
    	
    	if(unitStr.equals("h"))
    		ret *= 3600000;
    	else if(unitStr.equals("m"))
    		ret *= 60000;
    	else if(unitStr.equals("s"))
    		ret *= 1000;
    	
    	return ret;
    }

	@Override
	protected boolean isRouteDisplayed()
	{
		return false;
	}
	
	public class TrackOverlay extends Overlay
	{
		private ArrayList<GeoPoint> mPoints = new ArrayList<GeoPoint>();
		
		public TrackOverlay()
		{
		}
		
		public void Add(GeoPoint geoPoint)
		{
			mPoints.add(geoPoint);
		}
		
		@Override
	    public void draw(Canvas canvas, MapView mapV, boolean shadow)
		{
			try
			{
				Projection projection = mMapView.getProjection();
				Paint paint = new Paint();
				paint.setARGB(255, 0, 0, 255);
				paint.setStrokeWidth(2);
				
				Point lastPoint = null;
				for(int i=0; i<mPoints.size(); i++)
				{
					Point curPoint = new Point();
					projection.toPixels(mPoints.get(i), curPoint);
					if(lastPoint != null)
					{
						canvas.drawLine(lastPoint.x, lastPoint.y, curPoint.x, curPoint.y, paint);
					}
					//canvas.drawCircle(curPoint.x, curPoint.y, 2, paint);
					lastPoint = curPoint;
				}
			}
	    	catch(Exception e)
	    	{
	    		ErrorFile.WriteException(e, getApplicationContext());
	    	}
		}
	}
	
	public class StopOverlay extends Overlay
	{
		private ArrayList<GeoPoint> mPoints = new ArrayList<GeoPoint>();
		
		public StopOverlay()
		{
		}
		
		public void Add(GeoPoint geoPoint)
		{
			mPoints.add(geoPoint);
		}
		
		@Override
	    public void draw(Canvas canvas, MapView mapV, boolean shadow)
		{
			try
			{
				Projection projection = mMapView.getProjection();
				Paint paint = new Paint();
				paint.setARGB(255, 255, 0, 0);
				paint.setStrokeWidth(2);
				
				for(int i=0; i<mPoints.size(); i++)
				{
					Point curPoint = new Point();
					projection.toPixels(mPoints.get(i), curPoint);
					canvas.drawCircle(curPoint.x, curPoint.y, 5, paint);
				}
			}
	    	catch(Exception e)
	    	{
	    		ErrorFile.WriteException(e, getApplicationContext());
	    	}
		}
	}


}