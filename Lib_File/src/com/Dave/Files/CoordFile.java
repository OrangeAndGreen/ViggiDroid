package com.Dave.Files;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import android.content.Context;
import android.location.Location;
import android.util.Log;
import android.widget.Toast;

import com.Dave.DateStrings.DateStrings;

public class CoordFile
{
	public String Filename = null;
	private Context mContext = null;
	private FileWriter mWriter = null;
	private File mFile = null;
	private List<GPSCoordinate> mCoords = new ArrayList<GPSCoordinate>();
	private List<Integer> mStrengths = new ArrayList<Integer>();

	//Creates a new coordinate file and autonames it
	public CoordFile(Context context)
	{
		this(context, null);
	}
	
	//Opens/creates a coordinate file with the given name
	public CoordFile(Context context, String filename)
	{
		mContext = context;
		Filename = filename;
		
		if(Filename == null)
		{
			Filename = String.format("CoordLog_%s.txt", DateStrings.GetDateTimeString(Calendar.getInstance()));
		}
		
		try
		{
			mFile = new File(filename);
			boolean exists = mFile.exists();
			
			mWriter = new FileWriter(filename, true);
			
			if(exists)
			{
				ReadFile();
			}
			else
			{
				String entry = "Date,Ticks,Longitude,Latitude,Altitude,Accuracy,Bearing,Speed\n";
				mWriter.append(entry);
			
	        	Toast t = Toast.makeText(mContext, String.format("Created output file %s", filename), Toast.LENGTH_SHORT);
	        	t.show();
			}
		}
		catch(Exception e)
		{
			Toast t = Toast.makeText(mContext, "Failed to create output file", Toast.LENGTH_SHORT);
			t.show();
		}

	}
	
	public void WriteEntry(Location location, int strength)
	{
		try
		{
			GPSCoordinate coord = new GPSCoordinate(location);
			
			mCoords.add(coord);
			mStrengths.add(strength);
			
			mWriter.append(coord.ToString() + ", " + strength + "\n");
			
			//Toast t = Toast.makeText(mContext, "Wrote log entry", Toast.LENGTH_SHORT);
			//t.show();
		}
		catch(Exception e)
		{
			Toast t = Toast.makeText(mContext, "Failed to write to output file", Toast.LENGTH_SHORT);
			t.show();
		}
	}
		
	private void ReadFile()
	{
		long ticks = 0;
		try
		{
			BufferedReader br = new BufferedReader(new FileReader(mFile));
			char[] buf = new char[(int)mFile.length()];
			br.read(buf, 0, (int)mFile.length());
			br.close();
			String text = String.copyValueOf(buf);
			
			String[] lines = text.split("\n");
			
			mCoords.clear();
			mStrengths.clear();
			for(int i=0; i<lines.length; i++)
			{
				String[] parts = lines[i].split(",");
				if(parts.length < 8)
					continue;
				
				if(parts[0].trim().equals("Date"))
					continue;
				
				Location location = new Location("File");
				int strength = 0;
				try
				{
					ticks = Long.parseLong(parts[1].trim());
					location.setTime(ticks);
				
					location.setLongitude(Double.parseDouble(parts[2].trim()));
					location.setLatitude(Double.parseDouble(parts[3].trim()));
					location.setAltitude(Double.parseDouble(parts[4].trim()));
					location.setAccuracy(Float.parseFloat(parts[5].trim()));
					location.setBearing(Float.parseFloat(parts[6].trim()));
					location.setSpeed(Float.parseFloat(parts[7].trim()));
					strength = Integer.parseInt(parts[8].trim());
				}
				catch(Exception e)
				{
					ErrorFile.WriteException(e, mContext);
				}
				
				mStrengths.add(strength);
				mCoords.add(new GPSCoordinate(location));
			}
		}
		catch(Exception e)
		{
			Toast t = Toast.makeText(mContext, "Failed to read log: " + e.toString() + ", " + ticks, Toast.LENGTH_SHORT);
			t.show();
		}
	}
	
	public void Close()
	{
		if(mWriter != null)
		{
			try
			{
				mWriter.close();
			}
			catch(Exception e)
			{
				Toast t = Toast.makeText(mContext, "Failed to close output file writer", Toast.LENGTH_SHORT);
				t.show();
			}
			mWriter = null;
		}
	}
	
	
	
	
	/* Single-entry data accessors */
	public long GetTime(int i)
	{
		return mCoords.get(i).GetTime();
	}
	
