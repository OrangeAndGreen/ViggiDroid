package com.Dave.helloandroid;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.TextView;

public class HelloAndroid extends Activity {
	private Button closeButton;
	private Button countButton;
	private Button resetButton;
	private AutoCompleteTextView autoText;
	private int counter1=0;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        countButton = (Button) this.findViewById(R.id.incButton);
        countButton.setOnClickListener(new CountListener());
        
        resetButton = (Button) this.findViewById(R.id.resetButton);
        resetButton.setOnClickListener(new ResetListener());
        
        closeButton = (Button)this.findViewById(R.id.closeButton);
        closeButton.setOnClickListener(new CloseListener());
        
        autoText = (AutoCompleteTextView)this.findViewById(R.id.autotext);
        
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_dropdown_item_1line, COUNTRIES);
        autoText.setAdapter(adapter);
        autoText.setThreshold(0);
    }
    
    private static final String[] COUNTRIES = new String[] {
        "Belgium","Isosceles", "France", "Italy", "Indigo", "Germany", "Spain" };
    
    public String GetDateTimeString(Date curDate)
    {
    	int year = curDate.getYear();
    	int month = curDate.getMonth();
    	int day = curDate.getDate();
    	int hour = curDate.getHours();
    	int minute = curDate.getMinutes();
    	int second = curDate.getSeconds();
    	String dateStr = String.format("%04d%02d%02d_%02d%02d%02d", year+1900, month+1, day, hour, minute, second);
    	return dateStr;
    }
    
    public void AddLogEntry(Date curDate, String curLabel)
    {
    	try
    	{
    	FileWriter fw = new FileWriter("/sdcard/TestLog.txt", true);
    	fw.append(GetDateTimeString(curDate) + " - " + curLabel + "\n");
    	fw.close();
    	}
    	catch(IOException e) {}
    }
    
    public class CountListener implements View.OnClickListener
    {
    	public void onClick(View v) {
            // Perform action on click
        	counter1++;
        	String display = Integer.toString(counter1);
        	TextView tv = (TextView) findViewById(R.id.countText);
        	tv.setText(display);
        	String dateStr = GetDateTimeString(new Date());
        	TextView tv2 = (TextView) findViewById(R.id.dateText);
        	tv2.setText(dateStr);
        	AddLogEntry(new Date(), display);
        }
    }
    
    public class ResetListener implements View.OnClickListener
    {
    	public void onClick(View v) {
            // Perform action on click
        	counter1 = 0;
        	String display = Integer.toString(counter1);
        	TextView tv = (TextView) findViewById(R.id.countText);
        	tv.setText(display);
        }
    }
    
    public class CloseListener implements View.OnClickListener
    {
    	public void onClick(View v) {
            // Perform action on click
        	finish();
        }
    }
}