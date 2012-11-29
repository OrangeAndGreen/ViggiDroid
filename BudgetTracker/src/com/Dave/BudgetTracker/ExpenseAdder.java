package com.Dave.BudgetTracker;

import java.util.Calendar;
import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import com.Dave.DateStrings.DateStrings;

public class ExpenseAdder extends Activity
{
	private Button mAddButton = null;
	private Button mCancelButton = null;
	private EditText mDateText = null;
	private EditText mLocationText = null;
	private EditText mAmountText = null;
	private EditText mCommentText = null;
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add);
        
        mAddButton = (Button) findViewById(R.id.addButton);
        mCancelButton = (Button) findViewById(R.id.addCancelButton);
        mDateText = (EditText) findViewById(R.id.dateText);
        mLocationText = (EditText) findViewById(R.id.locationText);
        mAmountText = (EditText) findViewById(R.id.amountText);
        mCommentText = (EditText) findViewById(R.id.commentText);
        
        mAddButton.setOnClickListener(new AddListener());
        mCancelButton.setOnClickListener(new CancelListener());
        
        mDateText.setText(DateStrings.GetDateTimeString(Calendar.getInstance()));
	}
	
	private class AddListener implements View.OnClickListener
	{
		public void onClick(View v)
		{
			Expense curExpense = new Expense(mDateText.getText().toString(), 
														mLocationText.getText().toString(),
														mAmountText.getText().toString(),
														mCommentText.getText().toString());
			setResult(RESULT_OK);
			finish();
		}
	}
	
	private class CancelListener implements View.OnClickListener
    {
    	public void onClick(View v) {
    		setResult(RESULT_CANCELED);
            finish();
        }
    }
}
