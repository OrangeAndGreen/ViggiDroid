package com.Dave.Sudoku.Prompt;

import java.util.List;

import com.Dave.Sudoku.R;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class NumberPrompt extends Dialog
{
	protected OnNumberSetListener mListener = null;
	
	public List<Byte> Options = null;
	
	private Button mButtonOne = null;
	private Button mButtonTwo = null;
	private Button mButtonThree = null;
	private Button mButtonFour = null;
	private Button mButtonFive = null;
	private Button mButtonSix = null;
	private Button mButtonSeven = null;
	private Button mButtonEight = null;
	private Button mButtonNine = null;
	private Button mButtonCancel = null;
	
	public NumberPrompt(Context context, OnNumberSetListener listener, List<Byte> options)
	{
		super(context);
		mListener = listener;
		Options = options;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.numberprompt);
		
		mButtonOne = (Button) findViewById(R.id.numberOne);
		mButtonOne.setOnClickListener(new NumberButtonListener(1));
		
		mButtonTwo = (Button) findViewById(R.id.numberTwo);
		mButtonTwo.setOnClickListener(new NumberButtonListener(2));
		
		mButtonThree = (Button) findViewById(R.id.numberThree);
		mButtonThree.setOnClickListener(new NumberButtonListener(3));
		
		mButtonFour = (Button) findViewById(R.id.numberFour);
		mButtonFour.setOnClickListener(new NumberButtonListener(4));
		
		mButtonFive = (Button) findViewById(R.id.numberFive);
		mButtonFive.setOnClickListener(new NumberButtonListener(5));
		
		mButtonSix = (Button) findViewById(R.id.numberSix);
		mButtonSix.setOnClickListener(new NumberButtonListener(6));
		
		mButtonSeven = (Button) findViewById(R.id.numberSeven);
		mButtonSeven.setOnClickListener(new NumberButtonListener(7));
		
		mButtonEight = (Button) findViewById(R.id.numberEight);
		mButtonEight.setOnClickListener(new NumberButtonListener(8));
		
		mButtonNine = (Button) findViewById(R.id.numberNine);
		mButtonNine.setOnClickListener(new NumberButtonListener(9));
		
		mButtonCancel = (Button) findViewById(R.id.numberCancel);
		mButtonCancel.setOnClickListener(new NumberButtonListener(0));
		
		SetOptions(Options);
	}
	
	@Override
	public void show()
	{
		super.show();
	}

	public void SetOptions(List<Byte> options)
	{
		Options = options;
		
		if(mButtonOne == null)
			return;
		
		mButtonOne.setEnabled(Options.contains((byte)1));
		mButtonTwo.setEnabled(Options.contains((byte)2));
		mButtonThree.setEnabled(Options.contains((byte)3));
		mButtonFour.setEnabled(Options.contains((byte)4));
		mButtonFive.setEnabled(Options.contains((byte)5));
		mButtonSix.setEnabled(Options.contains((byte)6));
		mButtonSeven.setEnabled(Options.contains((byte)7));
		mButtonEight.setEnabled(Options.contains((byte)8));
		mButtonNine.setEnabled(Options.contains((byte)9));
	}
	
	private class NumberButtonListener implements View.OnClickListener
	{
		private int mNumber = -1;
		
		public NumberButtonListener(int number)
		{
			mNumber = number;
		}

		public void onClick(View v)
		{
			if (mListener!=null)
				mListener.onNumberSet(NumberPrompt.this, mNumber);
			dismiss();
		}
		
	}
	
	public interface OnNumberSetListener
	{
		/**
		 * this method is called when a comment was selected by the user
		 * @param view			the caller of the method
		 * 
		 */
		public void onNumberSet(NumberPrompt view, int number);
	}

}