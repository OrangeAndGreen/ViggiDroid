package com.Dave.Math;

public class ArrayMath
{
	public static float[] GetRunningAverageCurve(float[] data, float window)
	{
		if(data==null)
			return null;
		if(window <= 0)
			window++;
		
		float[] averaged = new float[data.length];
		float curAverage = 0;
		int startLength = Math.min(data.length, (int)window);
		for(int i=0; i<startLength; i++)
			curAverage += data[i];
		curAverage /= startLength;
		
		averaged[0] = curAverage;
		float initialAverage = curAverage;

		for(int i=1; i<data.length; i++)
		{
			int index = i - (int) window;
			float curValue = initialAverage;
			if(index >= 0)
				curValue = data[index];
			//Subtract off the old point and add the new
			curAverage = curAverage - (curValue / window) + (data[i] / window);
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
