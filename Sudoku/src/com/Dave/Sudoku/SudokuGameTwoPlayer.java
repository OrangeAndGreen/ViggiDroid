package com.Dave.Sudoku;

import java.util.Calendar;
import java.util.List;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Point;
import android.text.Spannable;
import android.text.style.BackgroundColorSpan;
import android.util.Log;
import android.widget.TextView;

public class SudokuGameTwoPlayer
{
	private static final int mNumberOfPlayers = 2;
	
	private SudokuBoard Board = null;
	
	private IScoring mScoring = null;
	private IHand mHand = null;
	
	private int Player1Score = 0;
	private int Player2Score = 0;
	private int mProposedScore = 0;
	private int mProposedMultiplier = 0;
	private int mCurrentMultiplier = 0;
	
	private String Player1Name = null;
	private String Player2Name = null;
	
	private int GamePhase = 0;
	private int PlayerTurn = 0;
	
	public int GameId = -1;
	public Calendar StartDate = null;
	public Calendar PlayDate = null;
	public int Active = 0;
	
	//public String HandSystem = null;
	public int HandSize = 0;
	//public String ScoringSystem = null;
	
	private SudokuGameTwoPlayer()
	{
		
	}
	
	public SudokuGameTwoPlayer(SudokuView view, String player1Name, String player2Name, int cellsToFill, int bonusCells, String scoringSystem, String handSystem, int handSize)
	{
		Player1Name = player1Name;
		Player2Name = player2Name;
		
		Board = SudokuBoard.Create(cellsToFill, true, bonusCells);
		//Board = SudokuBoard.Create(-1, true);
		
		mScoring = CreateScoring(scoringSystem);
    	DebugLog.Write("Scoring: " + mScoring.GetName(), null);
		
    	mHand = CreateHand(handSystem, handSize);
    	DebugLog.Write("Hand: " + mHand.GetName(), null);
    	
		view.InitializeBoard(Board, GetPlayer1Color(), GetPlayer2Color());
	}
	
	public static SudokuGameTwoPlayer FromString(String input, boolean infoOnly)
	{
		SudokuGameTwoPlayer game = new SudokuGameTwoPlayer();
		
		Log.d("SudokuGameTwoPlayer", input);
		
		String[] parts = input.split(",");
		
		game.GameId = Integer.parseInt(parts[0]);
		game.Player1Name = parts[1];
		game.Player1Score = Integer.parseInt(parts[2]);
		game.Player2Name = parts[3];
		game.Player2Score = Integer.parseInt(parts[4]);
		game.StartDate = ParseCalendar(parts[5]);
		game.PlayDate = ParseCalendar(parts[6]);
		game.PlayerTurn = Integer.parseInt(parts[7].trim());
		game.Active = Integer.parseInt(parts[8].trim());
		
		if(!infoOnly)
		{
			//game.HandSystem = parts[9];
			game.HandSize = Integer.parseInt(parts[10].trim());
			//game.ScoringSystem = parts[11];
			
			String startingBoard = parts[12];
			String playerBoard = parts[13];
			String multipliers = parts[14];
			
			game.Board = new SudokuBoard(startingBoard, playerBoard, multipliers);
			game.mScoring = CreateScoring(parts[11]);
			game.mHand = CreateHand(parts[9], game.HandSize);
		}
		
		return game;
	}
	
	public static IScoring CreateScoring(String scoringSystem)
	{
		IScoring ret = null;
		
		if(scoringSystem != null && scoringSystem.equals("System 1"))
    	{
			ret = new ScoringConcept1();
    	}
    	else if(scoringSystem != null && scoringSystem.equals("System 2"))
    	{
    		ret = new ScoringConcept2();
    	}
    	else if(scoringSystem != null && scoringSystem.equals("Least square"))
    	{
    		ret = new ScoringConcept3();
    	}
    	else
    	{
    		ret = new ScoringVanilla();
    	}
		
		return ret;
	}
	
	public static IHand CreateHand(String handSystem, int handSize)
	{
		IHand ret = null;
		
		if(handSystem != null && handSystem.equals("Concept 1"))
			ret = new HandConcept1();
    	else
    		ret = new HandVanilla();
    	
		ret.SetHandSize(handSize);
    	
    	return ret;
	}
	
	private static Calendar ParseCalendar(String input)
	{
		Calendar ret = Calendar.getInstance();
		
		String[] parts = input.split(":");
		
		ret.set(Calendar.YEAR, Integer.parseInt(parts[0]));
		ret.set(Calendar.MONTH, Integer.parseInt(parts[1]) - 1);
		ret.set(Calendar.DAY_OF_MONTH, Integer.parseInt(parts[2]) - 1);
		ret.set(Calendar.HOUR, Integer.parseInt(parts[3]));
		ret.set(Calendar.MINUTE, Integer.parseInt(parts[4]));
		ret.set(Calendar.SECOND, Integer.parseInt(parts[5]));
		
		return ret;
	}

