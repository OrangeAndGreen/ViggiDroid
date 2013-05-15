package com.Dave.Sudoku;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.graphics.Point;
import android.util.Log;

public class HandConcept1 implements IHand
{
	private int mHandSize = 5;
	
	private List<Byte> mCurrentHand = null;
	private int mCurrentPlayer = -1;
	
	private List<Byte> mRandomSequence = null;
	
	public HandConcept1(String randomString)
	{
		if(randomString != null && randomString.length() > 0)
		{
			mRandomSequence = new ArrayList<Byte>();
			
			for(int i=0; i<randomString.length(); i++)
			{
				mRandomSequence.add((byte)Integer.parseInt(randomString.substring(i, i+1)));
			}
		}
	}
	
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
			if(mCurrentHand == null)
				mCurrentHand = new ArrayList<Byte>();
			
			if(mCurrentPlayer != playerTurn)
				mCurrentHand.clear();
			mCurrentPlayer = playerTurn;
			
			DebugLog.Write(String.format("Getting %d new values for hand", mHandSize - mCurrentHand.size()), null);
			
			//Build a list of all the options for every cell in the player's territory
			List<Byte> allOptions = new ArrayList<Byte>();
			List<Point> squares = SudokuBoard.GetPlayerSquares(playerTurn);
			for(int i=0; i<squares.size(); i++)
			{
				for(int x=0; x<SudokuBoard.SquareSize; x++)
					for(int y=0; y<SudokuBoard.SquareSize; y++)
					{
						Point cell = new Point(squares.get(i).x * SudokuBoard.SquareSize + x, squares.get(i).y * SudokuBoard.SquareSize + y);
						List<Byte> cellOptions = board.GetCellOptions(cell, false);
						for(int n = 0; n<cellOptions.size(); n++)
							if(!allOptions.contains(cellOptions.get(n)))
								allOptions.add(cellOptions.get(n));
					}
			}
			
			//Randomly select the required number of values from the available list
			for(int i=mCurrentHand.size(); i<mHandSize; i++)
			{
				int numOptions = allOptions.size();
				if(numOptions == 0)
					break;
				
				Byte value = 0;
				if(mRandomSequence != null && mRandomSequence.size() > 0)
				{
					while(true)
					{
						value = mRandomSequence.remove(0);
						if(allOptions.contains(value))
							break;
					}
				}
				else
				{
					int index = MyRandom.Get(0, numOptions - 1);
					value = allOptions.remove(index);
				}
				mCurrentHand.add(value);
			}
			
			//Sort the list
			Collections.sort(mCurrentHand);
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
				DebugLog.Write(String.format("Removing %d from hand", number), null);
				mCurrentHand.remove(i);
				break;
			}
		}
		
		List<Point> squares = SudokuBoard.GetPlayerSquares(playerTurn);
		
		//Get the valid options on the board for this player
		List<Byte> boardOptions = new ArrayList<Byte>();
		for(int i=0; i<squares.size(); i++)
		{
			List<Byte> squareOptions = board.GetSquareOptions(squares.get(i), false);
			
			String message = String.format("Options for (%d,%d): ", squares.get(i).x, squares.get(i).y);
			for(int n=0; n<squareOptions.size(); n++)
				message += String.format("%d", squareOptions.get(n));
			DebugLog.Write(message, null);
			
			for(int num = 0; num < squareOptions.size(); num++)
				if(!boardOptions.contains(squareOptions.get(num)))
					boardOptions.add(squareOptions.get(num));
		}
		
		//Remove any other numbers in the hand that are no longer valid
		for(int i=mCurrentHand.size() - 1; i >= 0; i--)
		{
			byte handValue = mCurrentHand.get(i);
			if(!boardOptions.contains(handValue))
			{
				DebugLog.Write(String.format("%d invalidated from hand", handValue), null);
				mCurrentHand.remove(i);
			}
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
