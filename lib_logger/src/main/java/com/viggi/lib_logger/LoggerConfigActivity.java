package com.viggi.lib_logger;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.crashlytics.android.Crashlytics;
import com.example.dave.lib_logger.R;
import com.viggi.lib_file.LogItem;
import com.viggi.lib_file.LoggerConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import io.fabric.sdk.android.Fabric;

public class LoggerConfigActivity extends Activity {

    private LoggerConfig mConfig = null;

    private EditText mRunningAverageText = null;
    private EditText mRolloverHourText = null;
    private CheckBox mSafeCheckbox = null;
    private LinearLayout mItemsList = null;

    private List<LoggerConfigItemView> mItemViews =  new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Crashlytics
        Fabric.with(this, new Crashlytics());

        setContentView(R.layout.activity_logger_config);

        Intent intent = getIntent();
        String configPath = intent.getStringExtra("directory") + "/"+ intent.getStringExtra("configfile");

        mConfig = LoggerConfig.FromFile(configPath);

        Button doneButton = (Button)findViewById(R.id.doneButton);
        doneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveFromUI();
                finish();
            }
        });

        mRunningAverageText = (EditText) findViewById(R.id.runningAverageInput);
        mRunningAverageText.setText(String.format(Locale.getDefault(), "%d", mConfig.AveragingWindow));

        mRolloverHourText = (EditText) findViewById(R.id.rolloverHourInput);
        mRolloverHourText.setText(String.format(Locale.getDefault(), "%d", mConfig.MidnightHour));

        mSafeCheckbox = (CheckBox) findViewById(R.id.safeModeInput);
        mSafeCheckbox.setChecked(mConfig.SafeMode);

        mItemsList = (LinearLayout) findViewById(R.id.itemsList);

        int cellID = 1;
        int buttonsPerRow = 4;
        LinearLayout grid = null;
        LinearLayout row = null;
        int insertIndex = 0;

        for(LogItem item : mConfig.Items)
        {
            try
            {
                View stub = View.inflate(this, R.layout.config_item, null);
                stub.setId(1000 + cellID);
                cellID++;

                final LoggerConfigItemView itemView = new LoggerConfigItemView();

                itemView.nameText = (TextView) stub.findViewById(R.id.itemName);
                itemView.nameText.setText(item.Name);
                itemView.nameText.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        int vis = itemView.optionsLayout.getVisibility();
                        itemView.optionsLayout.setVisibility(vis == View.GONE ? View.VISIBLE : View.GONE);
                    }
                });

                itemView.optionsLayout = (RelativeLayout) stub.findViewById(R.id.itemOptionsLayout);
                itemView.optionsLayout.setVisibility(View.GONE);
                itemView.toggleCheck = (CheckBox) stub.findViewById(R.id.itemToggleCheck);
                itemView.toggleCheck.setChecked(item.IsToggle);
                itemView.safeCheck = (CheckBox) stub.findViewById(R.id.itemSafeCheck);
                itemView.safeCheck.setChecked(item.IsSafe);
                itemView.valueCheck = (CheckBox) stub.findViewById(R.id.itemValueCheck);
                itemView.valueCheck.setChecked(item.IsValue);
                itemView.backupCheck = (CheckBox) stub.findViewById(R.id.itemBackupCheck);
                itemView.backupCheck.setChecked(item.IsBackup);
                itemView.locationCheck = (CheckBox) stub.findViewById(R.id.itemLocationCheck);
                itemView.locationCheck.setChecked(item.IsLocation);

                mItemsList.addView(stub);
                mItemViews.add(itemView);

                insertIndex++;
            }
            catch(NullPointerException e)
            {
                Log.e("Logger", "Null exception while loading config items");
            }

        }
    }

    private void saveFromUI()
    {
        mConfig.AveragingWindow = Integer.parseInt(mRunningAverageText.getText().toString());
        mConfig.MidnightHour = Integer.parseInt(mRolloverHourText.getText().toString());
        mConfig.SafeMode = mSafeCheckbox.isChecked();

        //TODO: Save config
        Toast.makeText(getApplicationContext(), "Changes not being saved yet", Toast.LENGTH_SHORT).show();
    }
}
