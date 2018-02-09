package com.viggi.lib_graph;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.viggi.lib_datestring.DateStrings;
import com.viggi.lib_file.ErrorFile;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

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

/**
 * Created by Dave on 2/22/2015.
 */
public class GraphView extends View
{
    private static final float GraphFontSize = 40; //was 12
    //private int mDrawCount = 0;

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
        Title = new GraphLabel(true, GraphFontSize);

        LeftAxis = new GraphAxis(GraphAxis.EdgeType.LEFT, GraphFontSize);
        RightAxis = new GraphAxis(GraphAxis.EdgeType.RIGHT, GraphFontSize);
        BottomAxis = new GraphAxis(GraphAxis.EdgeType.BOTTOM, GraphFontSize);
        TopAxis = new GraphAxis(GraphAxis.EdgeType.TOP, GraphFontSize);
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

        mDataRangeOverride = null;
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
                LeftAxis = new GraphAxis(GraphAxis.EdgeType.LEFT, GraphFontSize);
            if(RightAxis == null)
                RightAxis = new GraphAxis(GraphAxis.EdgeType.RIGHT, GraphFontSize);
            if(BottomAxis == null)
                BottomAxis = new GraphAxis(GraphAxis.EdgeType.BOTTOM, GraphFontSize);
            if(TopAxis == null)
                TopAxis = new GraphAxis(GraphAxis.EdgeType.TOP, GraphFontSize);

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

    public void EasyGraph(float[] data, FloatRectangle dataBounds)
    {
        EasyGraph(data);
        mDataRangeOverride = dataBounds;
    }

