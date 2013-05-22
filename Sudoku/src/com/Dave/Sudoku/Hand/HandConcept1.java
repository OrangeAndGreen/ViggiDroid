package com.Dave.Sudoku.Hand;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import com.Dave.Sudoku.DebugLog;
import com.Dave.Sudoku.MyRandom;
import com.Dave.Sudoku.SudokuBoard;

import android.graphics.Point;

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

	private List<Byte> GetHandOptions(SudokuBoard board, int playerTurn)
	{
		//Build a list of all the options for every cell in the player's territory
		List<Byte> allOptions = new ArrayList<Byte>();
		List<Point> squares = SudokuBoard.GetPlayerSquares(playerTurn);
		for(int i=0; i<squares.size(); i++)
		{
			for(int x=0; x<SudokuBoard.SquareSize; x++)
				for(int y=0; y<SudokuBoard.SquareSize; y++)
				{
					Point cell = new Point(squares.get(i).x * SudokuBoard.SquareSize + x, squares.get(i).y * SudokuBoard.SquareSize + y);
					List<Byte> cellOptions = board.GetCellOptions(cell, true);
					
					//String cellOptionsStr = String.format("Cell (%d,%d) options: ", cell.x, cell.y);
					//for(Byte b : cellOptions)
					//	cellOptionsStr += String.format("%d", b);
					//Log.i("", cellOptionsStr);
					
					for(int n = 0; n<cellOptions.size(); n++)
						if(!allOptions.contains(cellOptions.get(n)))
							allOptions.add(cellOptions.get(n));
				}
		}
		
		String optionsString = "";
		for(int i=0; i<allOptions.size(); i++)
			optionsString += String.format("%d", allOptions.get(i));
		DebugLog.Write("Hand options: " + optionsString, null);
		
		return allOptions;
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
			
			DebugLog.Write(String.format(Locale.US, "Getting %d new values for hand", mHandSize - mCurrentHand.size()), null);
			
			//Build a list of all the options for every cell in the player's territory
			List<Byte> allOptions = GetHandOptions(board, playerTurn);
			
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
			DebugLog.Write("Tried to remove a number from an empty hand", null);
			return;
		}
		
		//Find the number in the current list and remove it
		for(int i=0; i<mCurrentHand.size(); i++)
		{
			if(mCurrentHand.get(i) == number)
			{
				DebugLog.Write(String.format(Locale.US, "Removing %d from hand", number), null);
				mCurrentHand.remove(i);
				break;
			}
		}
		
		//Get the valid options on the board for this player
		List<Byte> boardOptions = GetHandOptions(board, playerTurn);
		
		//Remove any other numbers in the hand that are no longer valid
		for(int i=mCurrentHand.size() - 1; i >= 0; i--)
		{
			byte handValue = mCurrentHand.get(i);
			if(!boardOptions.contains(handValue))
			{
				DebugLog.Write(String.format(Locale.US, "%d invalidated from hand", handValue), null);
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
