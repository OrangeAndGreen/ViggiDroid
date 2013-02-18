package com.Dave.Service;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class Sandbox_ServiceActivity extends Activity
{
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        Intent intent = new Intent(this, SampleService.class);
        startService(intent);
    }
}