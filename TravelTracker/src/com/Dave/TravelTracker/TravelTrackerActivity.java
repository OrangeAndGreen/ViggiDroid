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
import android.content.Intent;
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
	private TravelTrackerSettings mSettings = null;
	
	private Context mContext = null;
	private CoordFile mFile = null;
	
	//GPS Manager and Listener
	private LocationManager mLocMgr = null;
	private MyLocationListener mListener = null;
	
	//Signal strength Manager and Listener
	private TelephonyManager mTelephonyManager = null;
	private PhoneStateListener mSignalListener = null;
	private int mDefaultSignal = -1000;
	private int mCurrentSignal = mDefaultSignal;
	
	//Map Controller
	private MapController mMapController = null;
	private TrackOverlay mTrack = null;
	private StopOverlay mStops = null;
	private int[] mColorTable = null;
	
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
        
        mSettings = new TravelTrackerSettings(getPreferences(MODE_PRIVATE), this);
        
        if(mSettings.CurrentPage == null)
        	mSettings.CurrentPage = OVERVIEW.toString();
        
        mColorTable = BuildColorTable();
        
        try
        {
	        setContentView(R.layout.main);
	        InitializeInterface();
	        
	        if(mSettings.IsRecording)
	        {
	        	mRecordButton.setChecked(mSettings.IsRecording);
	        	
	        	StartGPS();
	        }
	        else if(mSettings.Filename != null)
	        {
	        	mFile = new CoordFile(mContext, mSettings.Filename);
	        }
	        
	        LoadPage(null);
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
	        mMapView.setSatellite(mSettings.ShowSatellites);
	        mMapView.setTraffic(mSettings.ShowTraffic);
	        
	        mRecordButton.setOnClickListener(new RecordListener());
	        
	        mShowSatelliteCheck.setChecked(mSettings.ShowSatellites);
	        mShowSatelliteCheck.setOnCheckedChangeListener(new OnCheckedChangeListener()
	        {
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
				{
					try
					{
						mSettings.ShowSatellites = isChecked;
						mMapView.setSatellite(mSettings.ShowSatellites);
					}
			    	catch(Exception e)
			    	{
			    		ErrorFile.WriteException(e, getApplicationContext());
			    	}
				}
	        });
	        
	        mShowTrafficCheck.setChecked(mSettings.ShowTraffic);
	        mShowTrafficCheck.setOnCheckedChangeListener(new OnCheckedChangeListener()
	        {
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
				{
					try
					{
						mSettings.ShowTraffic = isChecked;
						mMapView.setTraffic(mSettings.ShowTraffic);
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
						mSettings.CurrentPage = mPages[mPageSelector.getSelectedItemPosition()].toString();
		        	
		        		LoadPage(null);
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
					
						mSettings.HistoryLength = GetGraphHistory(value);
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
					
						mSettings.RunningAverageLength = GetGraphHistory(value) / 1000;
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
						mSettings.Units.AltitudeUnits = mDistanceUnits[mAltitudeSelector.getSelectedItemPosition()].toString();
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
						mSettings.Units.AccuracyUnits = mDistanceUnits[mAccuracySelector.getSelectedItemPosition()].toString();
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
						mSettings.Units.DistanceUnits = mDistanceUnits[mDistanceSelector.getSelectedItemPosition()].toString();
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
						mSettings.Units.SpeedDistanceUnits = mDistanceUnits[mSpeedDistanceSelector.getSelectedItemPosition()].toString();
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
						mSettings.Units.SpeedTimeUnits = mTimeUnits[mSpeedTimeSelector.getSelectedItemPosition()].toString();
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
	        	if(mSettings.IsRecording)
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
		    	            	
		    	            	mSettings.IsRecording = !mSettings.IsRecording;
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
    		String rootDirectory = Environment.getExternalStorageDirectory().getPath() + '/' + mSettings.LogDirectory + "/";
        	
        	File dir = new File(rootDirectory);
        	if(!dir.exists() && !dir.mkdirs())
        	{
        		Toast t = Toast.makeText(getApplicationContext(), "Could not create directory " + rootDirectory, Toast.LENGTH_LONG);
        		t.show();
        	}
    		
	    	//Find the available log files
	    	File files[] = dir.listFiles();
	    	
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
		    			mSettings.Filename = mFile.Filename;
		    			
		    			List<Overlay> mapOverlays = mMapView.getOverlays();
		    			mTrack = new TrackOverlay();
		    			mStops = new StopOverlay();
		    			boolean stopped = true;
		    			for(int i=0; i<mFile.Size(); i++)
		    			{
		    				GeoPoint p = new GeoPoint(
		        					(int) (mFile.GetLatitude(i) * 1E6),
		        					(int) (mFile.GetLongitude(i) * 1E6));
		    				mTrack.Add(p, mFile.GetSpeed(i, mSettings.Units));
		    				
		    				boolean stoppedNow = mFile.IsStopped(i - 1, i, mSettings.StoppedThreshold, mSettings.Units);
		    				
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
		    					
		    					
		    			LoadPage(null);
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
    
    private void LoadPage(Location location)
    {
    	try
    	{
	    	if(mSettings.CurrentPage.equals(OVERVIEW))
	    		LoadOverviewPage(location);
	    	else if(mSettings.CurrentPage.equals(MAP))
	    		LoadMapPage(location);
	    	else if(mSettings.CurrentPage.equals(SPEED_GRAPH))
	    		LoadSpeedGraphPage();
	    	else if(mSettings.CurrentPage.equals(ACCELERATION_GRAPH))
	    		LoadAccelerationGraphPage();
	    	else if(mSettings.CurrentPage.equals(ALTITUDE_GRAPH))
	    		LoadAltitudeGraphPage();
	    	else if(mSettings.CurrentPage.equals(BEARING_GRAPH))
	    		LoadBearingGraphPage();
	    	else if(mSettings.CurrentPage.equals(ACCURACY_GRAPH))
	    		LoadAccuracyGraphPage();
	    	else if(mSettings.CurrentPage.equals(SIGNAL_GRAPH))
	    		LoadSignalGraphPage();
	    	else if(mSettings.CurrentPage.equals(SUMMARY))
	    		LoadSummaryPage();
	    	else if(mSettings.CurrentPage.equals(SETTINGS))
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
    	else if(mFile != null && mFile.Size() > 0)
    	{
    		GeoPoint p = new GeoPoint(
        			(int) (mFile.GetLatitude(mFile.Size() - 1) * 1E6),
        			(int) (mFile.GetLongitude(mFile.Size() - 1) * 1E6));
    		mMapController.animateTo(p);
    	}
    	
    	mMapView.invalidate();
    }
    
    private void LoadOverviewPage(Location location)
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
        	status += String.format("Altitude: %.02f %s\n", altitude, GPSUnits.GetDistanceAbbreviation(mSettings.Units.AltitudeUnits));
        	status += "\n";
        	status += String.format("Bearing: %.02f\n", bearing);
        	status += String.format("Speed: %.02f %s\n", speed, GPSUnits.GetSpeedAbbreviation(mSettings.Units.SpeedDistanceUnits, mSettings.Units.SpeedTimeUnits));
        	status += String.format("Accuracy: %.02f %s\n", accuracy, GPSUnits.GetDistanceAbbreviation(mSettings.Units.AccuracyUnits));
    	}
    	
    	if(mSettings.Filename != null)
    	{
    		status += String.format("\nFile:%s\n\n", mSettings.Filename);
    	}
    	
    	if(mFile != null)
    	{
    		float distance = mFile.GetDistanceTravelled(mSettings.Units);
    		status += String.format("Distance travelled: %.02f %s\n", distance, mSettings.Units.DistanceUnits.toLowerCase());
    		
    		status += String.format("Start date: %s\n", DateStrings.GetDateTimeString(mFile.GetStartDate()));
    		status += String.format("End date: %s\n", DateStrings.GetDateTimeString(mFile.GetEndDate()));
    		status += String.format("Elapsed time: %s\n\n", DateStrings.GetElapsedTimeString(mFile.GetRuntime(), 3));
    		
    		float speed = mFile.GetAverageSpeed(mSettings.Units);
    		status += String.format("Average speed: %.02f %s\n", speed, GPSUnits.GetSpeedAbbreviation(mSettings.Units.SpeedDistanceUnits, mSettings.Units.SpeedTimeUnits));
    		status += String.format("Stopped time: %s\n", DateStrings.GetElapsedTimeString(mFile.GetStopTime(mSettings.StoppedThreshold, mSettings.Units), 3));
    		status += String.format("Number of stops: %d\n", mFile.GetNumberOfStops(mSettings.StoppedThreshold, mSettings.Units));
    		
    		status += String.format("Drive efficiency: %.02f%%\n", mFile.GetEfficiency(mSettings.StoppedThreshold, mSettings.Units));
    	}
    	
    	if(mCurrentSignal != mDefaultSignal)
    		status += String.format("\nSignal: %d\n", mCurrentSignal);
    	
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
			data[i] = mFile.GetSpeed(i, mSettings.Units);
			if(mFile.GetTime(mFile.Size() - 1) - mFile.GetTime(i) < mSettings.HistoryLength)
			{
				numValues++;
			}
		}
		
		//Calculate the average curves
		float[] ave = ArrayMath.GetRunningAverageCurve(data, mSettings.RunningAverageLength);
		float[] allAve = ArrayMath.GetAllTimeRunningAverageCurve(data);
		float average = ArrayMath.GetAverage(data);

		
		//Crop all the data arrays down to what will be displayed
		float[] tempData = new float[numValues];
		float[] tempAve = new float[numValues];
		float[] tempAllAve = new float[numValues];
		int curValue = 0;
		for(int i=0; i<mFile.Size(); i++)
		{
			if(mFile.GetTime(mFile.Size() - 1) - mFile.GetTime(i) < mSettings.HistoryLength)
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
		String units = GPSUnits.GetSpeedAbbreviation(mSettings.Units.SpeedDistanceUnits, mSettings.Units.SpeedTimeUnits);
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
			float newVal = mFile.GetSpeed(i, mSettings.Units);
			data[i] = newVal - last;
			last = newVal;
			if(mFile.GetTime(mFile.Size() - 1) - mFile.GetTime(i) < mSettings.HistoryLength)
			{
				numValues++;
			}
		}
		
		//Calculate the average curves
		float[] ave = ArrayMath.GetRunningAverageCurve(data, mSettings.RunningAverageLength);
		float[] allAve = ArrayMath.GetAllTimeRunningAverageCurve(data);
		float average = ArrayMath.GetAverage(data);

		//Crop all the data arrays down to what will be displayed
		float[] tempData = new float[numValues];
		float[] tempAve = new float[numValues];
		float[] tempAllAve = new float[numValues];
		int curValue = 0;
		for(int i=0; i<mFile.Size(); i++)
		{
			if(mFile.GetTime(mFile.Size() - 1) - mFile.GetTime(i) < mSettings.HistoryLength)
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
		String units = GPSUnits.GetSpeedAbbreviation(mSettings.Units.SpeedDistanceUnits, mSettings.Units.SpeedTimeUnits) + "/s";
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
			data[i] = (float) mFile.GetAltitude(i, mSettings.Units);
			if(mFile.GetTime(mFile.Size() - 1) - mFile.GetTime(i) < mSettings.HistoryLength)
			{
				numValues++;
			}
		}
		
		//Calculate the average curves
		float[] ave = ArrayMath.GetRunningAverageCurve(data, mSettings.RunningAverageLength);
		float[] allAve = ArrayMath.GetAllTimeRunningAverageCurve(data);
		float average = ArrayMath.GetAverage(data);

		//Crop all the data arrays down to what will be displayed
		float[] tempData = new float[numValues];
		float[] tempAve = new float[numValues];
		float[] tempAllAve = new float[numValues];
		int curValue = 0;
		for(int i=0; i<mFile.Size(); i++)
		{
			if(mFile.GetTime(mFile.Size() - 1) - mFile.GetTime(i) < mSettings.HistoryLength)
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
		String units = GPSUnits.GetDistanceAbbreviation(mSettings.Units.AltitudeUnits);
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
			if(mFile.GetTime(mFile.Size() - 1) - mFile.GetTime(i) < mSettings.HistoryLength)
			{
				numValues++;
			}
		}
		
		//Calculate the average curves
		float[] ave = ArrayMath.GetRunningAverageCurve(data, mSettings.RunningAverageLength);
		float[] allAve = ArrayMath.GetAllTimeRunningAverageCurve(data);
		float average = ArrayMath.GetAverage(data);

		//Crop all the data arrays down to what will be displayed
		float[] tempData = new float[numValues];
		float[] tempAve = new float[numValues];
		float[] tempAllAve = new float[numValues];
		int curValue = 0;
		for(int i=0; i<mFile.Size(); i++)
		{
			if(mFile.GetTime(mFile.Size() - 1) - mFile.GetTime(i) < mSettings.HistoryLength)
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
			data[i] = mFile.GetAccuracy(i, mSettings.Units);
			if(mFile.GetTime(mFile.Size() - 1) - mFile.GetTime(i) < mSettings.HistoryLength)
			{
				numValues++;
			}
		}
		
		//Calculate the average curves
		float[] ave = ArrayMath.GetRunningAverageCurve(data, mSettings.RunningAverageLength);
		float[] allAve = ArrayMath.GetAllTimeRunningAverageCurve(data);
		float average = ArrayMath.GetAverage(data);

		//Crop all the data arrays down to what will be displayed
		float[] tempData = new float[numValues];
		float[] tempAve = new float[numValues];
		float[] tempAllAve = new float[numValues];
		int curValue = 0;
		for(int i=0; i<mFile.Size(); i++)
		{
			if(mFile.GetTime(mFile.Size() - 1) - mFile.GetTime(i) < mSettings.HistoryLength)
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
		String units = GPSUnits.GetDistanceAbbreviation(mSettings.Units.AccuracyUnits);
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
    	//Log.i("TravelTracker", "Loading signal graph page");
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
			if(mFile.GetTime(mFile.Size() - 1) - mFile.GetTime(i) < mSettings.HistoryLength)
			{
				numValues++;
			}
		}
		
		//Calculate the average curves
		float[] ave = ArrayMath.GetRunningAverageCurve(data, mSettings.RunningAverageLength);
		float[] allAve = ArrayMath.GetAllTimeRunningAverageCurve(data);
		float average = ArrayMath.GetAverage(data);

		//Crop all the data arrays down to what will be displayed
		float[] tempData = new float[numValues];
		float[] tempAve = new float[numValues];
		float[] tempAllAve = new float[numValues];
		int curValue = 0;
		for(int i=0; i<mFile.Size(); i++)
		{
			if(mFile.GetTime(mFile.Size() - 1) - mFile.GetTime(i) < mSettings.HistoryLength)
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
		mGraph.Title.Text = String.format("%s\nAverage: %.02f, recent: %.02f\nFirst entry: %s",
				SIGNAL_GRAPH.toString(), average, ave[ave.length - 1], DateStrings.GetDateTimeString(startDate));		
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
    				  tempFile.GetDistanceTravelled(mSettings.Units),
    				  DateStrings.GetElapsedTimeString(tempFile.GetRuntime(), 3),
    				  tempFile.GetAverageSpeed(mSettings.Units),
    				  tempFile.GetNumberOfStops(mSettings.StoppedThreshold, mSettings.Units),
    				  DateStrings.GetElapsedTimeString(tempFile.GetStopTime(mSettings.StoppedThreshold, mSettings.Units), 3),
    				  tempFile.GetEfficiency(mSettings.StoppedThreshold, mSettings.Units)
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
	        	
	        	if(mFile == null)
	        		return;
	        	
	        	mFile.WriteEntry(location, mCurrentSignal);
	            
	        	int latitude = (int)(location.getLatitude() * 1E6);
	        	int longitude = (int)(location.getLongitude() * 1E6);
	        	
	        	if(mTrack != null)
	        		mTrack.Add(new GeoPoint(latitude, longitude), location.getSpeed());
	        	
	        	boolean stoppedNow = mFile.IsStopped(mSettings.StoppedThreshold, mSettings.Units);
	        	
	        	if(!mSettings.IsStationary && stoppedNow)
	        	{
	        		mStops.Add(new GeoPoint(latitude, longitude));
	        	}
	        	mSettings.IsStationary = stoppedNow;
	        	
	            LoadPage(location);
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
    			if(mSettings.IsRecording)
    			{
    				StopGPS();
    				
    				mSettings.IsRecording = !mSettings.IsRecording;
    				
    				Toast t = Toast.makeText(mContext, "Stopped GPS", Toast.LENGTH_SHORT);
    				t.show();
    			}
    			else
    			{
    				if(mSettings.Filename != null)
    				{
	    				//Ask whether to continue previous log or start new?
	        			AlertDialog.Builder builder =  new AlertDialog.Builder(mContext);
	        	    	builder.setMessage("Continue last file or start new?");
	        	    	builder.setPositiveButton("Continue", new DialogInterface.OnClickListener()
	        	    	{
	        	            @Override
	        	            public void onClick(DialogInterface dialog, int which)
	        	            {
	        	            	ContinueFile();
	        	            }
	        	        });
	        	    	builder.setNegativeButton("New", new DialogInterface.OnClickListener()
	        	    	{
	        	            @Override
	        	            public void onClick(DialogInterface dialog, int which)
	        	            {
	        	            	StartNewFile();
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
    				else
    				{
    					StartNewFile();
    				}
    			}
    		}
        	catch(Exception e)
        	{
        		ErrorFile.WriteException(e, getApplicationContext());
        	}
    	}
    }
    
    private class SignalListener extends PhoneStateListener
    {
    	@Override
    	public void onSignalStrengthsChanged(SignalStrength signalStrength)
    	{
    		//An excellent dBm value would be -60, very poor (call-dropping) would be -112
    		mCurrentSignal = signalStrength.getCdmaDbm();
    		//String strengths = String.format("%d, %d, %d, %d", signalStrength.getCdmaDbm(), signalStrength.getEvdoDbm(), signalStrength.getEvdoSnr(), signalStrength.getGsmSignalStrength());
    		//ErrorFile.Write("TravelTracker", String.format("Signal strength: %s", strengths), getApplicationContext());
    	}
    }
    
    private void StartNewFile()
    {
    	try
    	{
    		mTrack = new TrackOverlay();
        	mStops = new StopOverlay();
        	
        	String rootDirectory = Environment.getExternalStorageDirectory().getPath() + '/' + mSettings.LogDirectory + "/";
        	
        	File dir = new File(rootDirectory);
        	if(!dir.exists() && !dir.mkdirs())
        	{
        		Toast t = Toast.makeText(getApplicationContext(), "Could not create directory " + rootDirectory, Toast.LENGTH_LONG);
        		t.show();
        	}
        	
        	//Create a new file
        	mSettings.Filename = String.format(rootDirectory + "TravelLog_%s.txt", DateStrings.GetDateTimeString(Calendar.getInstance()));
        	StartGPS();
        	
        	mSettings.IsRecording = !mSettings.IsRecording;
        	
        	Toast t = Toast.makeText(mContext, "Started GPS", Toast.LENGTH_SHORT);
    		t.show();
    	}
    	catch(Exception e)
    	{
    		ErrorFile.WriteException(e, mContext);
    	}
    }
    
    private void ContinueFile()
    {
    	StartGPS();
    	
    	mSettings.IsRecording = !mSettings.IsRecording;
    	
    	Toast t = Toast.makeText(mContext, "Started GPS", Toast.LENGTH_SHORT);
		t.show();
    }
    
    private void StartGPS()
    {
    	try
    	{
	    	if(mFile != null && mFile.Filename != mSettings.Filename)
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
	    	
	    	mFile = new CoordFile(this, mSettings.Filename);
	    	mSettings.Filename = mFile.Filename;
	    	
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
	        mLocMgr.requestLocationUpdates(LocationManager.GPS_PROVIDER, mSettings.LogInterval, 0, mListener);
	        
	        mSignalListener = new SignalListener();
	        
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
    	
    	CloseFile();
    	
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
    	
    	mSettings.Save(getPreferences(MODE_PRIVATE), this);
    	
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

    private int[] BuildColorTable()
    {
    	int[] colors = new int[256];
    	
    	int[] inputs = { Color.RED, Color.YELLOW, Color.GREEN, Color.BLUE };
    	
    	for(int i=0; i<256; i++)
    	{
    		float colorIndex = (float)i / 256 * (inputs.length - 1);
    		int color1 = inputs[(int)colorIndex];
    		int color2 = inputs[(int)colorIndex + 1];
    		
    		colorIndex = colorIndex % 1;
    		
    		int red = (int)((Color.red(color2) * colorIndex) + (Color.red(color1) * (1 - colorIndex)));
    		int green = (int)((Color.green(color2) * colorIndex) + (Color.green(color1) * (1 - colorIndex)));
    		int blue = (int)((Color.blue(color2) * colorIndex) + (Color.blue(color1) * (1 - colorIndex)));
    		
    		colors[i] = Color.rgb(red, green, blue);
    	}
    	
    	return colors;
    }
    
	@Override
	protected boolean isRouteDisplayed()
	{
		return false;
	}
	
	public class TrackOverlay extends Overlay
	{
		private ArrayList<GeoPoint> mPoints = new ArrayList<GeoPoint>();
		private ArrayList<Float> mValues = new ArrayList<Float>();
		
		public TrackOverlay()
		{
		}
		
		public void Add(GeoPoint geoPoint, float value)
		{
			mPoints.add(geoPoint);
			mValues.add(value);
		}
		
		@Override
	    public void draw(Canvas canvas, MapView mapV, boolean shadow)
		{
			try
			{
				float minValue = 1000;
				float maxValue = -1000;
				for(int i=0; i<mValues.size(); i++)
				{
					float curValue = mValues.get(i);
					if(curValue > maxValue)
						maxValue = curValue;
					if(curValue < minValue)
						minValue = curValue;
				}
				float valueRange = maxValue - minValue;
				
				Projection projection = mMapView.getProjection();
				Paint paint = new Paint();
				//paint.setARGB(255, 0, 0, 255);
				paint.setStrokeWidth(3);
				Point lastPoint = null;
				for(int i=0; i<mPoints.size(); i++)
				{
					//Set color based on value
					float percentage = (mValues.get(i) - minValue) / valueRange;
					
					//if(percentage < 0.10)
					//	paint.setARGB(255, 255, 0, 0); //red
					//else if(percentage < 0.25)
					//	paint.setARGB(255, 255, 128, 0); //orange
					//else if(percentage < 0.50)
					//	paint.setARGB(255, 255, 255, 0); //yellow
					//else if(percentage < 0.75)
					//	paint.setARGB(255, 0, 255, 0); //green
					//else
					//	paint.setARGB(255, 0, 0, 255); //blue
					
					int shade = (int)(percentage * 255);
					paint.setColor(mColorTable[shade]);
					//paint.setARGB(255, 0, 0, shade);
					
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