package com.Dave.Sudoku.Multipliers;

import java.util.List;

import android.graphics.Point;

import com.Dave.Sudoku.DebugLog;
import com.Dave.Sudoku.SudokuBoard;

public class Multiplier123 implements IMultiplier
{
	private int mCurrentMultiplier = 0;
	private int mProposedMultiplier = 0;

	public String GetName()
	{
		return "Mult123";
	}

	public void ResetMultiplier()
	{
		mCurrentMultiplier = 0;
		mProposedMultiplier = 0;
	}

	public int GetCurrentMultiplier()
	{
		return mCurrentMultiplier;
	}

	public int GetProposedMultiplier()
	{
		return mProposedMultiplier;
	}

	public void UpdateProposedMultiplier(SudokuBoard board, Point point, byte number)
	{
		int multiplier = GetCurrentMultiplier();
		
		if(number <= 0)
		{
			mProposedMultiplier = 0;
			return;
		}
		
		//For each of the opponent's squares:
			//Find what values are possible in the square
			//Apply the move, see if any values are no longer possible
			//Increment the multiplier if so
		
		int playerTurn = SudokuBoard.GetPlayerTerritory(point);
		
		List<Point> enemySquares = SudokuBoard.GetPlayerSquares(1 - playerTurn);
		
		for(int i=0; i<enemySquares.size(); i++)
		{
			List<Byte> initialSquareOptions = board.GetSquareOptions(enemySquares.get(i), false);
			
			board.SetCell(point, number, playerTurn, false);
			
			List<Byte> finalSquareOptions = board.GetSquareOptions(enemySquares.get(i), true);
			boolean incrementMultiplier = false;
			for(int n = 0; n < initialSquareOptions.size(); n++)
				if(!finalSquareOptions.contains(initialSquareOptions.get(n)))
				{
					if(multiplier == 0)
						multiplier = 1;
					
					//Log.i("ScoringConcept1", String.format("Incrementing multiplier to %d", multiplier + 1));
					incrementMultiplier = true;
					break;
				}
			
			if(incrementMultiplier)
			{
				multiplier++;
				
				DebugLog.Write("Increasing multiplier to " + multiplier, null);
				
				break;
			}
		}
		
		mProposedMultiplier = multiplier;
	}

	public void UpdateMultiplier(SudokuBoard board, Point point, byte number)
	{
		UpdateProposedMultiplier(board, point, number);
		mCurrentMultiplier = mProposedMultiplier;
	}

}
