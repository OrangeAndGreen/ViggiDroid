package com.Dave.Sudoku;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class LoginPrompt extends Dialog
{
	protected OnNameSetListener mListener = null;
	
	private EditText mNameText = null;
	private EditText mPasswordText = null;
	private Button mOKButton = null;
	private String mDefaultName = null;
	private String mDefaultPassword = null;
	
	public LoginPrompt(Context context, String defaultName, String defaultPassword, OnNameSetListener listener)
	{
		super(context);
		
		mDefaultName = defaultName;
		mDefaultPassword = defaultPassword;
				
		mListener = listener;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.loginprompt);
		
		mOKButton = (Button) findViewById(R.id.nameOk);
		mOKButton.setOnClickListener(new OKButtonListener());
		
		mNameText = (EditText) findViewById(R.id.nameInput);
		if(mDefaultName != null)
			mNameText.setText(mDefaultName);

		mPasswordText = (EditText) findViewById(R.id.passwordInput);
		if(mDefaultPassword != null)
			mPasswordText.setText(mDefaultPassword);
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
				mListener.onNameSet(LoginPrompt.this, mNameText.getText().toString(), mPasswordText.getText().toString());
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
		public void onNameSet(LoginPrompt view, String name, String password);
	}
}
