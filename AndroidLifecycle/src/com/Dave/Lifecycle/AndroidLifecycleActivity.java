package com.Dave.Lifecycle;

import java.util.Calendar;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;

/*
 * 
 * The purpose of this app is to illustrate the Android lifecycle
 * 
 * Override all of the lifecycle related methods and log them
 * On the main display, show the log and update it when appropriate
 * 
 * Menu options to refresh the log manually or reset it
 * 
 */


public class AndroidLifecycleActivity extends Activity
{
	private TextView mStatus = null;
	private String mReportFile = "/sdcard/Life.txt";
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        mStatus = (TextView) findViewById(R.id.statusText);
        
        DLog.Write(this, mReportFile, Calendar.getInstance(), "onCreate");
        
        mStatus.setText(DLog.Read(this, mReportFile));
    }
    
    @Override
    public void onStart()
    {
    	super.onStart();
    	DLog.Write(this, mReportFile, Calendar.getInstance(), "onStart");
    	
    	mStatus.setText(DLog.Read(this, mReportFile));
    }
    
    @Override
    public void onResume()
    {
    	super.onResume();
    	DLog.Write(this, mReportFile, Calendar.getInstance(), "onResume");
    	
    	mStatus.setText(DLog.Read(this, mReportFile));
    }
    
    @Override
    public void onRestart()
    {
    	super.onRestart();
    	DLog.Write(this, mReportFile, Calendar.getInstance(), "onRestart");
    	
    	mStatus.setText(DLog.Read(this, mReportFile));
    }
    
    
    @Override
    protected void onSaveInstanceState (Bundle outState)
    {
    	//if(mFile != null)
    	//	outState.putString("filename", mFile.Filename);
    	super.onSaveInstanceState(outState);
    }
    
    @Override
    public void onPause()
    {
    	DLog.Write(this, mReportFile, Calendar.getInstance(), "onPause");
    	super.onPause();
    }
    
    @Override
    public void onStop()
    {
    	DLog.Write(this, mReportFile, Calendar.getInstance(), "onStop");
    	super.onStop();
    }
    
    @Override
    public void onDestroy()
    {
    	DLog.Write(this, mReportFile, Calendar.getInstance(), "onDestroy");
    	super.onDestroy();
    }

    
  //Main-menu handling
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle item selection
        switch (item.getItemId())
        {
        case R.id.mainmenu_refresh:
        	mStatus.setText(DLog.Read(this, mReportFile));
        	return true;
        case R.id.mainmenu_reset:
        	DLog.Reset(getApplicationContext(), mReportFile);
        	mStatus.setText(DLog.Read(this, mReportFile));
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

}