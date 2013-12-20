package com.Dave.Graph;

import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import com.Dave.DateStrings.DateStrings;
import com.Dave.Files.ErrorFile;
import com.Dave.Graph.GraphAxis.EdgeType;
import com.Dave.Graph.GraphLabel.VerticalAlign;
import com.Dave.Graph.GraphPoint.PointType;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Point;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

/*
 * Reusable Graph View
 * 
 * Want this to be reusable for all graphing applications
 * 
 * Styles:
 * 		-Graph
 * 		-Scroll chart
 * 		-Sweep chart
 * 		-Polar graph/chart
 * 
 * Set data:
 * 		-Types: double, float, int, short, etc.
 * 		-Multiple "Plots", stored in a List<Plot>
 * 			-Plot contains data, color, thickness, point style, line style, constants
 * 
 * Customize:
 * 		-Graph border
 * 		-Vertical/horizontal axes
 * 		-Column/row shading
 * 		-Labels
 * 		-Tick marks
 * 
 * Zoom in/out on data, and scroll when zoomed
 * 
 * Long click:
 * 		-Export image
 * 		-Copy to clipboard
 * 		-Add label
 * 
 * Procedure thinking:
 * 		-Constructor: Create graph object (axes, title, etc.)
 * 		-EasyGraph:	 Fill in low-level settings (set title, axis specifics, data)
 * 		-onDraw: Calculate sizes, scale labels if applicable
 * 
 * Calculating the graph layout:
 * 		-Calculate data range from plots, points, bars, lines
 * 		-Generate axis labels from data if needed
 * 		-Calculate axis positions and extents
 * 		-Calculate graph edges
 * 		
 * 
 */

public class GraphView extends View
{
	private int mDrawCount = 0;
	
	//Drawing members
	private int mTotalHeight = 0;
	private int mTotalWidth = 0;
	private FloatRectangle mDataRange = null;
	private FloatRectangle mDataRangeOverride = null;
	private GraphRectangle mGraphBounds = null;
	private List<IGraphElement> mGraphElements = new ArrayList<IGraphElement>();

	//New graphing members
	public boolean DrawBorder = false;
	public GraphLabel Title = null;
	public GraphAxis LeftAxis = null;
	public GraphAxis RightAxis = null;
	public GraphAxis BottomAxis = null;
	public GraphAxis TopAxis = null;
	public List<GraphAxis> PolarAxes = new ArrayList<GraphAxis>();
	public List<GraphPlot> Plots = new ArrayList<GraphPlot>();
	public List<GraphRectangle> Bars = new ArrayList<GraphRectangle>();
	public List<GraphLine> Lines = new ArrayList<GraphLine>();
	public List<GraphPoint> Points = new ArrayList<GraphPoint>();
	public List<GraphLabel> Labels = new ArrayList<GraphLabel>();
	public int PointColor = Color.GREEN;
	

