package com.Dave.BudgetTracker;

import java.util.Calendar;
import com.Dave.DateStrings.DateStrings;

public class Expense
{
	public Calendar Date = null;
	public String Location = null;
	public float Amount = 0;
	public String Comment = null;
	
	public Expense(Calendar date, String location, float amount, String comment)
	{
		Date = (Calendar) date.clone();
		Location = location;
		Amount = amount;
		Comment = comment;
	}
	
	public Expense(String date, String location, String amount, String comment)
	{
		Date = DateStrings.ParseDateTimeString(date);
		Location = location;
		Amount = Float.parseFloat(amount.substring(1));
		Comment = comment;
	}
	

}
