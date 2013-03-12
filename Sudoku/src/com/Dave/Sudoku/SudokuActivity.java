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
 * 
 * -Link button prompt to numeric keys
 * -Start learning about how to connect in advertisements
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
	private CharSequence[] mFillOptions = { "-1", "0", "1", "3", "5" };
	private Spinner mHandSizeSpinner = null;
	private CharSequence[] mHandSizeOptions = { "3", "5", "7", "9" };
	private Spinner mBonusCellsSpinner = null;
	private CharSequence[] mBonusCellOptions = { "0", "1", "2", "3" };
	private Spinner mScoringSystemSpinner = null;
	private CharSequence[] mScoringOptions = { "Vanilla", "System 1", "System 2", "Least square" };
	private Spinner mHandSystemSpinner = null;
	private CharSequence[] mHandOptions = { "Vanilla", "Concept 1" };
	
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
	private List<Byte> mCellOptions = null;
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
    	mHandSizeSpinner = (Spinner) findViewById(R.id.spinnerHandSize);
    	mBonusCellsSpinner = (Spinner) findViewById(R.id.spinnerBonusCells);
    	mScoringSystemSpinner = (Spinner) findViewById(R.id.spinnerScoringSystem);
    	mHandSystemSpinner = (Spinner) findViewById(R.id.spinnerHandSystem);
		
		//Setup the Cell-to-fill Spinner
		ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(this, android.R.layout.simple_spinner_item, mFillOptions);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mCellsToFillSpinner.setAdapter(adapter);
		mCellsToFillSpinner.setSelection(0);
		
		//Setup the hand size Spinner
		adapter = new ArrayAdapter<CharSequence>(this, android.R.layout.simple_spinner_item, mHandSizeOptions);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mHandSizeSpinner.setAdapter(adapter);
		mHandSizeSpinner.setSelection(1);
		
		//Setup the bonus cells Spinner
		adapter = new ArrayAdapter<CharSequence>(this, android.R.layout.simple_spinner_item, mBonusCellOptions);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mBonusCellsSpinner.setAdapter(adapter);
		mBonusCellsSpinner.setSelection(2);
		
		//Setup the Hand system Spinner
		adapter = new ArrayAdapter<CharSequence>(this, android.R.layout.simple_spinner_item, mHandOptions);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mHandSystemSpinner.setAdapter(adapter);
		mHandSystemSpinner.setSelection(1);
		
		//Setup the Scoring system Spinner
		adapter = new ArrayAdapter<CharSequence>(this, android.R.layout.simple_spinner_item, mScoringOptions);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mScoringSystemSpinner.setAdapter(adapter);
		mScoringSystemSpinner.setSelection(3);
		
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
				
				int handSize = Integer.parseInt(mHandSizeOptions[mHandSizeSpinner.getSelectedItemPosition()].toString());
				
				int bonusCells = Integer.parseInt(mBonusCellOptions[mBonusCellsSpinner.getSelectedItemPosition()].toString());
				
				String handSystem = mHandOptions[mHandSystemSpinner.getSelectedItemPosition()].toString();
				
				String scoringSystem = mScoringOptions[mScoringSystemSpinner.getSelectedItemPosition()].toString();
				
				StartTwoPlayerGame(player1Name, player2Name, cellsToFill, handSize, bonusCells, handSystem, scoringSystem);
			}
		});
    }
    
    private void StartOnePlayerGame(String difficulty)
    {
    	DebugLog.Write("Starting one player game", null);
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
    
    private void StartTwoPlayerGame(String player1Name, String player2Name, int cellsToFill, int handSize, int bonusCells, String handSystem, String scoringSystem)
    {
    	DebugLog.Write("Starting two player game", null);
    	
    	PrepareGame();
    	
    	//Start two player game
    	mGame = new SudokuGameTwoPlayer(mSudoku, player1Name, player2Name, cellsToFill, bonusCells);
    	DebugLog.Write(String.format("Bonus cells: %d", bonusCells), null);
    	String cellsFilled = String.format("Fill %d", cellsToFill);
    	DebugLog.Write("Initial board:\n" + mGame.GetBoard().toString(), null);
    	
    	if(handSystem != null && handSystem == "Concept 1")
    		mHand = new HandConcept1();
    	else
    		mHand = new HandVanilla();
    	
    	mHand.SetHandSize(handSize);
    	DebugLog.Write("Hand: " + mHand.GetName(), null);
    	
    	if(scoringSystem != null && scoringSystem == "System 1")
    	{
    		mScoring = new ScoringConcept1();
    	}
    	else if(scoringSystem != null && scoringSystem == "System 2")
    	{
    		mScoring = new ScoringConcept2();
    	}
    	else if(scoringSystem != null && scoringSystem == "Least square")
    	{
    		mScoring = new ScoringConcept3();
    	}
    	else
    	{
    		mScoring = new ScoringVanilla();
    	}
    	DebugLog.Write("Scoring: " + mScoring.GetName(), null);
    	
    	String gameOptions = String.format("Game: %s, %s, %s", cellsFilled, mHand.GetName(), mScoring.GetName());
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
				if(mGame != null)
					DebugLog.Write("Aborted board:\n" + mGame.GetBoard().toString(), null);
				
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
    	
    	DebugLog.Write(String.format("Committing move %d at (%d, %d)", mPendingValue, mPendingPoint.x, mPendingPoint.y), null);
    	
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
    		
    		DebugLog.Write("Final board:\n" + mGame.GetBoard().toString(), null);
    		
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
    		
    		DebugLog.Write("Drawing hand: " + mHand.toString(), null);
    		
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
    		
    		//Log.i("SudokuActivity", String.format("Hand: %s", mHand.toString()));
    		
    		for(int i=mCellOptions.size() - 1; i>=0; i--)
    			if(!hand.contains(mCellOptions.get(i)))
    			{
    				//Log.i("SudokuActivity", String.format("Disabling %d", i));
    				mCellOptions.remove(i);
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
        		DebugLog.Write(String.format("Showing move %d at (%d, %d)", mPendingValue, mPendingPoint.x, mPendingPoint.y), null);
        		
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
			DebugLog.Write("Clearing move", null);
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
			
			if(mShowingPossibilities)
				DebugLog.Write("Showing possibilities", null);
			else
				DebugLog.Write("Showing main board", null);
			
			UpdateBoard();
		}
    }
}