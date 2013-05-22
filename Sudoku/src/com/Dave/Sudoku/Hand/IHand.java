package com.Dave.Sudoku.Hand;

import java.util.List;

import com.Dave.Sudoku.SudokuBoard;

public interface IHand
{
	String GetName();
	
	void SetHandSize(int size);
	
	List<Byte> GetHand(SudokuBoard board, int playerTurn);
	
	void TakeNumber(SudokuBoard board, int playerTurn, byte number);
}
