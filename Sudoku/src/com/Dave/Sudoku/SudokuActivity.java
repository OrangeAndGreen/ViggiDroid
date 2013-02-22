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
	
	private EditText mPlayer1Text = null;
	private EditText mPlayer2Text = null;
	private Spinner mCellsToFillSpinner = null;
	private CharSequence[] mFillOptions = {"0", "5", "10"};
	private Spinner mScoringSystemSpinner = null;
	private CharSequence[] mScoringOptions = {"Vanilla", "System 1"};
	
	private Context mContext = null;
	private TextView mGameText = null;
	private TextView mGameScore = null;
	private SudokuView mSudoku = null;
	private Button mClearButton = null;
	private Button mConfirmButton = null;
	private Button mShowButton = null;
	
	private boolean mShowingPossibilities = false;
	private Point mCurrentPoint = null;
	private byte mCurrentValue = 0;
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
		mScoringSystemSpinner.setSelection(1);
		
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
    	
    	if(scoringSystem != null && scoringSystem == "System 1")
    		mScoring = new ScoringConcept1();
    	else
    		mScoring = new ScoringVanilla();
    	
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

        mShowingPossibilities = false;
    }
    
    private void MakeMove()
    {
    	AlertDialog.Builder builder = mGame.MakeMove(mContext, mSudoku, mCurrentPoint, mCurrentValue, mScoring);
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
    	
    	mCurrentPoint = null;
    	
    	UpdateBoard();
    }
    
    private void UpdateBoard()
    {
    	if(mShowingPossibilities)
		{
    		//Calling ShowMove() to update (possibly erase) a pending move within the game
    		mGame.ShowMove(mSudoku, mCurrentPoint, mCurrentValue, mScoring);
    		mSudoku.ShowSquareOptions();
		}
		else
		{
			mGame.ShowMove(mSudoku, mCurrentPoint, mCurrentValue, mScoring);
			mGame.UpdateScore(mGameScore);
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
    
    private NumberPrompt.OnNumberSetListener mNumberSetListener = new NumberPrompt.OnNumberSetListener()
	{
        public void onNumberSet(NumberPrompt view, int number)
        {
        	if(number < 0)
        		return;
        	
        	//In one player mode, a 0 will reset the tile (when the player clicks cancel)
        	if(mGame.GetNumberOfPlayers() > 1 && number == 0)
        		return;

        	mCurrentValue = (byte)number;
        	
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
			if(currentPoint.x < 0 || currentPoint.x >= SudokuLogic.BoardSize || currentPoint.y < 0 || currentPoint.y >= SudokuLogic.BoardSize)
				return false;
			
			//Show the number prompt if the cell clicked is valid in the current game
			if(mGame.HandleClick(currentPoint))
			{
				mCurrentPoint = currentPoint;
				mCellOptions = SudokuLogic.GetOptions(mGame.GetFullBoard(), mCurrentPoint);
				if(mPrompt != null)
					mPrompt.SetOptions(mCellOptions);
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
			mCurrentValue = 0;
			
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