package com.Dave.Sudoku.Scoring;

import com.Dave.Sudoku.SudokuBoard;

import android.graphics.Point;

public interface IScoring
{
	String GetName();
	
	int ScoreMove(SudokuBoard board, Point point, byte number, int multiplier, String bonusSystem);
}
