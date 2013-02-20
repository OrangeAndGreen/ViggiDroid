package com.Dave.Sudoku;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Point;
import android.widget.TextView;

public interface ISudokuGame
{
	int GetNumberOfPlayers();
	
	int GetPlayer1Color();
	
	int GetPlayer2Color();
	
	int GetCurrentPlayer();
	
	void StartGame(SudokuView view, String difficulty, String player1Name, String player2Name);
	
	int[][] GetFullBoard();
	
	boolean HandleClick(Point point);
	
	boolean GetConfirmCommit();
	
	boolean ShowMove(SudokuView view, Point point, int number);
	
	AlertDialog.Builder MakeMove(Context context, SudokuView view, Point point, int number);
	
	void UpdateScore(TextView view);
}