	public double GetLongitude(int i)
	{
		return mCoords.get(i).GetLongitude();
	}
	
	public double GetLatitude(int i)
	{
		return mCoords.get(i).GetLatitude();
	}
	
	public double GetAltitude(int i, GPSUnits units)
	{
		return  mCoords.get(i).GetAltitude(units);
	}
	
	public float GetAccuracy(int i, GPSUnits units)
	{
		return  mCoords.get(i).GetAccuracy(units);
	}
	
	public float GetBearing(int i)
	{
		return  mCoords.get(i).GetBearing();
	}
	
	public float GetSpeed(int i, GPSUnits units)
	{
		return  mCoords.get(i).GetSpeed(units);
	}
	
	public int GetStrength(int i)
	{
		return mStrengths.get(i);
	}
	
	
	/* Stats about file */
	public int Size()
	{
		return mCoords.size();
	}
	
	public Calendar GetStartDate()
	{
		if(Size() < 1)
			return Calendar.getInstance();
		Calendar ret = Calendar.getInstance();
		
		ret.setTimeInMillis(mCoords.get(0).GetTime());
		
		return ret;
	}
	
	public Calendar GetEndDate()
	{
		if(Size() < 1)
			return Calendar.getInstance();
		
		Calendar ret = Calendar.getInstance();
		
		ret.setTimeInMillis(mCoords.get(Size() - 1).GetTime());
		
		return ret;
	}
	
	public long GetRuntime()
	{
		return GetEndDate().getTimeInMillis() - GetStartDate().getTimeInMillis();
	}
	
	public float GetDistanceTravelled(GPSUnits units)
	{
		if(Size() < 2)
			return 0;
		GPSCoordinate first = mCoords.get(0);
		GPSCoordinate last = mCoords.get(Size() - 1);
		return GPSCoordinate.GetDistance(first, last, units);
	}
	
	public float GetAverageSpeed(GPSUnits units)
	{
		if(Size() < 1)
			return 0;
		
		float ret = 0;

		for(int i=0; i<Size(); i++)
			ret += mCoords.get(i).GetSpeed(units);
		
		ret /= Size();
		
		return ret;
	}
	
	public long GetStopTime(float stoppedThresh, GPSUnits units)
	{
		long ret = 0;
		
		long prevTime = 0;
		float prevSpeed = 0;
		for(int i=0; i<Size(); i++)
		{
			long curTime = mCoords.get(i).GetTime();
			float curSpeed = GetSpeed(i, units);
			
			if(i > 0 && curSpeed < stoppedThresh && prevSpeed < stoppedThresh)
			{
				ret += (curTime - prevTime);
			}
			
			prevTime = curTime;
			prevSpeed = curSpeed;
		}
		
		return ret;
	}
	
	public int GetNumberOfStops(float stoppedThresh, GPSUnits units)
	{
		int ret = 0;
		
		long curStopTime = 0;
		
		long prevTime = 0;
		float prevSpeed = 0;
		for(int i=0; i<Size(); i++)
		{
			long curTime = mCoords.get(i).GetTime();
			float curSpeed = GetSpeed(i, units);
			
			if(i > 0 && curSpeed < stoppedThresh && prevSpeed < stoppedThresh)
			{
				if(curStopTime == 0)
					ret++;
				curStopTime += (curTime - prevTime);
			}
			else if(curStopTime > 0)
			{
				curStopTime = 0;
			}
			
			prevTime = curTime;
			prevSpeed = curSpeed;
		}
		
		return ret;
	}
	
	public float GetEfficiency(float stoppedThresh, GPSUnits units)
	{
		return (float)(GetRuntime() - GetStopTime(stoppedThresh, units)) / GetRuntime() * 100;
	}
	
	public boolean IsStopped(float stoppedThresh, GPSUnits units)
	{
		return IsStopped(Size() - 2, Size() - 1, stoppedThresh, units);
	}
	
	public boolean IsStopped(int prevPoint, int curPoint, float stoppedThresh, GPSUnits units)
	{
		float prevSpeed = 0;
		if(Size() > prevPoint && prevPoint >= 0)
			prevSpeed = GetSpeed(prevPoint, units);
		float curSpeed = 0;
		if(Size() > curPoint && curPoint >= 0)
			curSpeed = GetSpeed(curPoint, units);
		
		return curSpeed < stoppedThresh && prevSpeed < stoppedThresh;
	}

}
