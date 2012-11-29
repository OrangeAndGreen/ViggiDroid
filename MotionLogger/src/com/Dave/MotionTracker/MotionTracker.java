package com.Dave.MotionTracker;

import java.util.Calendar;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

/*
 * ToDo:
 * -Figure out sample rate
 * -Nail down gravity compensation
 * -Plot sweep charts of x, y, z, total
 * -Polar plot of current acceleration (and recent history)
 * 
 * 
 */

public class MotionTracker extends Activity implements Runnable, SensorEventListener
{
	//Timed update thread
    private Thread mUpdateThread = null;
    private boolean mQuitThread = false;
    
    //GUI References
    private Button mCloseButton = null;
    private TextView mAccelText = null;
    
    //Hardware References
    private SensorManager mSensorManager = null;
    private Sensor mAccelerometer = null;
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        //Grab GUI elements
        mCloseButton = (Button) findViewById(R.id.closeButton);
        mCloseButton.setOnClickListener(new CloseListener());
        mAccelText = (TextView) findViewById(R.id.accelText);
        
        //Grab hardware references
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        
        mUpdateThread = new Thread(this);
    	mUpdateThread.start();
    }
    
    private float[] gravity = new float[3];
    private Calendar lastUpdate = Calendar.getInstance();
    
    public void onSensorChanged(SensorEvent event)
    {
    	synchronized(this)
    	{
    		if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
    		{
    			Calendar date = Calendar.getInstance();
    			//Update accelerometer info
    			float[] linear = new float[3];
    			float alpha = 0.8f;

    			long elapsed = date.getTimeInMillis() - lastUpdate.getTimeInMillis();
    			lastUpdate = date;
    			
    	        gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
    	        gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
    	        gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];

    	        linear[0] = event.values[0] - gravity[0];
    	        linear[1] = event.values[1] - gravity[1];
    	        linear[2] = event.values[2] - gravity[2];
    			
    			String text = String.format("%f, %f, %f\n%d ms", linear[0], linear[1], linear[2], elapsed);
    			mAccelText.setText(text);
    		}
    	}
    }
    
    public void onAccuracyChanged(Sensor sensor, int val)
    {
    	
    }
    
    public void run()
    {
    	while(!mQuitThread)
    	{
        	handler.sendEmptyMessage(0);
        	try { Thread.sleep(1000); }
        		catch (Exception e) {}
    	}
    }
    
    private Handler handler = new Handler()
    {
        @Override
        public void handleMessage(Message msg)
        {
        	//Perform GUI update
        }
    };
    
    private class CloseListener implements View.OnClickListener
    {
    	public void onClick(View v)
    	{
        	finish();
        }
    }

    @Override
    protected void onStop()
    {
    	mSensorManager.unregisterListener(this);
    	super.onStop();
    }


}