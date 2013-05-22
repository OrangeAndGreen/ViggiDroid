package com.Dave.Sudoku.Game;

import com.Dave.Sudoku.SudokuBoard;
import com.Dave.Sudoku.SudokuView;
import com.Dave.Sudoku.Scoring.IScoring;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Point;
import android.widget.TextView;

public class SudokuGameOnePlayer implements ISudokuGame
{
	private static final int mNumberOfPlayers = 1;
	
	private SudokuBoard mBoard = null;
	
	public SudokuGameOnePlayer(SudokuView view, String difficulty)
	{
		//Create the starting board
        if(difficulty == null || difficulty.equals("Hard"))
        	mBoard = SudokuBoard.Create(false, 20, false, 0);
        else
        	mBoard = SudokuBoard.Create(false, 40, false, 0);
        
        //Fill in a couple entries (for testing)
        //PlayerBoard[2][0] = 1;
        //PlayerBoard[3][5] = 2;
        //PlayerBoard[7][2] = 8;
        
        view.InitializeBoard(mBoard, null, null);
        //mSudoku.UpdateBoard(PlayerBoard);
	}
	
	public int GetNumberOfPlayers()
	{
		return mNumberOfPlayers;
	}
	
	public String GetPlayer1Name()
	{
		return "Player 1";
	}
	
	public int GetPlayer1Score()
	{
		return 0;
	}
	
	public int GetPlayer1Color()
	{
		return Color.BLACK;
	}
	
	public String GetPlayer2Name()
	{
		return "";
	}
	
	public int GetPlayer2Score()
	{
		return 0;
	}
	
	public int GetPlayer2Color()
	{
		return Color.BLACK;
	}
	
	public int GetCurrentPlayer()
	{
		return 0;
	}
	
	public boolean IsLocalPlayerTurn(String localPlayer)
	{
		return true;
	}
	
	public int GetGamePhase()
	{
		return 1;
	}
	
	public SudokuBoard GetBoard()
	{
		return mBoard;
	}

	public boolean HandleClick(Point point)
	{
		//Don't show the dialog if the user clicked in a tile with an initial value
		if(mBoard.GetSubBoard(-1)[point.x][point.y] <= 0)
			return true;
		
		return false;
	}

	public boolean GetConfirmCommit()
	{
		return false;
	}
	
	public void ShowMove(SudokuView view, Point point, byte number, IScoring scoring)
	{
		//Do nothing
	}
	
	public AlertDialog.Builder MakeMove(Context context, SudokuView view, Point point, byte number, IScoring scoring)
	{
		mBoard.SetCell(point, number, 0, true);
		view.UpdateBoard();
		
		if(mBoard.CheckBoard(true))
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
