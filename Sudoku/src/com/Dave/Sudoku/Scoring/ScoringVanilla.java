package com.Dave.Sudoku.Scoring;

import com.Dave.Sudoku.SudokuBoard;

import android.graphics.Point;

public class ScoringVanilla implements IScoring
{
	public String GetName()
	{
		return "Vanilla";
	}
	
	public int ScoreMove(SudokuBoard board, Point point, byte number, int multiplier, String bonusSystem)
	{
		return number * board.GetCellMultiplier(point);
	}
}
