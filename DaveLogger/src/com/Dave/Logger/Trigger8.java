package com.Dave.Logger;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class Trigger8 extends Activity
{
	private final int mTriggerNumber = 8;
	
	@Override
    public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
        Intent i = new Intent(getApplicationContext(), TriggerActivity.class);
    	i.putExtra("trigger", mTriggerNumber);
    	startActivity(i);
    	
    	finish();
	}
}
