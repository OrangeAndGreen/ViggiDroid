package com.Dave.Sudoku;

import android.graphics.Point;

public interface IScoring
{
	String GetName();
	
	int ScoreMove(SudokuBoard board, Point point, byte number, int multiplier);
	
	int GetNextMultiplier(SudokuBoard board, Point point, byte number, int currentMultiplier);
}
