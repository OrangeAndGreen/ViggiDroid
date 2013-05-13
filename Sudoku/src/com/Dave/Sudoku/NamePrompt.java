package com.Dave.Sudoku;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class NamePrompt extends Dialog
{
	protected OnNameSetListener mListener = null;
	
	private EditText mNameText = null;
	private Button mOKButton = null;
	private String mDefaultName = null;
	
	public NamePrompt(Context context, String defaultName, OnNameSetListener listener)
	{
		super(context);
		
		mDefaultName = defaultName;
				
		mListener = listener;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.nameprompt);
		
		mOKButton = (Button) findViewById(R.id.nameOk);
		mOKButton.setOnClickListener(new OKButtonListener());
		
		mNameText = (EditText) findViewById(R.id.nameInput);
		if(mDefaultName != null)
			mNameText.setText(mDefaultName);

	}
	
	@Override
	public void show()
	{
		super.show();
	}
	
	private class OKButtonListener implements View.OnClickListener
	{		
		public OKButtonListener()
		{
		}

		public void onClick(View v)
		{
			if (mListener!=null)
				mListener.onNameSet(NamePrompt.this, mNameText.getText().toString());
			dismiss();
		}
		
	}
	
	public interface OnNameSetListener
	{
		/**
		 * this method is called when a name was selected by the user
		 * @param view			the caller of the method
		 * 
		 */
		public void onNameSet(NamePrompt view, String name);
	}
}
