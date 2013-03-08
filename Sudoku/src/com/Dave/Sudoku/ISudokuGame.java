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
	
	int GetGamePhase();
	
	SudokuBoard GetBoard();
	
	boolean HandleClick(Point point);
	
	boolean GetConfirmCommit();
	
	void ShowMove(SudokuView view, Point point, byte number, IScoring scoring);
	
	AlertDialog.Builder MakeMove(Context context, SudokuView view, Point point, byte number, IScoring scoring);
	
	void UpdateScore(TextView view);
}
