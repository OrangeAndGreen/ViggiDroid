package com.Dave.GasLogger;

import java.util.Calendar;
import java.util.Hashtable;
import java.util.List;
import java.util.Map.Entry;

import com.Dave.DateStrings.DateStrings;
import com.Dave.Graph.GraphView;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemSelectedListener;

/*
 * TODO:
 * -Implement graphs
 * 
 * New DB table for Stations:
 *	Start with a pre-defined list, then update when importing
 * 
 * -Settings:
 * 		-Edit/delete data
 * 		-Export/email data
 * 		-Miles or km
 * 		-Edit Locations
 * -Enforce numeric rules for numeric inputs
 * -Add some title labels to the top of screens
 * -Add ability to store GPS coord for location
 * -Error-checking when adding a new fillup
 * 
 */

public class GasLoggerActivity extends Activity
{
	private static String HomePage = "Home";
	private static String LowFuelPage = "LowFuel";
	private static String FuelPage = "Fuel";
	private static String ViewPage = "View";
	private static String SettingsPage = "Settings";
	
	
	public static DBAdapter DB = null;
	public static String[] ViewTypes = { "All entries", "Recent info" , "Mileage Graph", "Cost Graph", "Gallons Graph" };
	public static String[] Octanes = { "87", "89", "91", "93" };
	public static String[] Indicators = { "< Empty", "Empty", "> Empty", "< 1/4", "1/4", "> 1/4", "< 1/2", "1/2", "> 1/2", "< 3/4", "3/4", "> 3/4", "< Full", "Full" };
	public static String[] Stations = { "BP", "Chevron", "Citgo", "Exxon", "Hess", "Mobil", "Pilot", "RaceTrac/Raceway", "Shell", "7-11", "Speedway", "Sunoco", "Valero" };
	public static String[] States = { "AK – Alaska", "AL – Alabama", "AR – Arkansas", "AZ – Arizona", "CA – California", "CO – Colorado", "CT – Connecticut", "DE – Delaware", 
		                              "DC – District of Columbia", "FL – Florida", "GA – Georgia", "HI – Hawaii", "IA – Iowa", "ID – Idaho", "IL – Illinois", "IN – Indiana", 
		                              "KS – Kansas", "KY – Kentucky", "LA – Louisiana", "MA – Massachusetts", "MD – Maryland", "ME – Maine", "MI – Michigan", "MN – Minnesota",
		                              "MS – Mississippi", "MO – Missouri", "MT – Montana", "NC – North Carolina", "ND – North Dakota", "NE – Nebraska", "NH – New Hampshire",
		                              "NJ – New Jersey", "NM – New Mexico", "NV – Nevada", "NY – New York", "OH – Ohio", "OK – Oklahoma", "OR – Oregon", "PA – Pennsylvania",
		                              "RI – Rhode Island", "SC – South Carolina", "SD – South Dakota", "TN – Tennessee", "TX – Texas", "UT – Utah", "VA – Virginia", "VT – Vermont",
		                              "WA – Washington", "WI – Wisconsin", "WV – West Virginia", "WY – Wyoming"}; 
	private String mCurrentPage = null;
	
	//Select screen GUI controls
	private Button mSelectQuitButton = null;
	private Button mSelectLowFuelButton = null;
	private Button mSelectFuelButton = null;
	private Button mSelectViewButton = null;
	private Button mSelectSettingsButton = null;
	
	//Low fuel screen GUI controls
	private Button mLFBackButton = null;
	private Button mLFQuitButton = null;
	private EditText mLFLowMileageText = null;
	private Button mLFOKButton = null;
	
	//Fuel screen GUI controls
	private Button mFuelBackButton = null;
	private Button mFuelQuitButton = null;
	private Button mFuelOKButton = null;
	private EditText mDateText = null;
	private EditText mMileageText = null;
	private Spinner mOctaneSpinner = null;
	private EditText mGallonsText = null;
	private EditText mCostPerGallonText = null;
	private Spinner mIndicatorSpinner = null;
	private Spinner mStationSpinner = null;
	private EditText mCarText = null;
	private Spinner mStateSpinner = null;
	private EditText mLocationText = null;
	private EditText mMilesToEmpty = null;
	private CheckBox mReceipt = null;
	private EditText mLowMileageText = null;
	private EditText mLowDaysText = null;
	
