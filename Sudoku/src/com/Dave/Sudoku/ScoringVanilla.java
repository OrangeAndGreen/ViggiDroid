package com.Dave.Sudoku;

import android.graphics.Point;

public class ScoringVanilla implements IScoring
{
	public int ScoreMove(int[][] fullBoard, Point point, int number, int multiplier)
	{
		return number;
	}
	
	public int GetNextMultiplier(int[][] fullBoard, Point point, int number)
	{
		return 0;
	}
}
