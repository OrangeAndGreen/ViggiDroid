package com.Dave.Sudoku;

import android.graphics.Point;

/*
 * This scoring system awards points when a number is made invalid in an opponent's square
 */
public class ScoringConcept1 implements IScoring
{
	public String GetName()
	{
		return "System 1";
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
			boolean[] initialSquareOptions = board.GetSquareOptions(enemySquares[i], true);
			
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
		return 0;
	}
}
