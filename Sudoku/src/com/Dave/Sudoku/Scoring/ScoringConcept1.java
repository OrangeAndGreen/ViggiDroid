package com.Dave.Sudoku.Scoring;

import java.util.List;

import com.Dave.Sudoku.SudokuBoard;

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
		
		int score = number * board.GetCellMultiplier(point);
		if(multiplier > 0)
			score *= multiplier;
		
		//For each of the opponent's squares:
			//Find what values are possible in the square
			//Apply the move, see if any values are no longer possible
			//Add that number to the score if so
		
		int playerTurn = SudokuBoard.GetPlayerTerritory(point);
		
		List<Point> enemySquares = SudokuBoard.GetPlayerSquares(1 - playerTurn);
		
		for(int i=0; i<enemySquares.size(); i++)
		{
			List<Byte> initialSquareOptions = board.GetSquareOptions(enemySquares.get(i), true);
			
			board.SetCell(point, number, playerTurn, false);
			
			List<Byte> finalSquareOptions = board.GetSquareOptions(enemySquares.get(i), true);
			
			for(int n = 0; n < initialSquareOptions.size(); n++)
				if(!finalSquareOptions.contains(initialSquareOptions.get(n)))
				{
					//Log.i("ScoringConcept1", String.format("Adding %d to score", n));
					score += n;
				}
		}
		
		return score;
	}
}
