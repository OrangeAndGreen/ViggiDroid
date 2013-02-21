package com.Dave.Sudoku;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Point;
import android.text.Spannable;
import android.text.style.BackgroundColorSpan;
import android.util.Log;
import android.widget.TextView;

public class SudokuGameTwoPlayer implements ISudokuGame
{
	private static final int mNumberOfPlayers = 2;
	
	public int[][] InitialBoard = null;
	public int[][] Player1Board = null;
	public int[][] Player2Board = null;
	
	public int Player1Score = 0;
	public int Player2Score = 0;
	private int mProposedScore = 0;
	
	public String Player1Name = null;
	public String Player2Name = null;
	
	public int GamePhase = 0;
	public int PlayerTurn = 0;

	public int GetNumberOfPlayers()
	{
		return mNumberOfPlayers;
	}
	
	public int GetPlayer1Color()
	{
		return Color.rgb(79, 129, 189);
	}
	
	public int GetPlayer2Color()
	{
		return Color.rgb(149, 55, 53);
	}
	
	public int GetCurrentPlayer()
	{
		return PlayerTurn;
	}
	
	public void StartGame(SudokuView view, String difficulty, String player1Name, String player2Name)
	{
		Player1Name = player1Name;
		Player2Name = player2Name;
		
		InitialBoard = SudokuLogic.CreateBoard(10, true);
		Player1Board = SudokuLogic.CreateBoard(0);
		Player2Board = SudokuLogic.CreateBoard(0);
		
		//InitialBoard[4][4] = 9;
		
		view.InitializeBoard(InitialBoard, GetPlayer1Color(), GetPlayer2Color());
	}
	
	public int[][] GetFullBoard()
	{
		return SudokuLogic.GetFullBoard(InitialBoard, Player1Board, Player2Board);
	}

	public boolean HandleClick(Point cell)
	{
		//The cell must be blank
		if(InitialBoard[cell.x][cell.y] != 0)
			return false;
		if(Player1Board[cell.x][cell.y] != 0)
			return false;
		if(Player2Board[cell.x][cell.y] != 0)
			return false;
		
		Point square = SudokuLogic.GetSquare(cell);
		
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
	
	public void ShowMove(SudokuView view, Point point, int number, IScoring scoring)
	{
		int[][] fullBoard = SudokuLogic.GetFullBoard(InitialBoard, Player1Board, Player2Board);
		if(GamePhase == 1)
			mProposedScore = scoring.ScoreMove(fullBoard, point, number, 1);
		
		UpdateBoard(view, point, number, true);
	}
	
	public AlertDialog.Builder MakeMove(Context context, SudokuView view, Point point, int number, IScoring scoring)
	{
		UpdateBoard(view, point, number, false);
		
		int[][] fullBoard = SudokuLogic.GetFullBoard(InitialBoard, Player1Board, Player2Board);
		
		//Update the current player's score
		if(GamePhase == 1)
		{
			int score = scoring.ScoreMove(fullBoard, point, number, 1);
			if(PlayerTurn == 0)
				Player1Score += score;
			else
				Player2Score += score;
			mProposedScore = 0;
		}
		
		int multiplier = scoring.GetNextMultiplier(fullBoard, point, number);
		if(multiplier <= 0)
		{
			//Make it the other player's turn
			PlayerTurn = 1 - PlayerTurn;
			
			//Make sure the other player can move
			
			if(!CanPlayerMove(fullBoard, PlayerTurn))
			{
				//Go back to the current player's turn
				PlayerTurn = 1 - PlayerTurn;
			}
		}
		
		//See if we need to move from the intro phase to the main phase
		if(GamePhase == 0 && SudokuLogic.CheckSquare(fullBoard, 1, 1))
		{
			GamePhase = 1;
		}
		
		//See if the game is over
		if(SudokuLogic.CheckBoard(InitialBoard, Player1Board, Player2Board, false))
		{
			return CreateEnding(context);
		}
		
		return null;
	}
	
	private void UpdateBoard(SudokuView view, Point point, int number, boolean justShow)
	{
		int[][] tempPlayer1Board = Player1Board;
		int[][] tempPlayer2Board = Player2Board;
		
		if(justShow)
		{
			tempPlayer1Board = SudokuLogic.CreateBoard(Player1Board);
			tempPlayer2Board = SudokuLogic.CreateBoard(Player2Board);
		}
		
		//Update the current player's board and score
		if(PlayerTurn == 0)
		{
			tempPlayer1Board[point.x][point.y] = number;
		}
		else
		{
			tempPlayer2Board[point.x][point.y] = number;
		}
				
		int[][] fullBoard = SudokuLogic.GetFullBoard(InitialBoard, tempPlayer1Board, tempPlayer2Board);
				
		//boolean[] options = SudokuLogic.GetOptions(fullBoard, new Point(0, 3));
		//int numOptions = 0;
		//for(int i=0; i<options.length; i++)
		//	if(options[i])
		//		numOptions++;
		//Log.i("", String.format("%d options", numOptions));
				
		//Set invalid cells
		for(int x=0; x<SudokuLogic.BoardSize; x++)
			for(int y=0; y<SudokuLogic.BoardSize; y++)
				if(fullBoard[x][y] == 0 && !SudokuLogic.IsSquareValid(fullBoard, new Point(x, y)))
				{
					Log.i("SudokuGameTwoPlayer", String.format("Setting cell (%d, %d) invalid", x, y));
					if(SudokuLogic.GetPlayerTerritory(new Point(x, y)) == 0)
						tempPlayer1Board[x][y] = -1;
					else
						tempPlayer2Board[x][y] = -1;
				}
				
		//Draw the updated board
		Point highlightPoint = null;
		if(justShow)
			highlightPoint = point;
		view.UpdateBoard(tempPlayer1Board, tempPlayer2Board, highlightPoint);
	}
	
	private boolean CanPlayerMove(int[][] fullBoard, int player)
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
	
	public void UpdateScore(TextView view)
	{
		String player1String = String.format("%s: %d", Player1Name, Player1Score);
		String player2String = String.format("\n%s: %d", Player2Name, Player2Score);
		
		int player1Background = Color.TRANSPARENT;
		int player2Background = Color.TRANSPARENT;
		
		String proposedScore = " (your turn)";
		if(mProposedScore != 0)
			proposedScore = String.format(" (+%d)", mProposedScore);
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
