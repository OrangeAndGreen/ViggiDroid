package com.Dave.Sudoku;


import java.util.List;

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
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
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
 * -Highlight all cells affected by a move during preview
 * -Apply multiplier when player gets to "go again"
 * 
 */

public class SudokuActivity extends Activity
{
	private ISudokuGame mGame = null;
	private IScoring mScoring = null;
	private IHand mHand = null;
	
	private EditText mPlayer1Text = null;
	private EditText mPlayer2Text = null;
	private Spinner mCellsToFillSpinner = null;
	private CharSequence[] mFillOptions = { "0", "5", "10" };
	private Spinner mScoringSystemSpinner = null;
	private CharSequence[] mScoringOptions = { "Vanilla", "System 1", "System 2" };
	
	private Context mContext = null;
	private TextView mGameText = null;
	private TextView mGameScore = null;
	private SudokuView mSudoku = null;
	private Button mClearButton = null;
	private Button mConfirmButton = null;
	private Button mShowButton = null;
	private TextView mHandText = null;
	private TextView mDebugText = null;
	
	private boolean mShowingPossibilities = false;
	private Point mCurrentPoint = null;
	private Point mPendingPoint = null;
	private byte mPendingValue = 0;
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
    	mPrompt = new NumberPrompt(this, mNumberSetListener, mCellOptions);
    	
    	PreparePrompt();
    	
