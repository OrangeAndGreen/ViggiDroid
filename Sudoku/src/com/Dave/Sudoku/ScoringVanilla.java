package com.Dave.Sudoku;

import android.graphics.Point;

public class ScoringVanilla implements IScoring
{
	public String GetName()
	{
		return "Vanilla";
	}
	
	public int ScoreMove(SudokuBoard board, Point point, byte number, int multiplier)
	{
		return number;
	}
	
	public int GetNextMultiplier(SudokuBoard board, Point point, byte number, int currentMultiplier)
	{
		return 0;
	}
}
