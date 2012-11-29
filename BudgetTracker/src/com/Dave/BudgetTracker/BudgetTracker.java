package com.Dave.BudgetTracker;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

/*
 * ToDo:
 * -Pass added transaction back to main interface to save log
 * -Auto-fill Date with current date
 * 		-Later: Determine how to reuse DateStrings class from DaveLogger
 * -Income button and interface
 * -
 */

public class BudgetTracker extends Activity {

	public static String LogFilename = "/sdcard/BudgetTracker.txt";
	private Button mAddButton = null;
	private Button mCloseButton = null;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        mAddButton = (Button) findViewById(R.id.addExpenseButton);
        mCloseButton = (Button) findViewById(R.id.endButton);
        
        mAddButton.setOnClickListener(new AddListener());
        mCloseButton.setOnClickListener(new CloseListener());
    }
    
    public class AddListener implements View.OnClickListener
    {
    	public void onClick(View v)
    	{
    		Intent i = new Intent(BudgetTracker.this, ExpenseAdder.class);
            startActivity(i);
    	}
    }
    
    public class CloseListener implements View.OnClickListener
    {
    	public void onClick(View v)
    	{
    		finish();
    	}
    }
}