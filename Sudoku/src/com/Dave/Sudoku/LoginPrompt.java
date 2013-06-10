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
	private Button mNewButton = null;
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
		
		mOKButton = (Button) findViewById(R.id.nameLogin);
		mOKButton.setOnClickListener(new OKButtonListener());
		
		mNewButton = (Button) findViewById(R.id.nameNew);
		mNewButton.setOnClickListener(new NewButtonListener());
		
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
	
	@Override
	public void onBackPressed()
	{
		if (mListener!=null)
			mListener.onCancelled();
		dismiss();
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

	private class NewButtonListener implements View.OnClickListener
	{		
		public NewButtonListener()
		{
		}

		public void onClick(View v)
		{
			if (mListener!=null)
				mListener.onNewPlayer(LoginPrompt.this, mNameText.getText().toString(), mPasswordText.getText().toString());
			dismiss();
		}
		
	}
	
	public interface OnNameSetListener
	{
		public void onNameSet(LoginPrompt view, String name, String password);
		
		public void onNewPlayer(LoginPrompt view, String name, String password);
		
		public void onCancelled();
	}
}