	//View screen GUI controls
	private Button mViewBackButton = null;
	private Button mViewQuitButton = null;
	private Spinner mViewSelector = null;
	private TextView mViewText = null;
	private GraphView mGraph = null;
	
	//Settings screen GUI controls
	private Button mSettingsBackButton = null;
	private Button mSettingsQuitButton = null;
	//private Button mSettingsEditButton = null;
	private Button mSettingsImportButton = null;
	private Button mSettingsExportButton = null;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        String loadPage = HomePage;
        if(savedInstanceState != null)
        {
        	loadPage = savedInstanceState.getString("page");
        }
        
        DB = new DBAdapter(this, getResources());
        DB.Open();
        
        if(loadPage.equals(HomePage))
        	LoadSelectScreen();
        else if(loadPage.equals(LowFuelPage))
        	LoadLowFuelScreen();
        else if(loadPage.equals(FuelPage))
        	LoadFuelScreen();
        else if(loadPage.equals(ViewPage))
        	LoadViewScreen();
        else if(loadPage.equals(SettingsPage))
        	LoadSettingsScreen();
    }
    
    @Override
    protected void onSaveInstanceState (Bundle outState)
    {
    	outState.putString("page", mCurrentPage);
    	super.onSaveInstanceState(outState);
    }

    
    private void LoadSelectScreen()
    {
    	mCurrentPage = HomePage;
    	setContentView(R.layout.select);
    	
    	try
    	{
    		//if(mSelectLowFuelButton == null)
    		{
    			mSelectLowFuelButton = (Button) findViewById(R.id.button_lowfuel);
    			mSelectLowFuelButton.setOnClickListener(new MenuButtonListener("LowFuel"));
    		}
        
    		//if(mSelectFuelButton == null)
    		{
    			mSelectFuelButton = (Button) findViewById(R.id.button_gas);
    			mSelectFuelButton.setOnClickListener(new MenuButtonListener("Fuel"));
    		}
    	
    		//if(mSelectViewButton == null)
    		{
    			mSelectViewButton = (Button) findViewById(R.id.button_view);
    			mSelectViewButton.setOnClickListener(new MenuButtonListener("View"));
    		}
    	
    		mSelectSettingsButton = (Button) findViewById(R.id.button_settings);
			mSelectSettingsButton.setOnClickListener(new MenuButtonListener("Settings"));
    		
    		//if(mSelectQuitButton == null)
    		{
    			mSelectQuitButton = (Button) findViewById(R.id.button_quit_select);
    			mSelectQuitButton.setOnClickListener(new QuitButtonListener());
    		}
    	}
    	catch(Exception e)
    	{
    		
    	}
    }
    
    private void LoadLowFuelScreen()
    {
    	mCurrentPage = LowFuelPage;
    	setContentView(R.layout.lowfuel);
        
    	try
    	{
    		mLFBackButton = (Button) findViewById(R.id.button_back_lf);
    		mLFBackButton.setOnClickListener(new BackButtonListener());
    	
    		mLFQuitButton = (Button) findViewById(R.id.button_quit_lf);
    		mLFQuitButton.setOnClickListener(new QuitButtonListener());
        
    		mLFOKButton = (Button) findViewById(R.id.button_ok_lf);
    		mLFOKButton.setOnClickListener(new OKButtonListener("LowFuel"));
    		
    		mLFLowMileageText = (EditText) findViewById(R.id.lowfuel_edit_lf);
    		
    		if(mLFLowMileageText != null && mLFLowMileageText.getText().toString().trim().length() == 0)
    		{
    			//Pre-load the control
    			String lowMileage = DB.FindRecentValue(DBAdapter.RECENT_LOW_MILE);
				
    			if(lowMileage != null)
    				mLFLowMileageText.setText(lowMileage);
    			else
    			{
    				Toast t = Toast.makeText(getApplicationContext(), "Couldn't pre-load", Toast.LENGTH_LONG);
    				t.show();
    			}
    		}
    	}
    	catch(Exception e)
    	{
    		
    	}
    }
    
    private void LoadFuelScreen()
    {
    	mCurrentPage = FuelPage;
    	setContentView(R.layout.fuel);
        
    	//try
    	//{
    	
    		//if(mFuelBackButton == null)
    		{
    			mFuelBackButton = (Button) findViewById(R.id.button_back_fuel);
    			mFuelBackButton.setOnClickListener(new BackButtonListener());
    		}
    	
    		//if(mFuelQuitButton == null)
    		{
    			mFuelQuitButton = (Button) findViewById(R.id.button_quit_fuel);
    			mFuelQuitButton.setOnClickListener(new QuitButtonListener());
    		}
        
    		//if(mFuelOKButton == null)
    		{
    			mFuelOKButton = (Button) findViewById(R.id.button_ok_fuel);
    			mFuelOKButton.setOnClickListener(new OKButtonListener("Fuel"));
    		}
    		
    		mDateText = (EditText) findViewById(R.id.date);
    		mMileageText = (EditText) findViewById(R.id.mileage);
    		
    		mOctaneSpinner = (Spinner) findViewById(R.id.octane);
    		ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(this, android.R.layout.simple_spinner_item, Octanes);
			adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			mOctaneSpinner.setAdapter(adapter);
    		
    		mGallonsText = (EditText) findViewById(R.id.gallons);
    		mCostPerGallonText = (EditText) findViewById(R.id.costpergallon);
    		
    		mIndicatorSpinner = (Spinner) findViewById(R.id.indicator);
    		adapter = new ArrayAdapter<CharSequence>(this, android.R.layout.simple_spinner_item, Indicators);
			adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			mIndicatorSpinner.setAdapter(adapter);
			
			mStationSpinner = (Spinner) findViewById(R.id.station);
    		adapter = new ArrayAdapter<CharSequence>(this, android.R.layout.simple_spinner_item, Stations);
			adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			mStationSpinner.setAdapter(adapter);
			
			mCarText = (EditText) findViewById(R.id.car);
			
			mStateSpinner = (Spinner) findViewById(R.id.state);
    		adapter = new ArrayAdapter<CharSequence>(this, android.R.layout.simple_spinner_item, States);
			adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			mStateSpinner.setAdapter(adapter);						
    		
    		mLocationText = (EditText) findViewById(R.id.location);
    		mMilesToEmpty = (EditText) findViewById(R.id.mte);
    		mReceipt = (CheckBox) findViewById(R.id.receipt);
    		mLowMileageText = (EditText) findViewById(R.id.lowmileage);
    		mLowDaysText = (EditText) findViewById(R.id.lowdays);
    		
    		PreloadFuelControls();
    	//}
    	//catch(Exception e)
    	//{
    	//	Toast t = Toast.makeText(getApplicationContext(), "Failed to load fuel screen", Toast.LENGTH_SHORT);
    	//	t.show();
    	//}
    }
    
    private void PreloadFuelControls()
    {
    	//If the controls exist
    	if(mMileageText != null)
    	{
    		String text = mMileageText.getText().toString().trim();
    		
    		//If the controls haven't been filled out yet
    		if(text.length() == 0)
    		{
    			mDateText.setText(DateStrings.GetDateTimeString(Calendar.getInstance()));
    			mIndicatorSpinner.setSelection(0);
    			mLowDaysText.setText("-1");
    			
    			//Pre-load with the last entry
    			String date = DB.FindRecentValue(DBAdapter.RECENT_DATE);
    			
    			//If a previous entry was found
    			if(date != null)
    			{
    				//Update values from last entry
    				Fillup fillup = DB.RetrieveFillup(DBAdapter.IDFromDate(date));
    				if(fillup != null)
    				{
    					mMileageText.setText(String.format("%d", fillup.Mileage));
    					
    					String octaneString = String.format("%d", fillup.Octane);
    					for(int i=0; i<Octanes.length; i++)
    						if(Octanes[i].equals(octaneString))
    							mOctaneSpinner.setSelection(i);
    					
    					for(int i=0; i<Stations.length; i++)
    						if(Stations[i].equals(fillup.Station))
    							mStationSpinner.setSelection(i);
    					
    					mGallonsText.setText(String.format("%.03f", fillup.Gallons));
    					mCostPerGallonText.setText(String.format("%.03f", fillup.CostPerGallon));
    					mLocationText.setText(fillup.Location);
    					mLowMileageText.setText(String.format("%d", fillup.LowMileage));
    				}
    			}

    			//Now see if the lowDate was updated more recently than the last fillup
    			String lowDate = DB.FindRecentValue(DBAdapter.RECENT_LOW_DATE);
    			if(DBAdapter.IDFromDate(lowDate) > DBAdapter.IDFromDate(date))
    			{
    				//Use this mileage for the low fuel mileage
    				String lowMileage = DB.FindRecentValue(DBAdapter.RECENT_LOW_MILE);
    				
    				if(lowMileage != null)
    				{
    					mLowMileageText.setText(lowMileage);
    					//DB.UpdateRecentValue(DBAdapter.RECENT_LOW_DATE, DateStrings.GetDateTimeString(Calendar.getInstance()));
    					Calendar lowTime = DateStrings.ParseDateTimeString(lowDate);
    					Log.e("GasLogger", String.format("Lowdate: %s", lowDate));
    					int days = DateStrings.GetActiveDiffInDays(lowTime, Calendar.getInstance(), 0);
    					mLowDaysText.setText(String.format("%s", days));
    				}
    			}
    		}
    	}
    }
    
    private void LoadViewScreen()
    {
    	mCurrentPage = ViewPage;
    	setContentView(R.layout.viewer);
        
    	try
    	{
    		//if(mViewBackButton == null)
    		{
    			mViewBackButton = (Button) findViewById(R.id.button_back_view);
    			mViewBackButton.setOnClickListener(new BackButtonListener());
    		}
    	
    		//if(mViewQuitButton == null)
    		{
    			mViewQuitButton = (Button) findViewById(R.id.button_quit_view);
    			mViewQuitButton.setOnClickListener(new QuitButtonListener());
    		}
        
    		//if(mViewSelector == null)
    		{
    			mViewSelector = (Spinner) findViewById(R.id.viewSelector);
    			
    			ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(this, android.R.layout.simple_spinner_item, ViewTypes);
    			adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    			mViewSelector.setAdapter(adapter);
    			
    			mViewSelector.setOnItemSelectedListener(new ViewSelectedListener());
    		}
    		
    		//if(mViewText == null)
    		{
    			mViewText = (TextView) findViewById(R.id.viewText);
    		}
    		
    		mGraph = (com.Dave.Graph.GraphView) findViewById(R.id.graphView);
    		
    		UpdateViewScreen();
    	}
    	catch(Exception e)
    	{
    		Toast t = Toast.makeText(getApplicationContext(), "Failed to load view screen", Toast.LENGTH_SHORT);
    		t.show();
    	}
    }
    
    private void UpdateViewScreen()
    {
    	if(mViewSelector != null && mViewText != null)
    	{
    		String displayText = "";
    		
    		int typeIndex = mViewSelector.getSelectedItemPosition();
    		String type = ViewTypes[typeIndex];
    		
    		if(type == "All entries")
    		{
    			mGraph.setVisibility(View.GONE);
    			mViewText.setVisibility(View.VISIBLE);
    			
    			List<Fillup> fillups = DB.GetAllFillups(false);
    			
    			for(int i=0; i<fillups.size(); i++)
    			{
    				Fillup fillup = fillups.get(i);
    				displayText += String.format("%s: m%d, %.03fg @ $%.03f/g\n", fillup.Date, fillup.Mileage, fillup.Gallons, fillup.CostPerGallon);
    			}
    		}
    		else if(type == "Recent info")
    		{
    			mGraph.setVisibility(View.GONE);
    			mViewText.setVisibility(View.VISIBLE);
    			
    			Hashtable<String,String> recents = DB.GetAllRecents();
    			for(Entry<String,String> entry : recents.entrySet())
    			{
    				displayText += String.format("%s: %s\n", entry.getKey(), entry.getValue());
    			}
    		}
    		else if (type == "Mileage Graph")
    		{
    			DrawGraph("Mileage");
    		}
    		else if (type == "Cost Graph")
    		{
    			DrawGraph("CostPerGallon");
    		}
    		else if (type == "Gallons Graph")
    		{
    			DrawGraph("Gallons");
    		}
    		else
    			displayText = type;
    		
    		mViewText.setText(displayText);
    	}
    }
    
    private void DrawGraph(String type)
    {
    	//Set the graph visible and text invisible
		mGraph.setVisibility(View.VISIBLE);
		mViewText.setVisibility(View.GONE);

		//Extract the data specified by "category"
		List<Fillup> fillups = DB.GetAllFillups(true);
		
		Calendar firstDate = DateStrings.ParseDateTimeString(fillups.get(0).Date);
		
		float[] datenums = new float[fillups.size()];
		float[] data = new float[fillups.size()];
		for(int i=0; i<fillups.size(); i++)
		{
			Calendar date = DateStrings.ParseDateTimeString(fillups.get(i).Date);
			datenums[i] = (date.getTimeInMillis() - firstDate.getTimeInMillis()) / 86400000;
			
			if(type == "Mileage")
				data[i] = fillups.get(i).Mileage;
			else if(type == "CostPerGallon")
				data[i] = fillups.get(i).CostPerGallon;
			else if(type == "Gallons")
				data[i] = fillups.get(i).Gallons;
		}

		mGraph.EasyGraph(data);
		
		//Setup the all-data plot
		mGraph.Plots.get(0).SetColor(Color.WHITE);
		mGraph.Plots.get(0).DrawPoints = true;
		mGraph.Plots.get(0).PointColor = Color.YELLOW;

		//Setup the title
		mGraph.Title.Text = "";
		
		//Add the weekend shading and start-of-month indicators
		//mGraph.AddDateInfo(startDate);

		//Turn off labels for the bottom axis since they are drawn with the date info
		mGraph.BottomAxis.DrawLabels = false;
		
		mGraph.invalidate();

    }
    
    private void LoadSettingsScreen()
    {
    	mCurrentPage = SettingsPage;
    	setContentView(R.layout.settings);
    	
    	mSettingsBackButton = (Button) findViewById(R.id.button_back_settings);
		mSettingsBackButton.setOnClickListener(new BackButtonListener());
		
		mSettingsQuitButton = (Button) findViewById(R.id.button_quit_settings);
		mSettingsQuitButton.setOnClickListener(new QuitButtonListener());
		
		mSettingsImportButton = (Button) findViewById(R.id.button_import);
		mSettingsImportButton.setOnClickListener(new SettingsButtonListener("Import"));
		
		mSettingsExportButton = (Button) findViewById(R.id.button_export);
		mSettingsExportButton.setOnClickListener(new SettingsButtonListener("Export"));
    }
    
    private class MenuButtonListener implements View.OnClickListener
    {
    	private String mMenu = null;
    	
    	public MenuButtonListener(String menu)
    	{
    		mMenu = menu;
    	}
    	
    	public void onClick(View v)
    	{
    		if(mMenu=="LowFuel")
    		{
    			LoadLowFuelScreen();
    		}
    		else if(mMenu == "Fuel")
    		{
    			LoadFuelScreen();
    		}
    		else if(mMenu == "View")
    		{
    			LoadViewScreen();
    		}
    		else if(mMenu == "Settings")
    		{
    			LoadSettingsScreen();
    		}
    	}
    }
    
    private class OKButtonListener implements View.OnClickListener
    {
    	private String mType = null;
    	
    	public OKButtonListener(String type)
    	{
    		mType = type;
    	}
    	
    	public void onClick(View v)
    	{
    		if(mType=="LowFuel")
    		{
    			try
    			{
    				int mileage = Integer.parseInt(mLFLowMileageText.getText().toString());
    				DB.UpdateRecentValue(DBAdapter.RECENT_LOW_DATE, DateStrings.GetDateTimeString(Calendar.getInstance()));
    				DB.UpdateRecentValue(DBAdapter.RECENT_LOW_MILE, String.format("%d", mileage));
        			Toast t = Toast.makeText(getApplicationContext(), "Updated low fuel info", Toast.LENGTH_LONG);
        			t.show();
    			}
    			catch(Exception e)
    			{
    				//Error persisting low fuel info
    				Toast t = Toast.makeText(getApplicationContext(), "Error persisting low fuel info", Toast.LENGTH_LONG);
    				t.show();
    			}
    			
    			LoadSelectScreen();
    		}
    		else if(mType == "Fuel")
    		{
    			//Build a Fillup from the inputs
    			Fillup fillup = new Fillup();
    			try
    			{
    				fillup.Date = "";
    				fillup.Date = mDateText.getText().toString();
    			}
    			catch(Exception e) {}
    			
    			try
    			{
    				fillup.Mileage = Integer.parseInt(mMileageText.getText().toString());
    			}
    			catch(Exception e) {}
    			
    			try
    			{
    				fillup.Octane = Integer.parseInt(Octanes[mOctaneSpinner.getSelectedItemPosition()]);
    			}
    			catch(Exception e) {}
    			
    			try
    			{
    				fillup.Gallons = Float.parseFloat(mGallonsText.getText().toString());
    			}
    			catch(Exception e) {}
    			
    			try
    			{
    				fillup.CostPerGallon = Float.parseFloat(mCostPerGallonText.getText().toString());
    			}
    			catch(Exception e) {}
    			
    			try
    			{
    				fillup.Indicator = "";
    				fillup.Indicator = Indicators[mIndicatorSpinner.getSelectedItemPosition()];
    			}
    			catch(Exception e) {}
    			
    			try
    			{
    				fillup.Station = "";
    				fillup.Station = Stations[mStationSpinner.getSelectedItemPosition()];
    			}
    			catch(Exception e) {}
    			
    			try
    			{
    				fillup.Location = "";
    				fillup.Location = mLocationText.getText().toString();
    			}
    			catch(Exception e) {}
    			
    			try
    			{
    				fillup.LowMileage = Integer.parseInt(mLowMileageText.getText().toString());
    			}
    			catch(Exception e) {}
    			
    			try
    			{
    				fillup.LowDays = Float.parseFloat(mLowDaysText.getText().toString());
    			}
    			catch(Exception e) {}
    			
    			
    			if(DB.AddFillup(fillup))
    			{
    				Toast t = Toast.makeText(getApplicationContext(), "Added entry to DB", Toast.LENGTH_LONG);
    				t.show();
    			}
    			else
    			{
    				Toast t = Toast.makeText(getApplicationContext(), "Adding to DB failed", Toast.LENGTH_LONG);
    				t.show();
    			}
    			
    			try
    			{
    				int mileage = Integer.parseInt(mLowMileageText.getText().toString());
    				DB.UpdateRecentValue(DBAdapter.RECENT_DATE, fillup.Date);
    				DB.UpdateRecentValue(DBAdapter.RECENT_LOW_DATE, fillup.Date);
    				DB.UpdateRecentValue(DBAdapter.RECENT_LOW_MILE, String.format("%d", mileage));
    			}
    			catch(Exception e)
    			{
    				//Error persisting low fuel info
    				//Toast t = Toast.makeText(getApplicationContext(), "Error persisting low fuel info", Toast.LENGTH_LONG);
    				//t.show();
    			}
    			
    			LoadSelectScreen();
    		}
    	}
    }
    
    private class BackButtonListener implements View.OnClickListener
    {
    	public void onClick(View v)
    	{
    		LoadSelectScreen();
    	}
    }
    
    private class QuitButtonListener implements View.OnClickListener
    {    	
    	public void onClick(View v)
    	{
    		if(DB != null)
    			DB.Close();
    		
    		finish();
    	}
    }
    
    public class ViewSelectedListener implements OnItemSelectedListener
    {
        public ViewSelectedListener()
        {
        }

        public void onItemSelected(AdapterView<?> parent, View v, int pos, long row)
        {
        	UpdateViewScreen();
        }

        public void onNothingSelected(AdapterView<?> parent)
        {
            // do nothing
        }
    }

    private class SettingsButtonListener implements View.OnClickListener
    {
    	private String mSetting = null;
    	
    	public SettingsButtonListener(String setting)
    	{
    		mSetting = setting;
    	}
    	
    	public void onClick(View v)
    	{
    		if(mSetting == "Import")
    		{
    			LogFile.ImportToDB(DB, getApplicationContext());
    		}
    		else if(mSetting == "Export")
    		{
    			LogFile.ExportFromDB(DB);
    		}
    	}
    }
    
    
    
    
}