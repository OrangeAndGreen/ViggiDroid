package com.Dave.Sudoku.Hand;

import java.util.ArrayList;
import java.util.List;

import com.Dave.Sudoku.SudokuBoard;

public class HandVanilla implements IHand
{
	private int mHandSize = 9;
	
	public String GetName()
	{
		return "Vanilla";
	}
	
	public void SetHandSize(int size)
	{
	}

	public List<Byte> GetHand(SudokuBoard board, int playerTurn)
	{
		//Always return all possibilities
		List<Byte> ret = new ArrayList<Byte>();
		
		for(int i=0; i<mHandSize; i++)
		{
			ret.add((byte)(i + 1));
		}
		
		return ret;
	}
	
	public void TakeNumber(SudokuBoard board, int playerTurn, byte number)
	{
		//Do nothing
	}
}
