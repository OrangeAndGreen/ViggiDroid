package com.Dave.Files;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import com.Dave.DateStrings.DateStrings;
import com.Dave.Files.Transaction.PayMethod;

/*
 * Auto-split files by month
 * Don't load anything on startup, just log entry
 * 		Later: "View" button causes history to be loaded as necessary
 * 
 */

public class TransactionFile
{
	public static String LogDirectory = "/sdcard/00Logs/";
	
	public static void AddTransaction(Transaction transaction)
	{
		try
    	{
			String filename = String.format("%sTransactions %d %d.txt", LogDirectory, 
												transaction.Date.get(Calendar.YEAR),
												transaction.Date.get(Calendar.MONTH));
			
    		//Write entry to log file
    		FileWriter fw = new FileWriter(filename, true);

    		fw.append(DateStrings.GetDateTimeString(transaction.Date) + "\n");
    		fw.append(String.format("   Amount: %.02f\n", transaction.Amount));
    		
    		if(transaction.Entity != null)
    			fw.append(String.format("   Entity: %s\n", transaction.Entity));
    		
    		if(transaction.Categories != null)
    			for(int i=0; i<transaction.Categories.length; i++)
    				fw.append(String.format("   Category: %s\n", transaction.Categories[i]));
    		
    		if(transaction.Method != PayMethod.UNKNOWN)
    			fw.append(String.format("   Method: %s\n", transaction.Method.toString()));
    		
    		if(transaction.Comment != null)
    			fw.append(String.format("   Comment: %s\n", transaction.Comment));
    		
    		fw.append("\n");
    		fw.close();
    	}
    	catch(IOException e) {}
	}
	
	public static List<Transaction> ReadLog(Calendar searchDate)
	{
		String filename = String.format("%sTransactions %d %d.txt", LogDirectory, 
											searchDate.get(Calendar.YEAR),
											searchDate.get(Calendar.MONTH));
		
		List<Transaction> result = new ArrayList<Transaction>();
		
		//Get the lines from "Filename" file
		String[] logLines = new String[0];
		try
		{
			File f = new File(filename);
			BufferedReader br = new BufferedReader(new FileReader(f));
			String log = "";
			char[] buf = new char[(int)f.length()];
			br.read(buf, 0, (int)f.length());
			br.close();
			log = String.copyValueOf(buf);
			logLines = log.split("\n");
		}
		catch(Exception e) {}
		
		//Parse the entries and add Transactions to result
		Calendar curDate = null;
		float curAmount = 0;
		String curEntity = null;
		List<String> curCategories = new ArrayList<String>();
		PayMethod curMethod = PayMethod.UNKNOWN;
		String curComment = null;
		for(int i=0; i<logLines.length; i++)
		{
			if(logLines[i].trim().length() == 0 && curDate != null)
			{
				result.add(new Transaction(curDate, curAmount, curEntity, (String[])curCategories.toArray(), curMethod, curComment));
				curDate = null;
				curAmount = 0;
				curEntity = null;
				curCategories = null;
				curMethod = PayMethod.UNKNOWN;
				curComment = null;
			}
			String[] parts = logLines[i].split(": ");
			
			if(parts.length == 1)
			{
				curDate = DateStrings.ParseDateTimeString(parts[0]);
			}
			else if(parts[0].trim().equals("Amount"))
			{
				curAmount = Float.parseFloat(parts[1]);
			}
			else if(parts[0].trim().equals("Entity"))
			{
				curEntity = parts[1].trim();
			}
			else if(parts[0].trim().equals("Category"))
			{
				curCategories.add(parts[1].trim());
			}
			else if(parts[0].trim().equals("Method"))
			{				
				if(parts[1].toUpperCase().equals("UNKNOWN"))
					curMethod = PayMethod.UNKNOWN;
				else if(parts[1].toUpperCase().equals("CASH"))
					curMethod = PayMethod.CASH;
				else if(parts[1].toUpperCase().equals("DEBIT"))
					curMethod = PayMethod.DEBIT;
				else if(parts[1].toUpperCase().equals("CREDIT"))
					curMethod = PayMethod.CREDIT;
				else if(parts[1].toUpperCase().equals("CHECK"))
					curMethod = PayMethod.CHECK;
			}
			else if(parts[0].trim().equals("Comment"))
			{
				curComment = parts[1].trim();
			}
		}
		
		if(curDate != null)
			result.add(new Transaction(curDate, curAmount, curEntity, (String[])curCategories.toArray(), curMethod, curComment));
		
		return result;
	}
}