    public void EasyLineGraph(float[] xValues, float[] yValues, FloatRectangle dataBounds)
    {
        try
        {
            //Draw the default graph
            ClearGraph();

            mDataRangeOverride = dataBounds;

            Title.Text = "Scatter plot";

            Plots.add(new GraphPlot(xValues, yValues));
            Plots.get(0).SetColor(Color.RED);
            Plots.get(0).DrawLine = true;
            Plots.get(0).DrawPoints = true;

            if(LeftAxis == null)
                LeftAxis = new GraphAxis(GraphAxis.EdgeType.LEFT, GraphFontSize);
            if(RightAxis == null)
                RightAxis = new GraphAxis(GraphAxis.EdgeType.RIGHT, GraphFontSize);
            if(BottomAxis == null)
                BottomAxis = new GraphAxis(GraphAxis.EdgeType.BOTTOM, GraphFontSize);
            if(TopAxis == null)
                TopAxis = new GraphAxis(GraphAxis.EdgeType.TOP, GraphFontSize);

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
                LeftAxis = new GraphAxis(GraphAxis.EdgeType.LEFT);
            if(RightAxis == null)
                RightAxis = new GraphAxis(GraphAxis.EdgeType.RIGHT);
            if(BottomAxis == null)
                BottomAxis = new GraphAxis(GraphAxis.EdgeType.BOTTOM);
            if(TopAxis == null)
                TopAxis = new GraphAxis(GraphAxis.EdgeType.TOP);

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

        UpdateParams();


        Log.i("GraphView.Draw", String.format("Graph bounds: %d-%d, %d-%d", mGraphBounds.Left, mGraphBounds.Right, mGraphBounds.Bottom, mGraphBounds.Top));
        //mDrawCount++;

        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        canvas.drawRect(0, 0, mTotalWidth, mTotalHeight, paint);
        //Draw all of the elements for the graph
        //Log.e("GraphView", String.format("Number of elements: %d", mGraphElements.size()));
        for(int i=0; i<mGraphElements.size(); i++)
        {
            //Log.e("GraphView", String.format("Drawing element %s", mGraphElements.get(i).GetName()));
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
        Bitmap ret = Bitmap.createBitmap(mTotalWidth, mTotalHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas();
        canvas.setBitmap(ret);

        Draw(canvas);

        return ret;
    }

    public Bitmap GetBlankBitmap()
    {
        Bitmap ret = Bitmap.createBitmap(mTotalWidth, mTotalHeight, Bitmap.Config.ARGB_8888);
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

        Log.i("GraphView", String.format("AddDateInfo: %d dates, %d total days", numDates, totalDays));

        if(totalDays < numDates)
            totalDays = numDates;

        List<GraphLabel> labels = new ArrayList<GraphLabel>();
        int graphWidth = mGraphBounds.GetWidth();
        float pixelRatio = (float) graphWidth / (totalDays - 1);
        int lastMonthDay = -1;
        int blue = Color.rgb(0, 0, 64);
        for(int i=0; i<numDates; i++)
        {
            Calendar curDate = dates.get(i);
            Log.i("GraphView", "Adding date info for " + DateStrings.GetDateTimeString(curDate));

            //Add weekend shading and first-of-month lines/labels
            float x = (pixelRatio * DateStrings.GetActiveDiffInDays(dates.get(0), curDate, 0)) + mGraphBounds.Left;
            int curDay = curDate.get(Calendar.DAY_OF_WEEK) % 7;
            int monthDay = curDate.get(Calendar.DAY_OF_MONTH);
            int month = curDate.get(Calendar.MONTH) + 1;
            int year = curDate.get(Calendar.YEAR);
            if(lastMonthDay != monthDay) //TODO: Should probably use day of year here instead
            {
                if( totalDays < 370 && (curDay == 0 || curDay == 1))
                {
                    Log.i("GraphView", "Adding weekend shading for " + DateStrings.GetDateTimeString(curDate));
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

                if(labelDates)
                {
                    boolean drawTick = false;
                    int lineThickness = 0;
                    String labelText = null;

                    //if(i == 0 || i == numDates - 1)
                    //	drawTick = true;

                    if(totalDays < 30)
                    {
                        //Label each day
                        labelText = String.format(Locale.getDefault(), "%d", monthDay);
                        drawTick = true;
                    }
                    else if(totalDays < 60)
                    {
                        drawTick = true;

                        //Label each Saturday
                        if(curDay == 0)
                            labelText = String.format(Locale.getDefault(), "%d", monthDay);
                    }
                    else if(monthDay == 1)
                    {
                        drawTick = true;
                        if(totalDays < 730) //Label start-of-month
                            labelText = String.format(Locale.getDefault(), "%d", month);
                        else if(month == 1) //Label start-of-year
                            labelText = String.format(Locale.getDefault(), "%d", year);
                    }
                    else if(totalDays < 730 && curDay == 0)
                    {
                        drawTick = true;
                    }

                    if(monthDay == 1)
                    {
                        //Draw line for start-of-month
                        lineThickness = 1;
                        if(month == 1 || month == 7) //Make January and July thicker lines
                            lineThickness = 3;
                    }

                    if(lineThickness > 0)
                    {
                        GraphLine line = new GraphLine(new Point((int)x, mGraphBounds.Top), new Point((int)x, mGraphBounds.Bottom), lineThickness);
                        line.SetColor(Color.DKGRAY);
                        Lines.add(line);
                    }

                    if(drawTick)
                    {
                        if(labelText == null)
                            labelText = "";

                        //Log.i("GraphView", "Adding label: '" + labelText + "'");

                        GraphLabel label = new GraphLabel(new Point((int)x, mGraphBounds.Bottom + BottomAxis.TickLength + 2), labelText, GraphFontSize);
                        labels.add(label);
                    }
                }
            }
            lastMonthDay = monthDay;
        }

        BottomAxis.SetLabels(labels, mGraphBounds);
    }

    public void AddDataPoint(float x, float y, int color)
    {
        UpdateParams();

        GraphPoint graphPoint = new GraphPoint(null, x, y, 2, GraphPoint.PointType.CROSS);
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
        int left = LeftAxis == null ? 0 : LeftAxis.FindGraphEdge(0, mTotalWidth);
        int right = RightAxis == null ? mTotalWidth : RightAxis.FindGraphEdge(0, mTotalWidth);
        int top = Title == null ? 0 : Title.GetExtent(new Point(mTotalWidth, mTotalHeight)).GetHeight() + 5;
        int bottom = BottomAxis == null ? mTotalHeight : BottomAxis.FindGraphEdge(0, mTotalHeight);

        return new GraphRectangle(left, top, right, bottom);
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
        int specSize = View.MeasureSpec.getSize(measureSpec);

        if (specMode == View.MeasureSpec.EXACTLY)
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
