package com.Dave.Sudoku;

import android.graphics.Point;

public class ScoringVanilla implements IScoring
{
	public int ScoreMove(byte[][] fullBoard, Point point, byte number, int multiplier)
	{
		return number;
	}
	
	public int GetNextMultiplier(byte[][] fullBoard, Point point, byte number)
	{
		return 0;
	}
}
