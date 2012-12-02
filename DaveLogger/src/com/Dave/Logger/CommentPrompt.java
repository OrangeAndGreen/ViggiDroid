package com.Dave.Logger;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class CommentPrompt extends Dialog
{
	protected OnCommentSetListener onCommentSetListener;
	protected String mComment = null;
	private EditText mEditText = null;
	
	public CommentPrompt(Context context, OnCommentSetListener l)
	{
		super(context);
		this.onCommentSetListener = l;
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.comment);
		
		Button okButton = (Button) findViewById(R.id.commentOkButton);
		okButton.setOnClickListener(okButtonClickListener);

		Button cancelButton = (Button) findViewById(R.id.commentCancelButton);
		cancelButton.setOnClickListener(cancelButtonClickListener);
		
		mEditText = (EditText) findViewById(R.id.commentText);
		mEditText.setText("");
	}
	
	@Override
	public void show()
	{
		mEditText.setText("");
		super.show();
	}
	
	private android.view.View.OnClickListener okButtonClickListener = new android.view.View.OnClickListener() {
		public void onClick(View v)
		{
			Editable text = mEditText.getText();
			mComment = text.toString();
			if (onCommentSetListener!=null)
				onCommentSetListener.onCommentSet(CommentPrompt.this, mComment);
			dismiss();
		}
	};
	
	private android.view.View.OnClickListener cancelButtonClickListener = new android.view.View.OnClickListener() {
		public void onClick(View v) {
			dismiss();
		}
	};
	
	public interface OnCommentSetListener {
		/**
		 * this method is called when a comment was selected by the user
		 * @param view			the caller of the method
		 * 
		 */
		public void onCommentSet(CommentPrompt view, String comment);
	}
}
