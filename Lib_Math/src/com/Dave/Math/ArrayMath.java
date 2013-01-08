package com.Dave.Math;

public class ArrayMath
{
	public static float[] GetRunningAverageCurve(float[] data, float window)
	{
		if(data==null)
			return null;
		if(window <= 0)
			window = 1;
		if (window > data.length)
			window = data.length;
		
		float[] averaged = new float[data.length];

		//Calculate the initial average
		float initialAverage = 0;
		for(int i=0; i<window; i++)
			initialAverage += data[i];
		initialAverage /= window;
		
		float curAverage = initialAverage;
		for(int i=0; i<data.length; i++)
		{
			if(i >= window)
			{
				//Subtract off the old point and add the new
				curAverage = curAverage - (data[i - (int) window] / window) + (data[i] / window);
			}
			averaged[i] = curAverage;
		}
		return averaged;
	}

	public static float[] GetAllTimeRunningAverageCurve(float[] data)
	{
		int length = data.length;
		float[] output = new float[length];
		float runningTotal = 0;
		
		for(int i=0; i<length; i++)
		{
			runningTotal += data[i];
			output[i] = runningTotal / (i+1);
		}
		
		return output;
	}
	
	public static float[] GetGaussianEstimate(float[] data, int min, int max, int numPoints)
	{
		float mean = GetAverage(data);
		float stddev = GetStandardDeviation(data);
		float variance = stddev * stddev;
		float factor = 100 / (float)Math.sqrt(2 * Math.PI * variance);
		
		float multiplier = (max - min) / (float)numPoints;
		float[] ret = new float[numPoints];
		for(int i=0; i<numPoints; i++)
		{
			float xVal = (i * multiplier) - min;
			float exponent = -0.5f * (xVal - mean) * (xVal - mean) / variance;
			ret[i] = factor * (float)Math.pow(Math.E, exponent); 
		}
		
		return ret;
	}
	
	public static float GetAverage(float[] data)
	{
		float ret = 0;
		
		for(int i=0; i<data.length; i++)
			ret += data[i];
		ret /= data.length;
		
		return ret;
	}
	
	public static float GetStandardDeviation(float[] data)
	{
		float mean = GetAverage(data);
		
		float total = 0;
		for(int i=0; i<data.length; i++)
			total += ((data[i] - mean) * (data[i] - mean));
		total /= data.length;
		return (float)Math.sqrt(total);
	}
	
	public static float GetMin(float[] data)
	{
		float min = Float.MAX_VALUE;
		if(data == null)
			return min;
		for(int i=0; i<data.length; i++)
			if(data[i] < min)
				min = data[i];
		return min;
	}
	
	public static float GetMax(float[] data)
	{
		float max = Float.MIN_VALUE;
		if(data == null)
			return max;
		for(int i=0; i<data.length; i++)
			if(data[i] > max)
				max = data[i];
		return max;
	}
	
	public static float GetCeiling(float[] data)
	{
		float ceiling = GetMax(data);
		if(ceiling % 1 != 0)
			ceiling = ceiling + 1 - (ceiling % 1);
		return ceiling;
	}
}
