package com.Dave.Sudoku;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Point;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.TextView;


/*
 * Plans:
 * -Game generator
 * -Intro menu
 * 		-Start new game
 * 		-Load existing game
 * 		-Place to go when game is completed
 * -Menu options:
 * 		-Reset board
 * 		-Export image
 * 		-Save game
 * -2 Player mode:
 * 		-Get player names
 * 		-Show summary above or below the actual game
 * 
 * -Link button prompt to numeric keys
 * -Start learning about how to connect in advertisements
 * 
 * Next:
 * -Allow 2-player to see move before committing it
 * -Allow 1-player to clear a cell
 * 
 * 
 */

public class SudokuActivity extends Activity
{
	private ISudokuGame mGame = null;
	
	private Context mContext = null;
	private TextView mGameText = null;
	private TextView mGameScore = null;
	private SudokuView mSudoku = null;
	private Point mCurrentPoint = null;
	private boolean[] mCellOptions = null;
	private NumberPrompt mPrompt = null;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        
        mContext = this;
        
        LoadMainMenu();
    }
    
    @Override
    protected Dialog onCreateDialog(int id)
    {
    	mPrompt = new NumberPrompt(this, mCommentSetListener, mCellOptions);
    	return mPrompt;
    }
    
    private void LoadMainMenu()
    {
    	setContentView(R.layout.mainmenu);
    	
    	Button easyButton = (Button) findViewById(R.id.buttonEasy);
    	easyButton.setOnClickListener(new StartButtonListener("Easy"));
    	
    	Button hardButton = (Button) findViewById(R.id.buttonHard);
    	hardButton.setOnClickListener(new StartButtonListener("Hard"));
    	
    	Button twoPlayerButton = (Button) findViewById(R.id.buttonTwoPlayer);
    	twoPlayerButton.setOnClickListener(new StartButtonListener("TwoPlayer"));
    }
    
    private void StartGame(String difficulty)
    {
    	setContentView(R.layout.main);
        
        mSudoku = (SudokuView) findViewById(R.id.sudoku);
        mSudoku.setOnTouchListener(new SudokuTouchListener());
        
        mGameText = (TextView) findViewById(R.id.gameIntro);
        mGameScore = (TextView) findViewById(R.id.gameScore);
        
        Button quitButton = (Button) findViewById(R.id.buttonQuit);
        quitButton.setOnClickListener(new OnClickListener()
        {
			public void onClick(View v)
			{
				LoadMainMenu();
			}
		});
        
        if(difficulty == null || !difficulty.equals("TwoPlayer"))
        	mGame = new SudokuGameOnePlayer();
        else
        {
        	mGame = new SudokuGameTwoPlayer();
        	mGameText.setText("Player 1 turn");
        	mGame.UpdateScore(mGameScore);
        }
        
        mGame.StartGame(mSudoku, difficulty, "Player1", "Player2");
    }
    
    private NumberPrompt.OnNumberSetListener mCommentSetListener = new NumberPrompt.OnNumberSetListener()
	{
        public void onNumberSet(NumberPrompt view, int number)
        {
        	if(number <= 0)
        		return;
        	
        	AlertDialog.Builder builder = mGame.MakeMove(mContext, mSudoku, mCurrentPoint, number);
        	if(builder != null)
        	{
        		builder.setPositiveButton("Menu", new DialogInterface.OnClickListener()
        		{
        			public void onClick(DialogInterface dialog,int id)
        			{
        				// if this button is clicked, close
        				// current activity
        				//MainActivity.this.finish();
        				dialog.cancel();
        				LoadMainMenu();
        			}
        		  });
        		
        		builder.create().show();
        	}
        	
        	if(mGame.GetNumberOfPlayers() > 1)
        		mGameText.setText(String.format("Player %d turn", mGame.GetCurrentPlayer() + 1));
        	
        	mGame.UpdateScore(mGameScore);
        }
	};
    
	private class StartButtonListener implements OnClickListener
	{
		private String mDifficulty = null;
		
		public StartButtonListener(String difficulty)
		{
			mDifficulty = difficulty;
		}

		public void onClick(View v)
		{
			StartGame(mDifficulty);
		}
		
	}
	
    private class SudokuTouchListener implements OnTouchListener
    {
		public boolean onTouch(View arg0, MotionEvent arg1)
		{
			//Determine which cell was clicked
			mCurrentPoint = mSudoku.GetCell(arg1.getX(), arg1.getY());
			Log.i("SudokuActivity", String.format("Clicked box (%d, %d)", mCurrentPoint.x, mCurrentPoint.y));
			
			//Make sure we got a legal cell value
			if(mCurrentPoint.x < 0 || mCurrentPoint.x >= SudokuLogic.BoardSize || mCurrentPoint.y < 0 || mCurrentPoint.y >= SudokuLogic.BoardSize)
				return false;
			
			//Show the number prompt if the cell clicked is valid in the current game
			if(mGame.HandleClick(mCurrentPoint))
			{
				mCellOptions = SudokuLogic.GetOptions(mGame.GetFullBoard(), mCurrentPoint);
				if(mPrompt != null)
					mPrompt.SetOptions(mCellOptions);
				showDialog(0);
			}
			
			return false;
		}
    }
}