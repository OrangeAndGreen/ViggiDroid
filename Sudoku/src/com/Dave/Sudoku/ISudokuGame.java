package com.Dave.Sudoku;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Point;
import android.widget.TextView;

public interface ISudokuGame
{
	int GetNumberOfPlayers();
	
	String GetPlayer1Name();
	
	int GetPlayer1Score();
	
	int GetPlayer1Color();
	
	String GetPlayer2Name();
	
	int GetPlayer2Score();
	
	int GetPlayer2Color();
	
	int GetCurrentPlayer();
	
	boolean IsLocalPlayerTurn(String localPlayer);
	
	int GetGamePhase();
	
	SudokuBoard GetBoard();
	
	boolean HandleClick(Point point);
	
	boolean GetConfirmCommit();
	
	void ShowMove(SudokuView view, Point point, byte number, IScoring scoring);
	
	AlertDialog.Builder MakeMove(Context context, SudokuView view, Point point, byte number, IScoring scoring);
	
	void UpdateScore(TextView view);
}
