package com.Dave.Sudoku;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.Dave.Sudoku.Game.SudokuGameTwoPlayer;
import com.Dave.Sudoku.HttpClient.CreatePlayerListener;
import com.Dave.Sudoku.HttpClient.GameListReadyListener;
import com.Dave.Sudoku.HttpClient.GameReadyListener;
import com.Dave.Sudoku.HttpClient.GameUpdatedListener;
import com.Dave.Sudoku.HttpClient.PasswordChangedListener;
import com.Dave.Sudoku.HttpClient.PlayerStats;
import com.Dave.Sudoku.HttpClient.StatsReadyListener;
import com.Dave.Sudoku.Prompt.ChangePasswordPrompt;
import com.Dave.Sudoku.Prompt.ChangePasswordPrompt.OnSetPasswordListener;
import com.Dave.Sudoku.Prompt.LoginPrompt;
import com.Dave.Sudoku.Prompt.NumberPrompt;
import com.google.android.gcm.GCMRegistrar;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Point;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
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
	// Local IP: "http://10.0.2.2:8080";
	private String mServer = "http://orangeandgreen.no-ip.biz:33133";
	private String mSenderId = "563870167345";
	private String mRegisteredId = null;
	private HttpClient mClient = null;
	private SharedPreferences mPreferences = null;
	private boolean mShowingMainMenu = false;

	private SudokuGameTwoPlayer mGame = null;
	private String mPlayerName = null;
	private String mPlayerPassword = null;
	private ListView mTwoPlayerList = null;

	private EditText mPlayer2Text = null;
	private CheckBox mFillCenter = null;
	private Spinner mCellsToFillSpinner = null;
	private CharSequence[] mFillOptions = { "-2", "-1", "0", "1", "3", "5" };
	//private Spinner mHandSizeSpinner = null;
	//private CharSequence[] mHandSizeOptions = { "3", "5", "7", "9" };
	private Spinner mBonusCellsSpinner = null;
	private CharSequence[] mBonusCellOptions = { "0", "1", "2", "3" };
	private Spinner mScoringSystemSpinner = null;
	private CharSequence[] mScoringOptions = { "Vanilla", "System 1", "System 2", "Least square" };
	//private Spinner mHandSystemSpinner = null;
	//private CharSequence[] mHandOptions = { "Vanilla", "Concept 1" };
	private Spinner mBonusSystemSpinner = null;
	private CharSequence[] mBonusOptions = { "0", "+5", "10" };
	private Spinner mMultiplierSpinner = null;
	private CharSequence[] mMultiplierOptions = { "111", "122", "123" };

	private Context mContext = null;
	private TextView mGameText = null;
	private TextView mGameScore = null;
	private SudokuView mSudoku = null;
	private Button mClearButton = null;
	private Button mConfirmButton = null;
	private Button mShowButton = null;
	private TextView mHandText = null;
	private TextView mDebugText = null;
	private TextView mStatsView = null;

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

		mClient = new HttpClient();

		mPreferences = getPreferences(MODE_PRIVATE);
		mPlayerName = mPreferences.getString("PlayerName", "");
		mPlayerPassword = mPreferences.getString("PlayerPassword", "");

		// Show the name prompt
		showDialog(1);
	}

	@Override
	protected Dialog onCreateDialog(int id)
	{
		if (id == 0)
		{
			// Show the number prompt
			mPrompt = new NumberPrompt(this, mNumberSetListener, mCellOptions);

			PreparePrompt();

			return mPrompt;
		}
		else if (id == 1)
		{
			// Show the login prompt
			LoginPrompt prompt = new LoginPrompt(this, mPlayerName, mPlayerPassword, mNameSetListener);
			return prompt;
		}
		else if(id==2)
		{
			//Prepare the listener for when the password is changed
			final PasswordChangedListener listener = new PasswordChangedListener(){

				public void OnPasswordChanged(List<SudokuGameTwoPlayer> gameList, String newPassword)
				{
					Log.d("", "Password changed");

					Toast t = Toast.makeText(mContext, "Password changed", Toast.LENGTH_SHORT);
					t.show();
					
					mPlayerPassword = newPassword;
					
					Editor editor = mPreferences.edit();
					editor.putString("PlayerPassword", mPlayerPassword);
					editor.commit();
					
					LoadMainMenu(gameList);
				}

				public void OnLoginFailed()
				{
					Log.d("", "Login failed");
					
					Toast t = Toast.makeText(mContext, "Login failed", Toast.LENGTH_SHORT);
					t.show();
					
					showDialog(1);
				}

				public void OnConnectionFailed()
				{
					Log.e("", "Connection failed");
					
					Toast t = Toast.makeText(mContext, "Connection failed", Toast.LENGTH_SHORT);
					t.show();
					
					showDialog(1);
				}};
			
			//Show the change password prompt
			ChangePasswordPrompt prompt = new ChangePasswordPrompt(mContext, mPlayerPassword, new OnSetPasswordListener()
			{
				public void onPasswordSet(ChangePasswordPrompt view, String oldPassword, String newPassword)
				{
					mClient.ChangePassword(mServer, mPlayerName, mPlayerPassword, newPassword, listener);
				}

				public void onCancelled()
				{
					LoadMainMenu(null);
				}
			});
			return prompt;
		}

		return null;
	}

	@Override
	public void onBackPressed()
	{
		if (mShowingMainMenu)
			finish();
		else
			LoadMainMenu(null);
		return;
	}

	@Override
    public boolean onCreateOptionsMenu(Menu menu)
	{
		MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
		return true;
	}
	
	@Override
    public boolean onOptionsItemSelected(MenuItem item)
	{
		// Handle item selection
        switch (item.getItemId())
        {
        case R.id.mainmenu_stats:
        	LoadPlayerStats();
            return true;
        case R.id.mainmenu_password:
        	showDialog(2);
        	return true;
        default:
            return super.onOptionsItemSelected(item);
        }
	}
	
	private void LoadMainMenu(List<SudokuGameTwoPlayer> gameList)
	{
		setContentView(R.layout.mainmenu);

		mShowingMainMenu = true;

		// Button easyButton = (Button) findViewById(R.id.buttonEasy);
		// easyButton.setOnClickListener(new StartButtonListener(false,
		// "Easy"));

		// Button hardButton = (Button) findViewById(R.id.buttonHard);
		// hardButton.setOnClickListener(new StartButtonListener(false,
		// "Hard"));

		try
		{
		    NotificationManager nMgr = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		    nMgr.cancel(1);
		}
		catch(Exception e)
		{
			Log.i("", "Tried removing notification and failed");
		}
		
		Button twoPlayerButton = (Button) findViewById(R.id.buttonTwoPlayer);
		twoPlayerButton.setOnClickListener(new StartButtonListener(true, null));

		mTwoPlayerList = (ListView) findViewById(R.id.twoPlayerList);
		mArrayAdapter = new ArrayAdapter<String>(mContext, android.R.layout.simple_list_item_1, mExistingGames);
		mTwoPlayerList.setAdapter(mArrayAdapter);
		mTwoPlayerList.setOnItemClickListener(new ResumeGameListener(mExistingGameIds));

		try
		{
			GCMRegistrar.checkDevice(this);
		}
		catch(Exception e)
		{
			DebugLog.Write("GCM Failed checking device: " + e.getMessage(), null);
		}
		try
		{
			GCMRegistrar.checkManifest(this);
		}
		catch(Exception e)
		{
			DebugLog.Write("GCM Failed checking manifest: " + e.getMessage(), null);
		}
		try
		{
			mRegisteredId = GCMRegistrar.getRegistrationId(this);
			if (mRegisteredId.equals(""))
			{
				Log.i("GCM", "Registering with GCM");
				GCMRegistrar.register(this, mSenderId);
				mRegisteredId = GCMRegistrar.getRegistrationId(this);
			} else
			{
				Log.i("GCM", "Already registered with ID " + mRegisteredId);
			}
		}
		catch(Exception e)
		{
			DebugLog.Write("GCM Failed getting registration ID: " + e.getMessage(), null);
		}

		if(gameList != null)
		{
			Toast.makeText(mContext, String.format("%d games found", gameList.size()), Toast.LENGTH_SHORT).show();
			
			mArrayAdapter.clear();
			// mExistingGames.clear();
			mExistingGameIds.clear();

			if(mClient.Failed)
				mArrayAdapter.add("Failed to get list");
			else
			{
				// Split the list into individual entries
				//Loop 3 times for "your turn", "their turn", and "game over"
				for(int j = 0; j<3; j++)
					for (int i = 0; i < gameList.size(); i++)
					{
						SudokuGameTwoPlayer game = gameList.get(i);
						if(j != 2 && game.Status == 2)
							continue;
						if(j == 0 && !game.IsLocalPlayerTurn(mPlayerName))
							continue;
						if(j == 1 && game.IsLocalPlayerTurn(mPlayerName))
							continue;
						if(j == 2 && game.Status != 2)
							continue;
						
						String entry = String.format(Locale.US, "%s vs. %s, started %s", game.GetPlayer1Name(), game.GetPlayer2Name(), new SimpleDateFormat("MM/dd/yyyy", Locale.US).format(game.StartDate.getTime()));
						if(game.Status == 2)
							entry += " (game over)";
						else if (game.IsLocalPlayerTurn(mPlayerName))
							entry += " (your turn)";
						mArrayAdapter.add(entry);
						mExistingGameIds.add(game.GameId);
					}
			}
		}
		else
		{
			mArrayAdapter.clear();
			mArrayAdapter.add("Getting games");
			
			// Start getting the list of existing games
			mClient.GetGameList(mServer, mPlayerName, mPlayerPassword, mRegisteredId, mGameListReadyListener);
		}
	}

	private void LoadGameMenu()
	{
		setContentView(R.layout.gameoptions);

		mShowingMainMenu = false;

		mPlayer2Text = (EditText) findViewById(R.id.inputPlayer2);

		mFillCenter = (CheckBox) findViewById(R.id.fillCenterCheck);
		mCellsToFillSpinner = (Spinner) findViewById(R.id.spinnerCellsToFill);
		//mHandSizeSpinner = (Spinner) findViewById(R.id.spinnerHandSize);
		mBonusCellsSpinner = (Spinner) findViewById(R.id.spinnerBonusCells);
		mScoringSystemSpinner = (Spinner) findViewById(R.id.spinnerScoringSystem);
		//mHandSystemSpinner = (Spinner) findViewById(R.id.spinnerHandSystem);
		mBonusSystemSpinner = (Spinner) findViewById(R.id.spinnerBonusSystem);
		mMultiplierSpinner = (Spinner) findViewById(R.id.spinnerMultiplierStrategy);

		// Setup the Cell-to-fill Spinner
		ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(this, android.R.layout.simple_spinner_item, mFillOptions);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mCellsToFillSpinner.setAdapter(adapter);
		mCellsToFillSpinner.setSelection(0);

		// Setup the hand size Spinner
		//adapter = new ArrayAdapter<CharSequence>(this, android.R.layout.simple_spinner_item, mHandSizeOptions);
		//adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		//mHandSizeSpinner.setAdapter(adapter);
		//mHandSizeSpinner.setSelection(1);

		// Setup the bonus cells Spinner
		adapter = new ArrayAdapter<CharSequence>(this, android.R.layout.simple_spinner_item, mBonusCellOptions);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mBonusCellsSpinner.setAdapter(adapter);
		mBonusCellsSpinner.setSelection(2);

		// Setup the Hand system Spinner
		//adapter = new ArrayAdapter<CharSequence>(this, android.R.layout.simple_spinner_item, mHandOptions);
		//adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		//mHandSystemSpinner.setAdapter(adapter);
		//mHandSystemSpinner.setSelection(1);
		
		// Setup the Hand system Spinner
		adapter = new ArrayAdapter<CharSequence>(this, android.R.layout.simple_spinner_item, mBonusOptions);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mBonusSystemSpinner.setAdapter(adapter);
		mBonusSystemSpinner.setSelection(1);

		// Setup the Scoring system Spinner
		adapter = new ArrayAdapter<CharSequence>(this, android.R.layout.simple_spinner_item, mScoringOptions);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mScoringSystemSpinner.setAdapter(adapter);
		mScoringSystemSpinner.setSelection(3);
		
		// Setup the Scoring system Spinner
		adapter = new ArrayAdapter<CharSequence>(this, android.R.layout.simple_spinner_item, mMultiplierOptions);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mMultiplierSpinner.setAdapter(adapter);
		mMultiplierSpinner.setSelection(0);

		Button startButton = (Button) findViewById(R.id.buttonStart);
		startButton.setOnClickListener(new OnClickListener()
		{
			public void onClick(View v)
			{
				String player2Name = mPlayer2Text.getText().toString().trim();
				
				if(player2Name.equals("") || player2Name.toLowerCase(Locale.US).equals(mPlayerName.toLowerCase(Locale.US)))
				{
					Toast t = Toast.makeText(mContext, "Enter valid name for Player 2", Toast.LENGTH_SHORT);
					t.show();
				}
				else
				{
					boolean fillCenter = mFillCenter.isChecked();
					int cellsToFill = Integer.parseInt(mFillOptions[mCellsToFillSpinner.getSelectedItemPosition()].toString());
					int handSize = 9;//Integer.parseInt(mHandSizeOptions[mHandSizeSpinner.getSelectedItemPosition()].toString());
					int bonusCells = Integer.parseInt(mBonusCellOptions[mBonusCellsSpinner.getSelectedItemPosition()].toString());
					String handSystem = "Vanilla";//mHandOptions[mHandSystemSpinner.getSelectedItemPosition()].toString();
					String bonusSystem = mBonusOptions[mBonusSystemSpinner.getSelectedItemPosition()].toString();
					String scoringSystem = mScoringOptions[mScoringSystemSpinner.getSelectedItemPosition()].toString();
					String multiplierSystem = mMultiplierOptions[mMultiplierSpinner.getSelectedItemPosition()].toString();
	
					StartTwoPlayerGame(mPlayerName, player2Name, fillCenter, cellsToFill, handSize, bonusCells, handSystem, scoringSystem, multiplierSystem, bonusSystem);
				}
			}
		});
	}

	private void LoadPlayerStats()
	{
		setContentView(R.layout.playerstats);

		mShowingMainMenu = false;
		
		mStatsView = (TextView) findViewById(R.id.playerStats);
		
		mStatsView.setText("Getting stats");
		
		mClient.GetPlayerStats(mServer, mPlayerName, mPlayerPassword, new StatsReadyListener()
		{
			public void OnStatsReady(PlayerStats stats)
			{
				String text = String.format(Locale.US, "Wins: %d\nLosses: %d\nStreak: %d", stats.Wins, stats.Losses, stats.Streak);
				mStatsView.setText(text);
			}

			public void OnLoginFailed()
			{
				Log.d("", "Login failed");
				
				Toast t = Toast.makeText(mContext, "Login failed", Toast.LENGTH_SHORT);
				t.show();
				
				showDialog(1);
			}

			public void OnConnectionFailed()
			{
				Log.e("", "Connection failed");
				
				Toast t = Toast.makeText(mContext, "Connection failed", Toast.LENGTH_SHORT);
				t.show();
				
				showDialog(1);
			}
		});
	}
	
	private void StartOnePlayerGame(String difficulty)
	{
		DebugLog.Write("Starting one player game... BROKEN", null);
		Toast t = Toast.makeText(this, "1 player mode broken", Toast.LENGTH_SHORT);
		t.show();
		// PrepareGame();

		// Start one player game
		// mGame = new SudokuGameOnePlayer(mSudoku, difficulty);
		// mGameText.setText("One player Sudoku!");

		// mClearButton.setVisibility(Button.INVISIBLE);
		// mConfirmButton.setVisibility(Button.INVISIBLE);
		// mShowButton.setVisibility(Button.INVISIBLE);
	}

	private void StartTwoPlayerGame(String player1Name, String player2Name, boolean fillCenter, int cellsToFill, int handSize, int bonusCells, String handSystem, String scoringSystem, String multiplierSystem, String bonusSystem)
	{
		DebugLog.Write("Starting two player game", null);

		PrepareGame();

		// Start two player game
		mGame = new SudokuGameTwoPlayer(mSudoku, player1Name, player2Name, fillCenter, cellsToFill, bonusCells, scoringSystem, handSystem, handSize, multiplierSystem, bonusSystem);
		DebugLog.Write(String.format(Locale.US, "Bonus cells: %d", bonusCells), null);
		//String cellsFilled = String.format(Locale.US, "Fill %d", cellsToFill);

		DebugLog.Write("Initial board:\n" + mGame.GetBoard().toString(), null);

		mDebugText.setText(CreateDebugString());

		mGameText.setText("Battle Sudoku!");
		mGame.UpdateScore(mGameScore, mPlayerName);

		mClearButton.setVisibility(Button.VISIBLE);
		mConfirmButton.setVisibility(Button.VISIBLE);
		mShowButton.setVisibility(Button.VISIBLE);

		DrawHand();

		DisablePendingButtons();
	}

	private void ResumeGame(int gameId)
	{
		mClient.GetGame(mServer, gameId, mPlayerName, mPlayerPassword, new GameReadyListener()
		{
			public void OnGameReady(SudokuGameTwoPlayer game)
			{
				DebugLog.Write("Resuming two player game", null);

				PrepareGame();

				// Resume the two player game
				mGame = game;

				mDebugText.setText(CreateDebugString());

				mGameText.setText("Battle Sudoku!");
				mGame.UpdateScore(mGameScore, mPlayerName);

				mClearButton.setVisibility(Button.VISIBLE);
				mConfirmButton.setVisibility(Button.VISIBLE);
				mShowButton.setVisibility(Button.VISIBLE);

				DisablePendingButtons();

				mSudoku.InitializeBoard(mGame.GetBoard(), mGame.GetPlayer1Color(mPlayerName), mGame.GetPlayer2Color(mPlayerName));

				DrawHand();
				
				if(mGame.Status == 2)
				{
					FinishGame(mGame.CreateEnding(mContext));
				}
			}

			public void OnLoginFailed()
			{
				Log.e("", "PROBLEM: Login failed while resuming game");
				
				Toast t = Toast.makeText(mContext, "Login failed", Toast.LENGTH_SHORT);
				t.show();
				
				showDialog(1);
			}

			public void OnConnectionFailed()
			{
				Log.e("", "PROBLEM: Connection failed while resuming game");
				
				Toast t = Toast.makeText(mContext, "Connection failed", Toast.LENGTH_SHORT);
				t.show();
				
				showDialog(1);
			}
		});
	}

	private void PrepareGame()
	{
		setContentView(R.layout.game);

		mShowingMainMenu = false;

		// Get the GUI elements and assign listeners
		mSudoku = (SudokuView) findViewById(R.id.sudoku);
		mSudoku.setOnTouchListener(new SudokuTouchListener());

		mGameText = (TextView) findViewById(R.id.gameIntro);

		mGameScore = (TextView) findViewById(R.id.gameScore);
		mGameScore.setText("");

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

	private String CreateDebugString()
	{
		return String.format("Hand %s, Score %s, Bonus %s, %s", mGame.GetHandSystem(), mGame.GetScoringSystem(), mGame.BonusSystem, mGame.GetMultiplierSystem());
	}
	
	private void MakeMove()
	{
		DebugLog.Write(String.format(Locale.US, "Committing move %d at (%d, %d)", mPendingValue, mPendingPoint.x, mPendingPoint.y), null);

		int oldPlayer = mGame.GetCurrentPlayer();

		AlertDialog.Builder builder = mGame.MakeMove(mContext, mSudoku, mPendingPoint, mPendingValue);
		if (builder != null)
		{
			mGame.Status = 2;
			
			FinishGame(builder);			
		}

		DrawHand();

		mCurrentPoint = null;
		mPendingPoint = null;
		mPendingValue = 0;

		UpdateBoard();

		// Toast t = Toast.makeText(mContext, "Uploading game to server",
		// Toast.LENGTH_SHORT);
		// t.show();
		if (mGame.GetCurrentPlayer() != oldPlayer || mGame.Status == 2)
		{
			Log.i("", "Sending game to server");
			mClient.UpdateGame(mServer, mGame, mPlayerName, mPlayerPassword, new GameUpdatedListener()
			{
				public void OnGameUpdated()
				{
					Log.i("", "Update complete");
					Toast t = Toast.makeText(mContext, "Update complete", Toast.LENGTH_SHORT);
					t.show();
				}

				public void OnLoginFailed()
				{
					Log.d("", "Login failed when updating game");
					
					Toast t = Toast.makeText(mContext, "Login failed", Toast.LENGTH_SHORT);
					t.show();
					
					showDialog(1);
				}

				public void OnConnectionFailed()
				{
					Log.e("", "Connection failed when updating game");
					
					Toast t = Toast.makeText(mContext, "Connection failed", Toast.LENGTH_SHORT);
					t.show();
					
					showDialog(1);
				}

			});
		}

	}

	private void FinishGame(AlertDialog.Builder builder)
	{
		builder.setPositiveButton("Menu", new DialogInterface.OnClickListener()
		{
			public void onClick(DialogInterface dialog, int id)
			{
				dialog.cancel();
				LoadMainMenu(null);
			}
		});

		DebugLog.Write("Final board:\n" + mGame.GetBoard().toString(), null);

		builder.create().show();
	}
	
	private void UpdateBoard()
	{
		// Always calling ShowMove() to update (possibly erase) a pending move
		// within the game
		mGame.ShowMove(mSudoku, mPendingPoint, mPendingValue);
		mGame.UpdateScore(mGameScore, mPlayerName);
		if (mShowingPossibilities)
		{
			mSudoku.ShowSquareOptions();
		}
	}

	private void DrawHand()
	{
		if (mGame != null && mGame.GetGamePhase() > 0 && mGame.IsLocalPlayerTurn(mPlayerName))//mGame.GetHand() != null)
		{
			List<Byte> hand = mGame.GetHand();
			if(hand != null)
			{
				String handText = "";
				for (int i = 0; i < hand.size(); i++)
				{
					if (i > 0)
						handText += ", ";
					handText += String.format("%d", hand.get(i));
				}
				DebugLog.Write("Drawing hand: " + handText, null);
				handText = "Hand: " + handText;
	
				mHandText.setText(handText);
				return;
			}
		}
		
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
		if (hand != null && mGame.GetGamePhase() > 0)
		{
			// Log.i("SudokuActivity", String.format("Hand: %s",
			// mHand.toString()));

			// Disable any options that aren't present in the player's hand
			for (int i = mCellOptions.size() - 1; i >= 0; i--)
				if (!hand.contains(mCellOptions.get(i)))
				{
					// Log.i("SudokuActivity", String.format("Disabling %d",
					// i));
					mCellOptions.remove(i);
				}
		}

		if (mPrompt != null)
			mPrompt.SetOptions(mCellOptions);
	}
	

	private NumberPrompt.OnNumberSetListener mNumberSetListener = new NumberPrompt.OnNumberSetListener()
	{
		public void onNumberSet(NumberPrompt view, int number)
		{
			if (number < 0)
				return;

			// In one player mode, a 0 will reset the tile (when the player
			// clicks cancel)
			if (mGame.GetNumberOfPlayers() > 1 && number == 0)
				return;

			mPendingPoint = mCurrentPoint;
			mPendingValue = (byte) number;

			if (mGame.GetConfirmCommit())
			{
				DebugLog.Write(String.format(Locale.US, "Showing move %d at (%d, %d)", mPendingValue, mPendingPoint.x, mPendingPoint.y), null);

				UpdateBoard();

				EnablePendingButtons();
			} else
			{
				MakeMove();
			}
		}
	};


	private LoginPrompt.OnNameSetListener mNameSetListener = new LoginPrompt.OnNameSetListener()
	{
		public void onNameSet(LoginPrompt view, String name, String password)
		{
			mPlayerName = name;
			mPlayerPassword = password;

			Log.d("", "Logging in");
			
			// Attempt to login
			mClient.GetGameList(mServer, mPlayerName, mPlayerPassword, mRegisteredId, mGameListReadyListener);
		}

		public void onNewPlayer(LoginPrompt view, String name, String password)
		{
			mPlayerName = name;
			mPlayerPassword = password;

			Log.d("", "Creating new player");
			
			// Attempt to create player
			mClient.CreatePlayer(mServer, mPlayerName, mPlayerPassword, mRegisteredId, new CreatePlayerListener()
			{
				public void OnPlayerCreated()
				{
					Log.d("", "Create player succeeded");

					Editor editor = mPreferences.edit();
					editor.putString("PlayerName", mPlayerName);
					editor.putString("PlayerPassword", mPlayerPassword);
					editor.commit();
					
					LoadMainMenu(new ArrayList<SudokuGameTwoPlayer>());
				}

				public void OnCreateFailed()
				{
					Log.d("", "Failed to create player");
					
					Toast t = Toast.makeText(mContext, "Player already exists", Toast.LENGTH_SHORT);
					t.show();
					
					showDialog(1);
				}

				public void OnConnectionFailed()
				{
					Log.e("", "Connection failed");
					
					Toast t = Toast.makeText(mContext, "Connection failed", Toast.LENGTH_SHORT);
					t.show();
					
					showDialog(1);
				}
			});
		}
		
		public void onCancelled()
		{
			finish();
		}
	};

	private GameListReadyListener mGameListReadyListener = new GameListReadyListener()
	{
		public void OnGameListReady(List<SudokuGameTwoPlayer> gameList)
		{
			Log.d("", "Login succeeded");

			Editor editor = mPreferences.edit();
			editor.putString("PlayerName", mPlayerName);
			editor.putString("PlayerPassword", mPlayerPassword);
			editor.commit();
			
			LoadMainMenu(gameList);
		}

		public void OnLoginFailed()
		{
			Log.d("", "Login failed");
			
			Toast t = Toast.makeText(mContext, "Login failed", Toast.LENGTH_SHORT);
			t.show();
			
			showDialog(1);
		}

		public void OnConnectionFailed()
		{
			Log.e("", "Connection failed");
			
			Toast t = Toast.makeText(mContext, "Connection failed", Toast.LENGTH_SHORT);
			t.show();
			
			showDialog(1);
		}
	};

	// Different start buttons are used to select the number of players and difficulty
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
			if (mTwoPlayer)
				LoadGameMenu();
			else
				StartOnePlayerGame(mDifficulty);
		}
	}

	// This class responds when the user chooses to play an existing 2-player game
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

	// This class responds when the user clicks the Sudoku board
	private class SudokuTouchListener implements OnTouchListener
	{
		public boolean onTouch(View arg0, MotionEvent arg1)
		{
			DebugLog.Write("User clicked board", null);
			// Ignore the click if we're in the possibility view OR it's not the
			// local player's turn
			if (mShowingPossibilities)
			{
				DebugLog.Write("Ignoring board click, showing possibilities", null);
				return false;
			}
			if (!mGame.IsLocalPlayerTurn(mPlayerName))
			{
				DebugLog.Write("Ignoring board click, not this player's turn", null);
				return false;
			}

			// Determine which cell was clicked
			Point currentPoint = mSudoku.GetCell(arg1.getX(), arg1.getY());
			DebugLog.Write(String.format(Locale.US, "Clicked cell (%d, %d)", currentPoint.x, currentPoint.y), null);
			Log.i("SudokuActivity", String.format("Clicked box (%d, %d)", currentPoint.x, currentPoint.y));

			// Make sure we got a legal cell value
			if (currentPoint.x < 0 || currentPoint.x >= SudokuBoard.BoardSize || currentPoint.y < 0 || currentPoint.y >= SudokuBoard.BoardSize)
				return false;

			// Show the number prompt if the cell clicked is valid in the
			// current game
			if (mGame.HandleClick(currentPoint))
			{
				mCurrentPoint = currentPoint;

				PreparePrompt();

				showDialog(0);
			}

			return false;
		}
	}

	// This button clears a pending move
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

	// This button commits a move
	private class ConfirmButtonListener implements OnClickListener
	{
		public void onClick(View v)
		{
			MakeMove();

			DisablePendingButtons();
		}
	}

	// This button toggles the available numbers for a square or the full board
	private class ShowButtonListener implements OnClickListener
	{
		public void onClick(View v)
		{
			mShowingPossibilities = !mShowingPossibilities;

			if (mShowingPossibilities)
				DebugLog.Write("Showing possibilities", null);
			else
				DebugLog.Write("Showing main board", null);

			UpdateBoard();
		}
	}

}