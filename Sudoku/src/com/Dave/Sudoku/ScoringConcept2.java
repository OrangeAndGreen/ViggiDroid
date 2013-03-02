package com.Dave.Sudoku;

import android.graphics.Point;

public class ScoringConcept2 implements IScoring
{
	public String GetName()
	{
		return "System 2";
	}
	
	public int ScoreMove(SudokuBoard board, Point point, byte number, int multiplier)
	{
		if(number <= 0)
			return 0;
		
		int score = number;
		if(multiplier > 0)
			score *= multiplier;
		
		//For each of the opponent's squares:
			//Find what values are possible in the square
			//Apply the move, see if any values are no longer possible
			//Add that number to the score if so
		
		int playerTurn = SudokuBoard.GetPlayerTerritory(point);
		
		Point[] enemySquares = SudokuBoard.GetPlayerSquares(1 - playerTurn);
		
		for(int i=0; i<enemySquares.length; i++)
		{
			boolean[] initialSquareOptions = board.GetSquareOptions(enemySquares[i], false);
			
			board.SetCell(point, number, playerTurn, false);
			
			boolean[] finalSquareOptions = board.GetSquareOptions(enemySquares[i], true);
			
			for(int n = 1; n < initialSquareOptions.length; n++)
				if(initialSquareOptions[n] && !finalSquareOptions[n])
				{
					//Log.i("ScoringConcept1", String.format("Adding %d to score", n));
					score += n;
				}
		}
		
		return score;
	}

	public int GetNextMultiplier(SudokuBoard board, Point point, byte number, int currentMultiplier)
	{
		int multiplier = currentMultiplier;
		
		if(number <= 0)
			return 0;
		
		//For each of the opponent's squares:
			//Find what values are possible in the square
			//Apply the move, see if any values are no longer possible
			//Increment the multiplier if so
		
		int playerTurn = SudokuBoard.GetPlayerTerritory(point);
		
		Point[] enemySquares = SudokuBoard.GetPlayerSquares(1 - playerTurn);
		
		for(int i=0; i<enemySquares.length; i++)
		{
			boolean[] initialSquareOptions = board.GetSquareOptions(enemySquares[i], false);
			
			board.SetCell(point, number, playerTurn, false);
			
			boolean[] finalSquareOptions = board.GetSquareOptions(enemySquares[i], true);
			boolean incrementMultiplier = false;
			for(int n = 1; n < initialSquareOptions.length; n++)
				if(initialSquareOptions[n] && !finalSquareOptions[n])
				{
					if(multiplier == 0)
						multiplier = 1;
					
					//Log.i("ScoringConcept1", String.format("Incrementing multiplier to %d", multiplier + 1));
					incrementMultiplier = true;
					multiplier++;
				}
			
			if(incrementMultiplier)
			{
				multiplier++;
				break;
			}
		}
		
		return multiplier;
	}
}

