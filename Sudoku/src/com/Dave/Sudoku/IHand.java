package com.Dave.Sudoku;

import java.util.List;

public interface IHand
{
	String GetName();
	
	void SetHandSize(int size);
	
	List<Byte> GetHand(SudokuBoard board, int playerTurn);
	
	void TakeNumber(SudokuBoard board, int playerTurn, byte number);
}
