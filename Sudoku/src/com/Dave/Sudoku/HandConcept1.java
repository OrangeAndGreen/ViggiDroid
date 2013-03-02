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
			Log.i("HandConcept1", "Generating new hand");
			
			if(mCurrentHand == null)
				mCurrentHand = new ArrayList<Byte>();
			
			if(mCurrentPlayer != playerTurn)
				mCurrentHand.clear();
			
			Point[] squares = SudokuBoard.GetPlayerSquares(playerTurn);
			
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
			
			List<Byte> options = new ArrayList<Byte>();
			for(int i = 1; i<boardOptions.length; i++)
				if(boardOptions[i])
					options.add((byte)i);
			
			int numOptions =options.size(); 
			if(numOptions == 0)
				return mCurrentHand;
			
			//Randomly select the required number of values
			for(int i=mCurrentHand.size(); i<mHandSize; i++)
			{
				int index = (int)(Math.max(Math.min((int)Math.round(Math.random() * numOptions - .5), numOptions - 1), 0) + 1);
				mCurrentHand.add(options.get(index));
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
}
