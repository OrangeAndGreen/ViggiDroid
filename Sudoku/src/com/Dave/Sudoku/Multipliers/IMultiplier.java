package com.Dave.Sudoku.Multipliers;

import android.graphics.Point;

import com.Dave.Sudoku.SudokuBoard;

public interface IMultiplier
{
	String GetName();
	
	void ResetMultiplier();
	
	int GetCurrentMultiplier();
	
	int GetProposedMultiplier();
	
	void UpdateProposedMultiplier(SudokuBoard board, Point point, byte number);
	
	void UpdateMultiplier(SudokuBoard board, Point point, byte number);
}
