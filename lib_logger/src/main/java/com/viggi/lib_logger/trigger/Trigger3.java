package com.viggi.lib_logger.trigger;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/**
 * Created by Dave on 2/24/2015.
 */
public class Trigger3 extends Activity
{
    private final int mTriggerNumber = 3;

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
