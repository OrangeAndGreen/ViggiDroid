package com.Dave.Sudoku;


import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import com.Dave.Sudoku.HttpClient.GameListReadyListener;
import com.Dave.Sudoku.HttpClient.GameReadyListener;
import com.Dave.Sudoku.HttpClient.GameUpdatedListener;

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
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;


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
	private String mServer = "http://10.0.2.2:8080";
	private HttpClient mClient = null;
	private boolean mShowingMainMenu = false;
	
	private SudokuGameTwoPlayer mGame = null;
	private String mPlayerName = null;
	private ListView mTwoPlayerList = null;
	
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

	private ArrayList<String> mExistingGames = new ArrayList<String>();
	private ArrayList<Integer> mExistingGameIds = new ArrayList<Integer>();
	private ArrayAdapter<String> mArrayAdapter = null;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        
        mContext = this;

        mClient = new HttpClient(this);
        
        //Show the name prompt
        showDialog(1);
    }
    
    @Override
    protected Dialog onCreateDialog(int id)
    {
    	if(id == 0)
    	{
    		//Show the number prompt
    		mPrompt = new NumberPrompt(this, mNumberSetListener, mCellOptions);
    	
    		PreparePrompt();
    		
    		return mPrompt;
    	}
    	else if(id == 1)
    	{
    		//Show the name prompt
    		NamePrompt prompt = new NamePrompt(this, mNameSetListener);
    		return prompt;
    	}
    	
    	return null;
    }
    
    @Override
    public void onBackPressed()
    {
        if(mShowingMainMenu)
        	finish();
        else
        	LoadMainMenu();
        return;
    }
    
    private void LoadMainMenu()
    {
    	setContentView(R.layout.mainmenu);
    	
    	mShowingMainMenu = true;
    	
    	Button easyButton = (Button) findViewById(R.id.buttonEasy);
    	easyButton.setOnClickListener(new StartButtonListener(false, "Easy"));
    	
    	Button hardButton = (Button) findViewById(R.id.buttonHard);
    	hardButton.setOnClickListener(new StartButtonListener(false, "Hard"));
    	
    	Button twoPlayerButton = (Button) findViewById(R.id.buttonTwoPlayer);
    	twoPlayerButton.setOnClickListener(new StartButtonListener(true, null));
    	
    	mTwoPlayerList = (ListView) findViewById(R.id.twoPlayerList);
    	mArrayAdapter = new ArrayAdapter<String>(mContext, android.R.layout.simple_list_item_1, mExistingGames);
    	mTwoPlayerList.setAdapter(mArrayAdapter);
		mTwoPlayerList.setOnItemClickListener(new ResumeGameListener(mExistingGameIds));
			
    	//Start getting the list of existing games
        mClient.GetGameList(mServer, mPlayerName, new GameListReadyListener()
        {
			public void OnGameListReady(List<SudokuGameTwoPlayer> gameList)
			{
				Toast.makeText(mContext, String.format("%d games found", gameList.size()), Toast.LENGTH_SHORT).show();
				
				mArrayAdapter.clear();
				//mExistingGames.clear();
				mExistingGameIds.clear();
				
				//Split the list into individual entries
				for(int i=0; i<gameList.size(); i++)
				{
					SudokuGameTwoPlayer game = gameList.get(i);
					String entry = String.format("%s vs. %s, started %s", game.GetPlayer1Name(), game.GetPlayer2Name(), new SimpleDateFormat("MM/dd/yyyy").format(game.StartDate.getTime()));
					if(game.IsLocalPlayerTurn(mPlayerName))
						entry += " (your turn)";
					mArrayAdapter.add(entry);
					mExistingGameIds.add(game.GameId);
				}
					
			}
		});
    }
    
    private void LoadGameMenu()
    {
    	setContentView(R.layout.gameoptions);
    	
    	mShowingMainMenu = false;
    	
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
				String player2Name = mPlayer2Text.getText().toString();
				if(player2Name.trim().equals(""))
					player2Name = "Player 2";
				
				int cellsToFill = Integer.parseInt(mFillOptions[mCellsToFillSpinner.getSelectedItemPosition()].toString());
				
				int handSize = Integer.parseInt(mHandSizeOptions[mHandSizeSpinner.getSelectedItemPosition()].toString());
				
				int bonusCells = Integer.parseInt(mBonusCellOptions[mBonusCellsSpinner.getSelectedItemPosition()].toString());
				
				String handSystem = mHandOptions[mHandSystemSpinner.getSelectedItemPosition()].toString();
				
				String scoringSystem = mScoringOptions[mScoringSystemSpinner.getSelectedItemPosition()].toString();
				
				StartTwoPlayerGame(mPlayerName, player2Name, cellsToFill, handSize, bonusCells, handSystem, scoringSystem);
			}
		});
    }
    
    private void StartOnePlayerGame(String difficulty)
    {
    	DebugLog.Write("Starting one player game... BROKEN", null);
    	Toast t = Toast.makeText(this, "1 player mode broken", Toast.LENGTH_SHORT);
    	t.show();
    	//PrepareGame();
        
        //Start one player game
        //mGame = new SudokuGameOnePlayer(mSudoku, difficulty);
        //mGameText.setText("One player Sudoku!");
        
        //mClearButton.setVisibility(Button.INVISIBLE);
        //mConfirmButton.setVisibility(Button.INVISIBLE);
        //mShowButton.setVisibility(Button.INVISIBLE);
    }
    
    private void StartTwoPlayerGame(String player1Name, String player2Name, int cellsToFill, int handSize, int bonusCells, String handSystem, String scoringSystem)
    {
    	DebugLog.Write("Starting two player game", null);
    	
    	PrepareGame();
    	
    	//Start two player game
    	mGame = new SudokuGameTwoPlayer(mSudoku, player1Name, player2Name, cellsToFill, bonusCells, scoringSystem, handSystem, handSize);
    	DebugLog.Write(String.format("Bonus cells: %d", bonusCells), null);
    	String cellsFilled = String.format("Fill %d", cellsToFill);
    	
    	DebugLog.Write("Initial board:\n" + mGame.GetBoard().toString(), null);
    	
    	String gameOptions = String.format("Game: %s, %s, %s", cellsFilled, mGame.GetHandSystem(), mGame.GetScoringSystem());
    	mDebugText.setText(gameOptions);
    	
    	mGameText.setText("Battle Sudoku!");
    	mGame.UpdateScore(mGameScore, mPlayerName);
    	
    	mClearButton.setVisibility(Button.VISIBLE);
    	mConfirmButton.setVisibility(Button.VISIBLE);
    	mShowButton.setVisibility(Button.VISIBLE);
    	
    	DisablePendingButtons();
    }
    
    private void ResumeGame(int gameId)
    {
    	mClient.GetGame(mServer, gameId, mPlayerName, new GameReadyListener()
		{
			public void OnGameReady(SudokuGameTwoPlayer game)
			{
				DebugLog.Write("Resuming two player game", null);
		    	
		    	PrepareGame();
		    	
		    	//Resume the two player game
		    	mGame = game;
		    	
		    	String gameOptions = String.format("Game: %s, %s", mGame.GetHandSystem(), mGame.GetScoringSystem());
		    	mDebugText.setText(gameOptions);
		    	
		    	mGameText.setText("Battle Sudoku!");
		    	mGame.UpdateScore(mGameScore, mPlayerName);
		    	
		    	mClearButton.setVisibility(Button.VISIBLE);
		    	mConfirmButton.setVisibility(Button.VISIBLE);
		    	mShowButton.setVisibility(Button.VISIBLE);
		    	
		    	DisablePendingButtons();
		    	
		    	mSudoku.InitializeBoard(mGame.GetBoard(), mGame.GetPlayer1Color(), mGame.GetPlayer2Color());
		    	
		    	DrawHand();
			}
		});
    }
    
    private void PrepareGame()
    {
    	setContentView(R.layout.game);
    	
    	mShowingMainMenu = false;
        
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
				//if(mGame != null)
				//	DebugLog.Write("Aborted board:\n" + mGame.GetBoard().toString(), null);
				
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
    	DebugLog.Write(String.format("Committing move %d at (%d, %d)", mPendingValue, mPendingPoint.x, mPendingPoint.y), null);
    	
    	AlertDialog.Builder builder = mGame.MakeMove(mContext, mSudoku, mPendingPoint, mPendingValue);
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
    	
    	DrawHand();
    	
    	mCurrentPoint = null;
    	mPendingPoint = null;
    	mPendingValue = 0;
    	
    	UpdateBoard();
    	
    	//Toast t = Toast.makeText(mContext, "Uploading game to server", Toast.LENGTH_SHORT);
		//t.show();
		
    	mClient.UpdateGame(mServer, mGame, mPlayerName, new GameUpdatedListener()
    	{
			public void OnGameUpdated(boolean success)
			{
				String successStr = "Update failed";
				if(success)
					successStr = "Update complete";
				Toast t = Toast.makeText(mContext, successStr, Toast.LENGTH_SHORT);
				t.show();
			}
    		
    	});
    }
    
    private void UpdateBoard()
    {
    	//Always calling ShowMove() to update (possibly erase) a pending move within the game
    	mGame.ShowMove(mSudoku, mPendingPoint, mPendingValue);
		mGame.UpdateScore(mGameScore, mPlayerName);
    	if(mShowingPossibilities)
		{
    		mSudoku.ShowSquareOptions();
		}
    }
    
    private void DrawHand()
    {
    	if(mGame != null && mGame.GetHand() != null &&  mGame.GetGamePhase() > 0 && mGame.IsLocalPlayerTurn(mPlayerName))
    	{
    		List<Byte> hand = mGame.GetHand();	
    		String handText = "";
    		for(int i=0; i<hand.size(); i++)
    		{
    			if(i>0)
    				handText += ", ";
    			handText += String.format("%d", hand.get(i));
    		}
    		DebugLog.Write("Drawing hand: " + handText, null);
    		handText = "Hand: " + handText;
    		
    		mHandText.setText(handText);
    	}
    	else
    		mHandText.setText("");
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
    	
    	List<Byte> hand = mGame.GetHand();
    	if(hand != null && mGame.GetGamePhase() > 0)
    	{
    		//Log.i("SudokuActivity", String.format("Hand: %s", mHand.toString()));
    		
    		//Disable any options that aren't present in the player's hand
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
	
	private NamePrompt.OnNameSetListener mNameSetListener = new NamePrompt.OnNameSetListener()
	{
		public void onNameSet(NamePrompt view, String name)
		{
			mPlayerName = name;
			
			LoadMainMenu();
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

	//This class responds when the user chooses to play an existing 2-player game
	private class ResumeGameListener implements OnItemClickListener
	{
		private ArrayList<Integer> mGameIds = null;
		
		public ResumeGameListener(ArrayList<Integer> gameIds)
		{
			mGameIds = gameIds;
		}

		public void onItemClick(AdapterView<?> adapter, View v, int position, long id)
		{
			ResumeGame(mGameIds.get(position));
		}
	}
	
	//This class responds when the user clicks the Sudoku board
    private class SudokuTouchListener implements OnTouchListener
    {
		public boolean onTouch(View arg0, MotionEvent arg1)
		{
			//Ignore the click if we're in the possibility view OR it's not the local player's turn
			if(mShowingPossibilities || !mGame.IsLocalPlayerTurn(mPlayerName))
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