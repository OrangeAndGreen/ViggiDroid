package com.Dave.Sudoku;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Point;
import android.widget.TextView;

public class SudokuGameOnePlayer implements ISudokuGame
{
	private static final int mNumberOfPlayers = 1;
	
	String Player1Name = null;
	
	public int[][] InitialBoard = null;
	public int[][] PlayerBoard = null;

	public int GetNumberOfPlayers()
	{
		return mNumberOfPlayers;
	}
	
	public int GetCurrentPlayer()
	{
		return 0;
	}
	
	public void StartGame(SudokuView view, String difficulty, String player1Name, String player2Name)
	{
		Player1Name = player1Name;
		
		//Create the starting board
        if(difficulty == null || difficulty.equals("Hard"))
        	InitialBoard = SudokuLogic.CreateBoard(20);
        else
        	InitialBoard = SudokuLogic.CreateBoard(40);
        
        //Allocate the player board
        PlayerBoard = new int[SudokuLogic.BoardSize][];
        for(int i=0; i<SudokuLogic.BoardSize; i++)
        	PlayerBoard[i] = new int[SudokuLogic.BoardSize];
        
        //Fill in a couple entries (for testing)
        //PlayerBoard[2][0] = 1;
        //PlayerBoard[3][5] = 2;
        //PlayerBoard[7][2] = 8;
        
        view.InitializeBoard(InitialBoard, false);
        //mSudoku.UpdateBoard(PlayerBoard);
	}
	
	public int[][] GetFullBoard()
	{
		return SudokuLogic.GetFullBoard(InitialBoard, PlayerBoard, null);
	}

	public boolean HandleClick(Point point)
	{
		//Don't show the dialog if the user clicked in a tile with an initial value
		if(InitialBoard[point.x][point.y] <= 0)
			return true;
		
		return false;
	}

	public AlertDialog.Builder MakeMove(Context context, SudokuView view, Point point, int number)
	{
		PlayerBoard[point.x][point.y] = number;
		view.UpdateBoard(PlayerBoard);
		
		if(SudokuLogic.CheckBoard(InitialBoard, PlayerBoard, null, true))
		{
			return CreateEnding(context);
		}
		
		return null;
	}
	
	private AlertDialog.Builder CreateEnding(Context context)
	{
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		
		builder.setTitle("You win!");
		builder.setMessage("Holy frijole! You just solved one of the hardest Sudoku puzzles known to man. You must be some kind of sorcerer!");
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
		view.setText("");
	}
}