	public String GetScoringSystem()
	{
		return mScoring.GetName();
	}
	
	public String GetHandSystem()
	{
		return mHand.GetName();
	}
	
	public List<Byte> GetHand()
	{
		if(mHand == null)
			return null;
		
		return mHand.GetHand(GetBoard(), GetCurrentPlayer());
	}
	
	public int GetNumberOfPlayers()
	{
		return mNumberOfPlayers;
	}
	
	public String GetPlayer1Name()
	{
		return Player1Name;
	}
	
	public int GetPlayer1Score()
	{
		return Player1Score;
	}
	
	public int GetPlayer1Color()
	{
		return Color.rgb(79, 129, 189);
	}
	
	public String GetPlayer2Name()
	{
		return Player2Name;
	}
	
	public int GetPlayer2Score()
	{
		return Player2Score;
	}
	
	public int GetPlayer2Color()
	{
		return Color.rgb(149, 55, 53);
	}
	
	public int GetCurrentPlayer()
	{
		return PlayerTurn;
	}
	
	public boolean IsLocalPlayerTurn(String localPlayer)
	{
		int localPlayerIndex = 0;
		if(localPlayer.equals(Player2Name))
			localPlayerIndex = 1;
		
		return PlayerTurn == localPlayerIndex;
	}
	
	public SudokuBoard GetBoard()
	{
		return Board;
	}
	
	public int GetGamePhase()
	{
		return GamePhase;
	}

	public boolean HandleClick(Point cell)
	{
		//The cell must be blank
		if(Board.GetCell(cell, false) != 0)
			return false;
		
		Point square = SudokuBoard.GetSquare(cell);
		
		if(GamePhase == 0)
		{
			//Cell must be in the center square
			if(square.x != 1 || square.y != 1)
			{
				Log.i("SudokuGameTwoPlayer", "Not in center square during startup phase");
				return false;
			}
		}
		else
		{
			boolean goodSquare = false;
			//Cell must be in the current player's territory
			if(PlayerTurn == 0)
			{
				if(square.x == 0 && square.y == 0)
					goodSquare = true;
				if(square.x == 1 && square.y == 0)
					goodSquare = true;
				if(square.x == 2 && square.y == 1)
					goodSquare = true;
				if(square.x == 0 && square.y == 2)
					goodSquare = true;
			}
			else
			{
				if(square.x == 2 && square.y == 0)
					goodSquare = true;
				if(square.x == 0 && square.y == 1)
					goodSquare = true;
				if(square.x == 1 && square.y == 2)
					goodSquare = true;
				if(square.x == 2 && square.y == 2)
					goodSquare = true;
			}
			if(!goodSquare)
			{
				Log.i("SudokuGameTwoPlayer", "Not in player's territory");
				return false;
			}
		}
		
		return true;
	}

	public boolean GetConfirmCommit()
	{
		return true;
	}
	
	public void ShowMove(SudokuView view, Point point, byte number)
	{
		if(GamePhase == 1)
		{
			mProposedScore = mScoring.ScoreMove(Board, point, number, mCurrentMultiplier);
			mProposedMultiplier = mScoring.GetNextMultiplier(Board, point, number, mCurrentMultiplier);
		}
		
		UpdateBoard(view, point, number, true);
	}
	
	public AlertDialog.Builder MakeMove(Context context, SudokuView view, Point point, byte number)
	{
		if(mHand != null && GetGamePhase() > 0)
    	{
    		mHand.TakeNumber(GetBoard(), GetCurrentPlayer(), number);
    	}
		
		//Update the current player's score
		if(GamePhase == 1)
		{
			int score = mScoring.ScoreMove(Board, point, number, mCurrentMultiplier);
			//DebugLog.Write(String.format("Move scores %d", score), null);
			if(PlayerTurn == 0)
				Player1Score += score;
			else
				Player2Score += score;
		}
		
		int oldMultiplier = mCurrentMultiplier;
		mCurrentMultiplier = mScoring.GetNextMultiplier(Board, point, number, mCurrentMultiplier);
		DebugLog.Write(String.format("Turn multiplier: %d", mCurrentMultiplier), null);
		//TODO: Need a better way to tell when the turn is over. The multiplier should be able to stay the same for multiple turns.
		if(mCurrentMultiplier == 0 || mCurrentMultiplier <= oldMultiplier)
		{
			//Make it the other player's turn
			PlayerTurn = 1 - PlayerTurn;
			mCurrentMultiplier = 0;
			
			//Make sure the other player can move
			
			if(!CanPlayerMove(Board.GetFullBoard(true), PlayerTurn))
			{
				//Go back to the current player's turn
				PlayerTurn = 1 - PlayerTurn;
				DebugLog.Write(String.format("Player %d goes again", PlayerTurn + 1), null);
			}
			else
				DebugLog.Write(String.format("Now player %d's turn", PlayerTurn + 1), null);
		}
		
		mProposedScore = 0;
		mProposedMultiplier = 0;
		UpdateBoard(view, point, number, false);
		
		//See if we need to move from the intro phase to the main phase
		if(GamePhase == 0 && Board.CheckSquare(new Point(1, 1)))
		{
			DebugLog.Write("Moving to main phase of game", null);
			GamePhase = 1;
		}
		
		//See if the game is over
		if(Board.CheckBoard(false))
		{
			Active = 0;
			return CreateEnding(context);
		}
		
		return null;
	}
	
