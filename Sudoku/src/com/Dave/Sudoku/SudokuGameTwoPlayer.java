package com.Dave.Sudoku;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Point;
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
	
	public String Player1Name = null;
	public String Player2Name = null;
	
	public int GamePhase = 0;
	public int PlayerTurn = 0;

	public int GetNumberOfPlayers()
	{
		return mNumberOfPlayers;
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
		
		view.InitializeBoard(InitialBoard, true);
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

	public AlertDialog.Builder MakeMove(Context context, SudokuView view, Point point, int number)
	{
		//Update the current player's board and score
		if(PlayerTurn == 0)
		{
			Player1Board[point.x][point.y] = number;
			if(GamePhase == 1)
				Player1Score += number;
		}
		else
		{
			Player2Board[point.x][point.y] = number;
			if(GamePhase == 1)
				Player2Score += number;
		}
		
		int[][] fullBoard = SudokuLogic.GetFullBoard(InitialBoard, Player1Board, Player2Board);
		
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
						Player1Board[x][y] = -1;
					else
						Player2Board[x][y] = -1;
				}
		
		//Draw the updated board
		view.UpdateBoard(Player1Board, Player2Board);
		
		//Make it the other player's turn
		PlayerTurn = 1 - PlayerTurn;
		
		//Make sure the other player can move
		fullBoard = SudokuLogic.GetFullBoard(InitialBoard, Player1Board, Player2Board);
		if(!CanPlayerMove(fullBoard, PlayerTurn))
		{
			//Go back to the current player's turn
			PlayerTurn = 1 - PlayerTurn;
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
		String text = String.format("Player 1: %d\nPlayer 2: %d", Player1Score, Player2Score);
		view.setText(text);
	}

}
