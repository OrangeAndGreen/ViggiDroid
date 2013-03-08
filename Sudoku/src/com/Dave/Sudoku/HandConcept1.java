package com.Dave.Sudoku;

import java.util.ArrayList;
import java.util.List;

import android.graphics.Point;
import android.util.Log;

public class HandConcept1 implements IHand
{
	private int mHandSize = 5;
	
	private List<Byte> mCurrentHand = null;
	private int mCurrentPlayer = -1;
	
	public String GetName()
	{
		return "Concept 1";
	}
	
	public void SetHandSize(int size)
	{
		mHandSize = size;
		if(mHandSize > 9)
			mHandSize = 9;
	}

	public List<Byte> GetHand(SudokuBoard board, int playerTurn)
	{
		if(mCurrentHand == null || mCurrentHand.size() < mHandSize || mCurrentPlayer != playerTurn)
		{
			Log.i("HandConcept1", "Getting new values for hand");
			
			if(mCurrentHand == null)
				mCurrentHand = new ArrayList<Byte>();
			
			if(mCurrentPlayer != playerTurn)
				mCurrentHand.clear();
			mCurrentPlayer = playerTurn;
			
			//Build a list of all the options for every cell in the player's territory
			List<Byte> allOptions = new ArrayList<Byte>();
			Point[] squares = SudokuBoard.GetPlayerSquares(playerTurn);
			for(int i=0; i<squares.length; i++)
			{
				for(int x=0; x<SudokuBoard.SquareSize; x++)
					for(int y=0; y<SudokuBoard.SquareSize; y++)
					{
						Point cell = new Point(squares[i].x * SudokuBoard.SquareSize + x, squares[i].y * SudokuBoard.SquareSize + y);
						boolean[] cellOptions = board.GetCellOptions(cell, false);
						for(int n = 1; n<cellOptions.length; n++)
							if(cellOptions[n])
								allOptions.add((byte)n);
					}
			}
			
			//Randomly select the required number of values from the available list
			for(int i=mCurrentHand.size(); i<mHandSize; i++)
			{
				int numOptions = allOptions.size();
				if(numOptions == 0)
					break;
				int index = (int)(Math.max(Math.min((int)Math.round(Math.random() * numOptions - .5), numOptions - 1), 0));
				Byte value = allOptions.remove(index);
				mCurrentHand.add(value);
			}
		}
		
		return mCurrentHand;
	}
	
	public void TakeNumber(SudokuBoard board, int playerTurn, byte number)
	{
		if(mCurrentHand == null || mCurrentHand.size() == 0)
		{
			Log.i("HandConcept1", "Tried to remove a number from an empty hand");
			return;
		}
		
		//Find the number in the current list and remove it
		for(int i=0; i<mCurrentHand.size(); i++)
		{
			if(mCurrentHand.get(i) == number)
			{
				mCurrentHand.remove(i);
				return;
			}
		}
		
		Point[] squares = SudokuBoard.GetPlayerSquares(playerTurn);
		
		//Get the valid options on the board for this player
		boolean[] boardOptions = null;
		for(int i=0; i<squares.length; i++)
		{
			boolean[] squareOptions = board.GetSquareOptions(squares[i], false);
			if(boardOptions == null)
				boardOptions = squareOptions;
			else
				for(int num = 0; num < boardOptions.length; num++)
					boardOptions[num] |= squareOptions[num];
		}
		
		//Remove any other numbers in the hand that are no longer valid
		for(int i=mCurrentHand.size() - 1; i >= 0; i--)
		{
			if(!boardOptions[i])
				mCurrentHand.remove(i);
		}
	}

	@Override
    public String toString()
	{
		String ret = "";
		for(int i=0; i < mCurrentHand.size(); i++)
		{
			if(i > 0)
				ret += ", ";
			ret += mCurrentHand.get(i);
		}
		return ret;
	}
}