	private void UpdateBoard(SudokuView view, Point point, byte number, boolean justShow)
	{
		Board.SetCell(point, number, PlayerTurn, !justShow);
		
		//Draw the updated board
		//Point[] highlightPoints = null;
		//if(justShow)
		//{
		//	highlightPoints = new Point[1];
		//	highlightPoints[0] = new Point(point.x, point.y);
		//}
		view.UpdateBoard();
	}
	
	private boolean CanPlayerMove(byte[][] fullBoard, int player)
	{
		Point[] playerSquares = new Point[4];
		
		if(player == 0)
		{
			playerSquares[0] = new Point(0, 0);
			playerSquares[1] = new Point(1, 0);
			playerSquares[2] = new Point(2, 1);
			playerSquares[3] = new Point(0, 2);
		}
		else
		{
			playerSquares[0] = new Point(2, 0);
			playerSquares[1] = new Point(0, 1);
			playerSquares[2] = new Point(1, 2);
			playerSquares[3] = new Point(2, 2);
		}
		
		for(int square=0; square<4; square++)
		{
			for(int x=0; x<3; x++)
				for(int y=0; y<3; y++)
					if(fullBoard[playerSquares[square].x * 3 + x][playerSquares[square].y * 3 + y] == 0)
						return true;
		}
		
		return false;
	}
	
	private AlertDialog.Builder CreateEnding(Context context)
	{
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		if(Player1Score == Player2Score)
		{
			builder.setTitle("Tie game!");
			builder.setMessage("Same score! Even Steven! Split down the middle!\nNobody wins! Nobody loses!");
		}
		else
		{
			String winner = Player2Name;
			if(Player1Score > Player2Score)
				winner = Player1Name;
			
			builder.setTitle(winner + " wins!");
			builder.setMessage(String.format("%s wins ! Hooray for %s! %s is the champion!", winner, winner, winner));
			
		}
		builder.setCancelable(true);
		
		builder.setPositiveButton("Menu", new DialogInterface.OnClickListener()
		{
			public void onClick(DialogInterface dialog,int id)
			{
				// if this button is clicked, close
				// current activity
				//MainActivity.this.finish();
				dialog.cancel();
			}
		  });
		builder.setNegativeButton("Admire", new DialogInterface.OnClickListener()
		{
			public void onClick(DialogInterface dialog,int id)
			{
				// if this button is clicked, just close
				// the dialog box and do nothing
				dialog.cancel();
			}
		});
		
		// create alert dialog
		return builder;
	}
	
	public void UpdateScore(TextView view, String localPlayer)
	{
		String player1String = String.format("%s: %d", Player1Name, Player1Score);
		String player2String = String.format("\n%s: %d", Player2Name, Player2Score);
		
		int player1Background = Color.TRANSPARENT;
		int player2Background = Color.TRANSPARENT;
		
		String proposedScore = " (your turn";
		if(!IsLocalPlayerTurn(localPlayer))
			proposedScore = " (their turn";
		
		if(mCurrentMultiplier > 1)
			proposedScore += String.format(" with %dx bonus", mCurrentMultiplier);
		proposedScore += ")";
		if(mProposedScore != 0)
		{
			proposedScore = String.format(" (+%d", mProposedScore);
			if(mProposedMultiplier > mCurrentMultiplier)
				proposedScore += String.format(" and %dx bonus turn", mProposedMultiplier);
			proposedScore += ")";
		}
		if(GetCurrentPlayer() == 0)
		{
			player1Background = GetPlayer1Color();
			player1String += proposedScore;
		}
		else
		{
			player2Background = GetPlayer2Color();
			player2String += proposedScore;
		}
		
		CharSequence text = player1String + player2String;
		view.setText("");
		view.append(text);
		
		//CharSequence charText = view.getText();
		Spannable spannableText = (Spannable) view.getText();
        spannableText.setSpan(new BackgroundColorSpan(player1Background), 0, player1String.length(), 0);
        spannableText.setSpan(new BackgroundColorSpan(player2Background), player1String.length(), player1String.length() + player2String.length(), 0);
	}

}
