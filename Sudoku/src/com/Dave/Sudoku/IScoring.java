package com.Dave.Sudoku;

import android.graphics.Point;

public interface IScoring
{
	int ScoreMove(int[][] fullBoard, Point point, int number, int multiplier);
	
	int GetNextMultiplier(int[][] fullBoard, Point point, int number);
}
