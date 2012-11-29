package com.Dave.Files;

import java.util.Calendar;

public class Transaction
{
	public Calendar Date = null;
	public float Amount = 0;
	public String Entity = null;
	public String[] Categories = null;
	public PayMethod Method = PayMethod.UNKNOWN;
	public String Comment = null;
	
	public Transaction(Calendar date, float amount, String entity, String[] categories, PayMethod method, String comment)
	{
		Date = date;
		Amount = amount;
		Entity = entity;
		Categories = categories;
		Method = method;
		Comment = comment;
	}
	
	public enum PayMethod
	{
		UNKNOWN,
		CASH,
		DEBIT,
		CREDIT,
		CHECK
	}
}
