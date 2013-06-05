package com.Dave.Sudoku.Scoring;

public class BonusScore
{
	public static int GetAdjustedScore(int proposedScore, String bonusSystem)
	{
		if(bonusSystem != null)
		{
			if(bonusSystem.equals("+5"))
				return proposedScore + 5;
			if(bonusSystem.equals("10"))
				return 10;
		}
		
		return proposedScore;
	}
}
