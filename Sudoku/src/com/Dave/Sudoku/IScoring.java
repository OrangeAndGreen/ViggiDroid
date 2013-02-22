package com.Dave.Sudoku;

import android.graphics.Point;

public interface IScoring
{
	int ScoreMove(byte[][] fullBoard, Point point, byte number, int multiplier);
	
	int GetNextMultiplier(byte[][] fullBoard, Point point, byte number);
}