	//********** Constructor, View overrides, and sizing **********
	public GraphView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		CreateGraph();
	}

	public void CreateGraph()
	{
		Title = new GraphLabel(true);
	
		LeftAxis = new GraphAxis(EdgeType.LEFT);
		RightAxis = new GraphAxis(EdgeType.RIGHT);
		BottomAxis = new GraphAxis(EdgeType.BOTTOM);
		TopAxis = new GraphAxis(EdgeType.TOP);
	}
	
	public void ClearGraph()
	{
		Title.Text = "";
		Plots.clear();
		PolarAxes.clear();
		Bars.clear();
		Lines.clear();
		Points.clear();
		Labels.clear();
	}

	public void EmptyGraph()
	{
		ClearGraph();
	}
	
	public void EasyGraph(float[] data)
	{
		try
		{
			//Draw the default graph
			ClearGraph();
		
			Title.Text = "Quick graph";
		
			Plots.add(new GraphPlot(data, 1));
			Plots.get(0).SetColor(Color.RED);
		
			if(LeftAxis == null)
				LeftAxis = new GraphAxis(EdgeType.LEFT);
			if(RightAxis == null)
				RightAxis = new GraphAxis(EdgeType.RIGHT);
			if(BottomAxis == null)
				BottomAxis = new GraphAxis(EdgeType.BOTTOM);
			if(TopAxis == null)
				TopAxis = new GraphAxis(EdgeType.TOP);
			
			BottomAxis.DrawLines = false;
			RightAxis.GenerateLabels(Plots);
			LeftAxis.GenerateLabels(Plots);
			BottomAxis.GenerateLabels(0, data.length - 1, 1, true);
		}
		catch(Exception e)
		{
			ErrorFile.WriteException(e, null);
		}
	}
	
	public void EasyLineGraph(float[] xValues, float[] yValues)
	{
		try
		{
			//Draw the default graph
			ClearGraph();
		
			Title.Text = "Scatter plot";
		
			Plots.add(new GraphPlot(xValues, yValues));
			Plots.get(0).SetColor(Color.RED);
			Plots.get(0).DrawLine = true;
			Plots.get(0).DrawPoints = true;
		
			if(LeftAxis == null)
				LeftAxis = new GraphAxis(EdgeType.LEFT);
			if(RightAxis == null)
				RightAxis = new GraphAxis(EdgeType.RIGHT);
			if(BottomAxis == null)
				BottomAxis = new GraphAxis(EdgeType.BOTTOM);
			if(TopAxis == null)
				TopAxis = new GraphAxis(EdgeType.TOP);
			
			BottomAxis.DrawLines = false;
			RightAxis.GenerateLabels(Plots);
			LeftAxis.GenerateLabels(Plots);
			//TODO: Does this work? May need better way to generate labels
			//BottomAxis.GenerateLabels(DateStrings.GetActiveDiffInDays(oldDate, newDate, midnightHour), 1, true);
		}
		catch(Exception e)
		{
			ErrorFile.WriteException(e, null);
		}
	}
	
	public void EasyScatterPlot(float[] xValues, float[] yValues, boolean drawLinePairs, FloatRectangle dataBounds)
	{
		try
		{
			//Draw the default graph
			ClearGraph();
		
			mDataRangeOverride = dataBounds;
			
			Title.Text = "Scatter plot";
		
			Plots.add(new GraphPlot(xValues, yValues, drawLinePairs));
			Plots.get(0).SetColor(Color.RED);
			Plots.get(0).DrawLine = false;
			Plots.get(0).DrawPoints = true;
		
			if(LeftAxis == null)
				LeftAxis = new GraphAxis(EdgeType.LEFT);
			if(RightAxis == null)
				RightAxis = new GraphAxis(EdgeType.RIGHT);
			if(BottomAxis == null)
				BottomAxis = new GraphAxis(EdgeType.BOTTOM);
			if(TopAxis == null)
				TopAxis = new GraphAxis(EdgeType.TOP);
			
			if(mDataRangeOverride != null)
			{
				RightAxis.GenerateLabels((int)mDataRangeOverride.Bottom, (int)mDataRangeOverride.Top, 1, false);
				LeftAxis.GenerateLabels((int)mDataRangeOverride.Bottom, (int)mDataRangeOverride.Top, 1, false);
			}
			else
			{
				RightAxis.GenerateLabels(Plots);
				LeftAxis.GenerateLabels(Plots);
			}
			
			BottomAxis.DrawLines = false;
			
			//TODO: Does this work? May need better way to generate labels
			//BottomAxis.GenerateLabels(DateStrings.GetActiveDiffInDays(oldDate, newDate, midnightHour), 1, true);
		}
		catch(Exception e)
		{
			ErrorFile.WriteException(e, null);
		}
	}
	
	public void PolarPlot(float[] data)
	{
		try
		{
			//Draw the default graph
			ClearGraph();
		
			Title.Text = "Quick polar graph";
		
			Plots.add(new PolarPlot(data, 1));
			Plots.get(0).SetColor(Color.RED);
		
			PolarAxes.add(new GraphAxis(-1));
			
			BottomAxis = null;
			RightAxis = null;
			LeftAxis = null;
			TopAxis = null;
		}
		catch(Exception e)
		{
			ErrorFile.WriteException(e, null);
		}
	}
	
	@Override
	protected void onDraw(Canvas canvas)
	{
		try
		{
			//Paint paint = new Paint();
			//canvas.drawBitmap(GetBlankBitmap(), 0, 0, paint);
			Draw(canvas);
		}
		catch(Exception e)
		{
			ErrorFile.WriteException(e, null);
		}
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
	{
        setMeasuredDimension(measureWidth(widthMeasureSpec), measureHeight(heightMeasureSpec));
	}
	    
    public void Draw(Canvas canvas)
    {
    	Log.i("GraphView.Draw", String.format("Drawing graph"));
    	mDrawCount++;
    	UpdateParams();

    	Paint paint = new Paint();
    	paint.setColor(Color.BLACK);
    	canvas.drawRect(0, 0, mTotalWidth, mTotalHeight, paint);
		//Draw all of the elements for the graph
    	//Log.e("Graphing", String.format("Number of elements: %d", mGraphElements.size()));
		for(int i=0; i<mGraphElements.size(); i++)
		{
			//Log.e("Graphing", String.format("Drawing element %s", mGraphElements.get(i).GetName()));
			mGraphElements.get(i).Draw(canvas, mGraphBounds, mDataRange);
		}
		
		//Test code, draws BG rectangle and string showing size
		//setBackgroundColor(Color.DKGRAY);
		//mPaint.setColor(Color.GREEN);
		//String text = String.format("Size: (%d, %d), %d", mTotalWidth, mTotalHeight, mDrawCount);
		//String text2 = String.format("(%d, %d, %d, %d)", mGraphBounds.Left, mGraphBounds.Top, mGraphBounds.Right, mGraphBounds.Bottom);
		//mCanvas.drawText(text, 40, 20, mPaint);
		//mCanvas.drawText(text2, 40, 40, mPaint);
    }
    
    public Bitmap GetBitmap()
    {
    	Bitmap ret = Bitmap.createBitmap(mTotalWidth, mTotalHeight, Config.ARGB_8888);
    	Canvas canvas = new Canvas();
    	canvas.setBitmap(ret);
    	
    	Draw(canvas);
    	
    	return ret;
    }
    
    public Bitmap GetBlankBitmap()
    {
    	Bitmap ret = Bitmap.createBitmap(mTotalWidth, mTotalHeight, Config.ARGB_8888);
    	Canvas canvas = new Canvas();
    	Paint paint = new Paint();
    	canvas.setBitmap(ret);
    	canvas.drawRect(0, 0, mTotalWidth, mTotalHeight, paint);
    	
    	return ret;
    }
    
    public void AddDateInfo(Calendar startDate, boolean labelDates)
	{
		List<Calendar> allDates = new ArrayList<Calendar>();
	
		Calendar curDate = (Calendar) startDate.clone();
        int dataLength = Plots.get(0).Data.length;
		for(int i=0; i<dataLength; i++)
		{
			allDates.add((Calendar) curDate.clone());
			curDate.add(Calendar.HOUR, 24);
		}
		
		AddDateInfo(allDates, labelDates);
	}
	
    public void AddDateInfo(Calendar startDate, Calendar endDate, boolean labelDates)
    {
    	List<Calendar> allDates = new ArrayList<Calendar>();
    	
		Calendar curDate = (Calendar) startDate.clone();
		allDates.add((Calendar) curDate.clone());
		while(curDate.before(endDate))
		{
			allDates.add((Calendar) curDate.clone());
			curDate.add(Calendar.HOUR, 24);
		}
		
		AddDateInfo(allDates, labelDates);
    }
    
	public void AddDateInfo(List<Calendar> dates, boolean labelDates)
	{
		UpdateParams();
            
		int numDates = dates.size();
		int totalDays = DateStrings.GetActiveDiffInDays(dates.get(0), Calendar.getInstance(), 0) + 1;
		
		//Log.d("DEBUG", String.format("AddDateInfo: %d dates, %d total days", numDates, totalDays));
		
        int graphWidth = mGraphBounds.GetWidth();
        float pixelRatio = (float) graphWidth / (totalDays - 1);
		int lastMonthDay = -1;
		int blue = Color.rgb(0, 0, 64);
        for(int i=0; i<numDates; i++)
        {
			Calendar curDate = dates.get(i);
		
			//Add weekend shading and first-of-month lines/labels
            float x = (pixelRatio * DateStrings.GetActiveDiffInDays(dates.get(0), curDate, 0)) + mGraphBounds.Left;
            int curDay = curDate.get(Calendar.DAY_OF_WEEK) % 7;
            int monthDay = curDate.get(Calendar.DAY_OF_MONTH);
			int month = curDate.get(Calendar.MONTH) + 1;
			if(lastMonthDay != monthDay) //TODO: Should probably use day of year here instead
			{
				if(curDay == 1 || curDay == 0)
				{
					//Log.d("DEBUG", String.format("AddDateInfo: Weekend at %s", DateStrings.GetDateTimeString(curDate)));
					//Draw weekend shading
					float halfColumn = pixelRatio / 2;
					float xStart = x - halfColumn;
					float xEnd = x + halfColumn + 1;
					
					if(i == 0)
						xStart = x;
					if(i == numDates - 1)
						xEnd = x;
					GraphRectangle bar = new GraphRectangle((int)(xStart), mGraphBounds.Top, (int)xEnd, mGraphBounds.Bottom);
					bar.SetColor(blue);
					bar.Fill = true;
					bar.FillColor = blue;
					Bars.add(bar);
				}
				if(labelDates && monthDay == 1)
				{
					//Log.d("DEBUG", String.format("AddDateInfo: Month-line at %s", DateStrings.GetDateTimeString(curDate)));
					
					//Draw start-of-month line
					GraphLine line = new GraphLine(new Point((int)x, mGraphBounds.Top), new Point((int)x, mGraphBounds.Bottom));
					line.SetColor(Color.DKGRAY);
					Lines.add(line);
					
					if(month == 1 || month == 7)
					{
						//Draw a second line to make January and July thicker
						line = new GraphLine(new Point((int)x + 1, mGraphBounds.Top), new Point((int)x + 1, mGraphBounds.Bottom));
						line.SetColor(Color.DKGRAY);
						Lines.add(line);
					}
					
					if(totalDays < 730) //2 years
					{
						String monthText = String.format("%d", month);
						GraphLabel label = new GraphLabel(new Point((int)x, mGraphBounds.Bottom + BottomAxis.TickLength + 2), monthText);
						label.HAlign = Align.CENTER;
						label.VAlign = VerticalAlign.TOP;
						Labels.add(label);
					}
					else if (month == 1) //January
					{
						String yearText = String.format("%d", curDate.get(Calendar.YEAR));
						GraphLabel label = new GraphLabel(new Point((int)x, mGraphBounds.Bottom + BottomAxis.TickLength + 2), yearText);
						label.HAlign = Align.CENTER;
						label.VAlign = VerticalAlign.TOP;
						Labels.add(label);
					}
				}
			}
			lastMonthDay = monthDay;
        }
    }
        
    public void AddDataPoint(float x, float y, int color)
    {
    	UpdateParams();
    	
    	GraphPoint graphPoint = new GraphPoint(null, x, y, 2, PointType.CROSS);
    	graphPoint.SetColor(color);
    	Points.add(graphPoint);
    }
    
	private void UpdateParams()
	{
		//Update View size
		mTotalHeight = getHeight();
		mTotalWidth = getWidth();
		
		//Setup the list of elements to draw in the correct order
		mGraphElements.clear();
		if(DrawBorder)
			mGraphElements.add(new GraphRectangle(0, 1, mTotalWidth-1, mTotalHeight));		
		mGraphElements.add(Title);
		mGraphElements.addAll(Bars);
		if(LeftAxis != null)
			mGraphElements.add(LeftAxis);
		if(RightAxis != null)
			mGraphElements.add(RightAxis);
		if(BottomAxis != null)
			mGraphElements.add(BottomAxis);
		if(TopAxis != null)
			mGraphElements.add(TopAxis);
		if(PolarAxes != null)
			for(int i=0; i<PolarAxes.size(); i++)
			{
				GraphAxis axis = PolarAxes.get(i);
				if(axis != null)
					mGraphElements.add(axis);
			}
		mGraphElements.addAll(Lines);
		mGraphElements.addAll(Plots);
		mGraphElements.addAll(Points);
		mGraphElements.addAll(Labels);
		
		Title.GetExtent(new Point(mTotalWidth, mTotalHeight));
		
		//Update data range
		mDataRange = GetDataRange();
		
		//Update graph bounds
		mGraphBounds = GetGraphBounds();
	}
	
	public FloatRectangle GetDataRange()
	{
		if(mDataRangeOverride != null)
			return mDataRangeOverride;
		
		if(Plots.size() == 0)
			return new FloatRectangle();
		
		//Go through Plots and determine max bounds of the data
		FloatRectangle result = new FloatRectangle(Float.MAX_VALUE, Float.MIN_VALUE, Float.MIN_VALUE, Float.MAX_VALUE);
		for(int i=0; i<Plots.size(); i++)
			result.ExpandToEnclose(Plots.get(i).GetDataRange());
		for(int i=0; i<Points.size(); i++)
			result.ExpandToEnclose(Points.get(i).GetDataRange());
		
		//In case the Y max was set manually, 
		if(LeftAxis != null)
			for(int i=0; i<LeftAxis.Labels.size(); i++)
			{
				int num = Integer.parseInt(LeftAxis.Labels.get(i).Text);
				if(num > result.Top)
					result.Top = num;
			}
		return result;
	}
	
	public GraphRectangle GetGraphBounds()
	{
		//Go through axes and determine extent of plot area
		GraphRectangle result = new GraphRectangle(0, 1, mTotalWidth-1, mTotalHeight);
		if(LeftAxis != null && RightAxis != null && TopAxis != null && BottomAxis != null)
		{
			result.Left = LeftAxis.FindGraphEdge(0, mTotalWidth);
			result.Right = RightAxis.FindGraphEdge(0, mTotalWidth);
			result.Top = Title.GetExtent(new Point(mTotalWidth, mTotalHeight)).GetHeight() + 5;
			result.Bottom = BottomAxis.FindGraphEdge(0, mTotalHeight);
		}
		else
		{
			result.Left = 0;
			result.Top = 0;
			result.Right = mTotalWidth;
			result.Bottom = mTotalHeight;
		}
		return result;
	}
	

	/**
     * Determines the width of this view
     * @param measureSpec A measureSpec packed into an int
     * @return The width of the view, honoring constraints from measureSpec
     */
    private int measureWidth(int measureSpec) {
        int result = 0;
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);

        if (specMode == MeasureSpec.EXACTLY)
        {
            // We were told how big to be
            result = specSize;
        }
        else
        {
            // Set the size
            result = 200;
        }
        return result;
    }

    /**
     * Determines the height of this view
     * @param measureSpec A measureSpec packed into an int
     * @return The height of the view, honoring constraints from measureSpec
     */
    private int measureHeight(int measureSpec)
    {
        int result = 0;
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);

        if (specMode == MeasureSpec.EXACTLY)
        {
            // We were told how big to be
            result = specSize;
        }
        else
        {
        	//Set size
            result = 200;
        }
        return result;
    }
}