    	return mPrompt;
    }
    
    private void LoadMainMenu()
    {
    	setContentView(R.layout.mainmenu);
    	
    	Button easyButton = (Button) findViewById(R.id.buttonEasy);
    	easyButton.setOnClickListener(new StartButtonListener(false, "Easy"));
    	
    	Button hardButton = (Button) findViewById(R.id.buttonHard);
    	hardButton.setOnClickListener(new StartButtonListener(false, "Hard"));
    	
    	Button twoPlayerButton = (Button) findViewById(R.id.buttonTwoPlayer);
    	twoPlayerButton.setOnClickListener(new StartButtonListener(true, null));
    }
    
    private void LoadGameMenu()
    {
    	setContentView(R.layout.gameoptions);
    	
    	mPlayer1Text = (EditText) findViewById(R.id.inputPlayer1);
    	mPlayer2Text = (EditText) findViewById(R.id.inputPlayer2);
    	
    	mCellsToFillSpinner = (Spinner) findViewById(R.id.spinnerCellsToFill);
    	mScoringSystemSpinner = (Spinner) findViewById(R.id.spinnerScoringSystem);
		
		//Setup the graph types Spinner
		ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(this, android.R.layout.simple_spinner_item, mFillOptions);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mCellsToFillSpinner.setAdapter(adapter);
		mCellsToFillSpinner.setSelection(1);
		
		//Setup the graph times Spinner
		adapter = new ArrayAdapter<CharSequence>(this, android.R.layout.simple_spinner_item, mScoringOptions);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mScoringSystemSpinner.setAdapter(adapter);
		mScoringSystemSpinner.setSelection(2);
		
		Button startButton = (Button) findViewById(R.id.buttonStart);
		startButton.setOnClickListener(new OnClickListener()
		{
			public void onClick(View v)
			{
				String player1Name = mPlayer1Text.getText().toString();
				if(player1Name.trim().equals(""))
					player1Name = "Player 1";
				String player2Name = mPlayer2Text.getText().toString();
				if(player2Name.trim().equals(""))
					player2Name = "Player 2";
				
				int cellsToFill = Integer.parseInt(mFillOptions[mCellsToFillSpinner.getSelectedItemPosition()].toString());
				
				String scoringSystem = mScoringOptions[mScoringSystemSpinner.getSelectedItemPosition()].toString();
				
				StartTwoPlayerGame(player1Name, player2Name, cellsToFill, scoringSystem);
			}
		});
    }
    
    private void StartOnePlayerGame(String difficulty)
    {
    	PrepareGame();
        
        //Start one player game
        mGame = new SudokuGameOnePlayer(mSudoku, difficulty);
        mScoring = null;
        mHand = null;
        mGameText.setText("One player Sudoku!");
        
        mClearButton.setVisibility(Button.INVISIBLE);
        mConfirmButton.setVisibility(Button.INVISIBLE);
        mShowButton.setVisibility(Button.INVISIBLE);
    }
    
    private void StartTwoPlayerGame(String player1Name, String player2Name, int cellsToFill, String scoringSystem)
    {
    	PrepareGame();
    	
    	//Start two player game
    	mGame = new SudokuGameTwoPlayer(mSudoku, player1Name, player2Name, cellsToFill);
    	String cellsFilled = String.format("Fill %d", cellsToFill);
    	
    	String handType = "no hand";
    	handType = "Hand Concept 1";
    	mHand = new HandConcept1();
    	mHand.SetHandSize(9);
    	
    	String scoringType = null;
    	if(scoringSystem != null && scoringSystem == "System 1")
    	{
    		scoringType = "Scoring 1";
    		mScoring = new ScoringConcept1();
    	}
    	else if(scoringSystem != null && scoringSystem == "System 2")
    	{
    		scoringType = "Scoring 2";
    		mScoring = new ScoringConcept2();
    	}
    	else
    	{
    		scoringType = "Scoring Vanilla";
    		mScoring = new ScoringVanilla();
    	}
    	
    	String gameOptions = String.format("Game: %s, %s, %s", cellsFilled, scoringType, handType);
    	mDebugText.setText(gameOptions);
    	
    	mGameText.setText("Battle Sudoku!");
    	mGame.UpdateScore(mGameScore);
    	
    	mClearButton.setVisibility(Button.VISIBLE);
    	mConfirmButton.setVisibility(Button.VISIBLE);
    	mShowButton.setVisibility(Button.VISIBLE);
    	
    	DisablePendingButtons();
    }
    
    private void PrepareGame()
    {
    	setContentView(R.layout.game);
        
    	//Get the GUI elements and assign listeners
        mSudoku = (SudokuView) findViewById(R.id.sudoku);
        mSudoku.setOnTouchListener(new SudokuTouchListener());
        
        mGameText = (TextView) findViewById(R.id.gameIntro);
        
        mGameScore = (TextView) findViewById(R.id.gameScore);
        mGameScore.setText("");
        
        Button quitButton = (Button) findViewById(R.id.buttonQuit);
        quitButton.setOnClickListener(new OnClickListener()
        {
			public void onClick(View v)
			{
				LoadMainMenu();
			}
		});
        
        mClearButton = (Button) findViewById(R.id.buttonClear);
        mClearButton.setOnClickListener(new ClearButtonListener());
        
        mConfirmButton = (Button) findViewById(R.id.buttonConfirm);
        mConfirmButton.setOnClickListener(new ConfirmButtonListener());
        
        mShowButton = (Button) findViewById(R.id.buttonShow);
        mShowButton.setOnClickListener(new ShowButtonListener());
        
        mHandText = (TextView) findViewById(R.id.handText);
        mHandText.setText("");
        
        mDebugText = (TextView) findViewById(R.id.debugText);
        mDebugText.setText("");

        mShowingPossibilities = false;
    }
    
    private void MakeMove()
    {
    	if(mHand != null && mGame.GetGamePhase() > 0)
    	{
    		mHand.TakeNumber(mGame.GetBoard(), mGame.GetCurrentPlayer(), mPendingValue);
    	}
    	
    	AlertDialog.Builder builder = mGame.MakeMove(mContext, mSudoku, mPendingPoint, mPendingValue, mScoring);
    	if(builder != null)
    	{
    		builder.setPositiveButton("Menu", new DialogInterface.OnClickListener()
    		{
    			public void onClick(DialogInterface dialog,int id)
    			{
    				dialog.cancel();
    				LoadMainMenu();
    			}
    		  });
    		
    		builder.create().show();
    	}
    	
    	if(mHand != null && mGame.GetGamePhase() > 0)
    		DrawHand();
    	
    	mCurrentPoint = null;
    	mPendingPoint = null;
    	mPendingValue = 0;
    	
    	UpdateBoard();
    }
    
    private void UpdateBoard()
    {
    	//Always calling ShowMove() to update (possibly erase) a pending move within the game
    	mGame.ShowMove(mSudoku, mPendingPoint, mPendingValue, mScoring);
		mGame.UpdateScore(mGameScore);
    	if(mShowingPossibilities)
		{
    		mSudoku.ShowSquareOptions();
		}
    }
    
    private void DrawHand()
    {
    	if(mHand != null && mGame != null)
    	{
    		List<Byte> hand = mHand.GetHand(mGame.GetBoard(), mGame.GetCurrentPlayer());
    		
    		String handText = "Hand: ";
    		for(int i=0; i<hand.size(); i++)
    		{
    			if(i>0)
    				handText += ", ";
    			handText += String.format("%d", hand.get(i));
    		}
    		
    		mHandText.setText(handText);
    	}
    }
    
    private void EnablePendingButtons()
    {
    	mClearButton.setEnabled(true);
		mConfirmButton.setEnabled(true);
    }
    
    private void DisablePendingButtons()
    {
    	mClearButton.setEnabled(false);
		mConfirmButton.setEnabled(false);
    }
    
    private void PreparePrompt()
    {
    	mCellOptions = mGame.GetBoard().GetCellOptions(mCurrentPoint, false);
    	
    	if(mHand != null && mGame.GetGamePhase() > 0)
    	{
    		//Disable any options that aren't present in the player's hand
    		List<Byte> hand = mHand.GetHand(mGame.GetBoard(), mGame.GetCurrentPlayer());
    		
    		Log.i("SudokuActivity", String.format("Hand: %s", mHand.ToString()));
    		
    		for(int i=1; i<mCellOptions.length; i++)
    			if(mCellOptions[i] && !hand.contains((byte)i))
    			{
    				Log.i("SudokuActivity", String.format("Disabling %d", i));
    				mCellOptions[i] = false;
    			}
    	}
    	
		if(mPrompt != null)
			mPrompt.SetOptions(mCellOptions);
    }
    
    private NumberPrompt.OnNumberSetListener mNumberSetListener = new NumberPrompt.OnNumberSetListener()
	{
        public void onNumberSet(NumberPrompt view, int number)
        {
        	if(number < 0)
        		return;
        	
        	//In one player mode, a 0 will reset the tile (when the player clicks cancel)
        	if(mGame.GetNumberOfPlayers() > 1 && number == 0)
        		return;

        	mPendingPoint = mCurrentPoint;
        	mPendingValue = (byte)number;
        	
        	if(mGame.GetConfirmCommit())
        	{
        		UpdateBoard();
        		
        		EnablePendingButtons();
        	}
        	else
        	{
	        	MakeMove();
        	}
        }
	};
    
	//Different start buttons are used to select the number of players and difficulty
	private class StartButtonListener implements OnClickListener
	{
		private String mDifficulty = null;
		private boolean mTwoPlayer = false;
		
		public StartButtonListener(boolean twoPlayer, String difficulty)
		{
			mTwoPlayer = twoPlayer;
			mDifficulty = difficulty;
		}

		public void onClick(View v)
		{
			if(mTwoPlayer)
				LoadGameMenu();
			else
				StartOnePlayerGame(mDifficulty);
		}
	}

	//This class responds when the user clicks the Sudoku board
    private class SudokuTouchListener implements OnTouchListener
    {
		public boolean onTouch(View arg0, MotionEvent arg1)
		{
			if(mShowingPossibilities)
				return false;
			
			//Determine which cell was clicked
			Point currentPoint = mSudoku.GetCell(arg1.getX(), arg1.getY());
			Log.i("SudokuActivity", String.format("Clicked box (%d, %d)", currentPoint.x, currentPoint.y));
			
			//Make sure we got a legal cell value
			if(currentPoint.x < 0 || currentPoint.x >= SudokuBoard.BoardSize || currentPoint.y < 0 || currentPoint.y >= SudokuBoard.BoardSize)
				return false;
			
			//Show the number prompt if the cell clicked is valid in the current game
			if(mGame.HandleClick(currentPoint))
			{
				mCurrentPoint = currentPoint;
				
				PreparePrompt();
				
				showDialog(0);
			}
			
			return false;
		}
    }
    
    //This button clears a pending move
    private class ClearButtonListener implements OnClickListener
    {
		public void onClick(View v)
		{
			mCurrentPoint = null;
			mPendingPoint = null;
			mPendingValue = 0;
			
			UpdateBoard();
			
			DisablePendingButtons();
		}
    }
    
    //This button commits a move
    private class ConfirmButtonListener implements OnClickListener
    {
		public void onClick(View v)
		{
			MakeMove();
			
			DisablePendingButtons();
		}
    }
    
    //This button toggles the available numbers for a square or the full board
    private class ShowButtonListener implements OnClickListener
    {
		public void onClick(View v)
		{
			mShowingPossibilities = !mShowingPossibilities;
			
			UpdateBoard();
		}
    }
}