package com.Dave.Sudoku.Prompt;

import com.Dave.Sudoku.R;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class ChangePasswordPrompt extends Dialog
{
	protected OnSetPasswordListener mListener = null;
	
	private EditText mOldPasswordText = null;
	private EditText mNewPasswordText = null;
	private Button mOKButton = null;
	private String mOldPassword = null;
	
	public ChangePasswordPrompt(Context context, String oldPassword, OnSetPasswordListener listener)
	{
		super(context);
		
		mOldPassword = oldPassword;
				
		mListener = listener;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.changepasswordprompt);
		
		mOKButton = (Button) findViewById(R.id.changeButton);
		mOKButton.setOnClickListener(new OKButtonListener());
		
		mOldPasswordText = (EditText) findViewById(R.id.oldPasswordInput);
		if(mOldPassword != null)
			mOldPasswordText.setText(mOldPassword);

		mNewPasswordText = (EditText) findViewById(R.id.newPasswordInput);
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
				mListener.onPasswordSet(ChangePasswordPrompt.this, mOldPasswordText.getText().toString(), mNewPasswordText.getText().toString());
			dismiss();
		}
		
	}
	
	public interface OnSetPasswordListener
	{
		public void onPasswordSet(ChangePasswordPrompt view, String oldPassword, String newPassword);
		
		public void onCancelled();
	}
}
