package com.viggi.lib_graph;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;

import com.viggi.lib_math.ArrayMath;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Dave on 2/22/2015.
 */
public class GraphAxis implements IGraphElement
{
    public EdgeType Edge = EdgeType.BOTTOM;
    private int mGraphEdge = 0;
    public GraphLine Axis = null;
    public List<GraphLabel> Labels = new ArrayList<GraphLabel>();
    public List<GraphLine> Ticks = new ArrayList<GraphLine>();
    public List<GraphLine> Lines = new ArrayList<GraphLine>();
    public Boolean DrawLines = true;
    public Boolean DrawLabels = true;

    private Paint mPaint = new Paint();
    private String[] mLabels = null;
    public int TickLength = 10;
    public float FontSize = 20;

    public int PolarRadius = -1;

    public GraphAxis(EdgeType type)
    {
        this(type, null, 20);
    }

    public GraphAxis(EdgeType type, float fontSize)
    {
        this(type, null, fontSize);
    }

    public GraphAxis(EdgeType type, String[] labels, float fontSize)
    {
        Edge = type;
        mLabels = labels;
        FontSize = fontSize;
        SetColor(Color.DKGRAY);
        SetLabels(mLabels);
    }

    public GraphAxis(int radius)
    {
        Edge = EdgeType.POLAR;
        PolarRadius = radius;
        SetLabels(null);
        SetColor(Color.DKGRAY);
    }

    public int FindGraphEdge(int axisOffset, int dimensionSize)
    {
        int labelSpace = 60; //was 20
        int tickSpace = TickLength;
        if(mLabels == null ||mLabels.length == 0)
        {
            labelSpace = 0;
            tickSpace = 0;
        }

        if(Edge == EdgeType.LEFT || Edge == EdgeType.TOP)
        {
            mGraphEdge = axisOffset + labelSpace + tickSpace;
        }
        else if(Edge == EdgeType.RIGHT || Edge == EdgeType.BOTTOM)
        {
            mGraphEdge = dimensionSize - axisOffset - labelSpace - tickSpace;
        }
        else if(Edge == EdgeType.POLAR)
        {
            mGraphEdge = dimensionSize / 2 - PolarRadius;
        }
        return mGraphEdge;
    }

    public void SetLabels(String[] labels)
    {
        Labels.clear();
        Ticks.clear();
        Lines.clear();
        mLabels = labels;
        if(mLabels != null && mLabels.length > 0)// && !mLabels[0].equals(""))
            for(int i=0; i<mLabels.length; i++)
            {
                Labels.add(new GraphLabel(null, mLabels[i], FontSize));
                GraphLine tick = new GraphLine();
                tick.SetColor(GetColor());
                Ticks.add(tick);
                if(DrawLines)
                {
                    GraphLine line = new GraphLine();
                    line.SetColor(GetColor());
                    Lines.add(line);
                }
                GraphLabel gl = Labels.get(i);
                switch(Edge)
                {
                    case LEFT:
                        gl.HAlign = Paint.Align.RIGHT;
                        gl.VAlign = GraphLabel.VerticalAlign.MIDDLE;
                        break;
                    case RIGHT:
                        gl.HAlign = Paint.Align.LEFT;
                        gl.VAlign = GraphLabel.VerticalAlign.MIDDLE;
                        break;
                    case TOP:
                        gl.HAlign = Paint.Align.CENTER;
                        gl.VAlign = GraphLabel.VerticalAlign.BOTTOM;
                        break;
                    case BOTTOM:
                        gl.HAlign = Paint.Align.CENTER;
                        gl.VAlign = GraphLabel.VerticalAlign.TOP;
                        break;
                    default:
                        break;
                }
            }
    }

    public void SetLabels(List<GraphLabel> labels, GraphRectangle bounds)
    {
        Labels.clear();
        Ticks.clear();
        Lines.clear();

        for(int i=0; i<labels.size(); i++)
        {
            GraphLabel label = labels.get(i);

            GraphLine tick = new GraphLine();
            tick.SetColor(GetColor());

            switch(Edge)
            {
                case LEFT:
                    label.HAlign = Paint.Align.RIGHT;
                    label.VAlign = GraphLabel.VerticalAlign.MIDDLE;
                    tick.Start = new Point(bounds.Left, label.Location.y);
                    tick.End = new Point(bounds.Left - TickLength, label.Location.y);
                    break;
                case RIGHT:
                    label.HAlign = Paint.Align.LEFT;
                    label.VAlign = GraphLabel.VerticalAlign.MIDDLE;
                    tick.Start = new Point(bounds.Right, label.Location.y);
                    tick.End = new Point(bounds.Right + TickLength, label.Location.y);
                    break;
                case TOP:
                    label.HAlign = Paint.Align.CENTER;
                    label.VAlign = GraphLabel.VerticalAlign.BOTTOM;
                    tick.Start = new Point(label.Location.x, bounds.Top);
                    tick.End = new Point(label.Location.x, bounds.Top - TickLength);
                    break;
                case BOTTOM:
                    label.HAlign = Paint.Align.CENTER;
                    label.VAlign = GraphLabel.VerticalAlign.TOP;
                    tick.Start = new Point(label.Location.x, bounds.Bottom);
                    tick.End = new Point(label.Location.x, bounds.Bottom + TickLength);
                    break;
                default:
                    break;
            }

            Labels.add(label);
            Ticks.add(tick);
        }
    }

    public void GenerateLabels(int min, int max, float labelMultiplier, boolean forceAll)
    {
        int range = max - min;
        //Auto-adjust the label interval so at-most 20 labels are shown
        int multiplier = 1;
        float actualLabelMultiplier = labelMultiplier;
        if(!forceAll)
        {
            //Find the best label interval to use
            int maxLabels = 16;
            int numLabels = range;

            //First try multiplier 1, 2, 3...
            for(multiplier = 1; multiplier <= 3; multiplier++)
                if(numLabels / multiplier + 1 <= maxLabels)
                    break;

            //If the multiplier is still too low, try 5, 10, 15, 20...
            if(numLabels / multiplier + 1 > maxLabels)
                for(multiplier = 5; numLabels / multiplier + 1 > maxLabels; multiplier += 5)
                { /*The for statement does all the work */ }

            actualLabelMultiplier *= multiplier;
        }

        int numLabels = range / multiplier + 1;
        //int minLabel = (int) (min / multiplier);
        //if(min % multiplier != 0)
        //	minLabel = minLabel * multiplier - multiplier;

        //Log.i("GraphAxis", String.format("Generating %d labels for %s between %d and %d", numLabels, Edge, min, max));
        if(range % multiplier > 0)
            numLabels++;
        String[] labels = new String[numLabels];
        for(int i = 0; i < numLabels; i++)
            if(labelMultiplier % 1 == 0)
                labels[i] = String.format("%d", Math.round(min + (i * actualLabelMultiplier)));
            else
                labels[i] = String.format("%.01f", min + (i * actualLabelMultiplier));
        SetLabels(labels);
    }

    public void GenerateLabels(List<GraphPlot> plots)
    {
        float max = Float.MIN_VALUE;
        float min = Float.MAX_VALUE;
        for(GraphPlot p : plots)
        {
            float localMax = ArrayMath.GetCeiling(p.Data);
            float localMin = (int) ArrayMath.GetMin(p.Data);
            //Log.i("GraphAxis", String.format("Plot range: %d, %d", localMin, localMax));
            if(localMax > max)
                max = localMax;
            if(localMin < min)
                min = localMin;
        }
        GenerateLabels((int) min, (int)max, 1, false);
    }

    public void Build(GraphRectangle bounds)
    {
        Point axisStart = new Point();
        Point axisEnd = new Point();

        switch(Edge)
        {
            case LEFT:
                axisStart.x = bounds.Left;
                axisStart.y = bounds.Top;
                axisEnd.x = bounds.Left;
                axisEnd.y = bounds.Bottom;
                break;
            case RIGHT:
                axisStart.x = bounds.Right;
                axisStart.y = bounds.Top;
                axisEnd.x = bounds.Right;
                axisEnd.y = bounds.Bottom;
                break;
            case TOP:
                axisStart.x = bounds.Left;
                axisStart.y = bounds.Top;
                axisEnd.x = bounds.Right - 1;
                axisEnd.y = bounds.Top;
                break;
            case BOTTOM:
                axisStart.x = bounds.Left;
                axisStart.y = bounds.Bottom;
                axisEnd.x = bounds.Right - 1;
                axisEnd.y = bounds.Bottom;
                break;
            case POLAR:
                int xMiddle = (bounds.Left + bounds.Right) / 2;
                int yMiddle = (bounds.Top + bounds.Bottom) / 2;
                axisStart.x = xMiddle;
                axisStart.y = yMiddle;
                if(PolarRadius <= 0)
                    PolarRadius = Math.min(xMiddle, yMiddle);
                break;
            default:
                break;
        }
        if(Edge == EdgeType.POLAR)
            Axis = new GraphCircle(axisStart, PolarRadius);
        else
            Axis = new GraphLine(axisStart, axisEnd, 1);

        Axis.SetColor(GetColor());

        if(Labels != null)
        {
            int axisHeight = bounds.Bottom - bounds.Top;
            int axisWidth = bounds.Right - bounds.Left;
            for(int i=0; i<Labels.size() && Labels.size() > 1; i++)
            {
                GraphLabel gl = Labels.get(i);
                GraphLine tick = Ticks.get(i);
                GraphLine line = null;
                if(DrawLines)
                    line = Lines.get(i);
                switch(Edge)
                {
                    case LEFT:
                        if(gl.Location == null)
                        {
                            gl.Location = new Point(0,0);
                            gl.Location.x = bounds.Left - TickLength - 2;
                            gl.Location.y = bounds.Bottom - Math.round(((float)i/(Labels.size() - 1)) * axisHeight);
                        }
                        if(tick.Start == null || tick.End == null)
                        {
                            tick.Start = new Point(bounds.Left, gl.Location.y);
                            tick.End = new Point(bounds.Left - TickLength, gl.Location.y);
                        }
                        if(DrawLines)
                        {
                            line.Start = new Point(bounds.Left, gl.Location.y);
                            line.End = new Point(bounds.Right - 1, gl.Location.y);
                        }
                        break;
                    case RIGHT:
                        if(gl.Location == null)
                        {
                            gl.Location = new Point(0,0);
                            gl.Location.x = bounds.Right + TickLength + 2;
                            gl.Location.y = bounds.Bottom - Math.round(((float)i/(Labels.size() - 1)) * axisHeight);
                        }
                        if(tick.Start == null || tick.End == null)
                        {
                            tick.Start = new Point(bounds.Right, gl.Location.y);
                            tick.End = new Point(bounds.Right + TickLength, gl.Location.y);
                        }
                        if(DrawLines)
                        {
                            line.Start = new Point(bounds.Left, gl.Location.y);
                            line.End = new Point(bounds.Right, gl.Location.y);
                        }
                        break;
                    case TOP:
                        if(gl.Location == null)
                        {
                            gl.Location = new Point(0,0);
                            gl.Location.x = bounds.Left + Math.round(((float)i/(Labels.size() - 1)) * axisWidth);
                            gl.Location.y = bounds.Top - TickLength - 2;
                        }
                        if(tick.Start == null || tick.End == null)
                        {
                            tick.Start = new Point(gl.Location.x, bounds.Top);
                            tick.End = new Point(gl.Location.x, bounds.Top - TickLength);
                        }
                        if(DrawLines)
                        {
                            line.Start = new Point(gl.Location.x, bounds.Top);
                            line.End = new Point(gl.Location.x, bounds.Bottom);
                        }
                        break;
                    case BOTTOM:
                        if(gl.Location == null)
                        {
                            gl.Location = new Point(0,0);
                            gl.Location.x = bounds.Left + Math.round(((float)i/(Labels.size() - 1)) * axisWidth);
                            gl.Location.y = bounds.Bottom + TickLength + 2;
                        }
                        if(tick.Start == null || tick.End == null)
                        {
                            tick.Start = new Point(gl.Location.x, bounds.Bottom);
                            tick.End = new Point(gl.Location.x, bounds.Bottom + TickLength);
                        }
                        if(DrawLines)
                        {
                            line.Start = new Point(gl.Location.x, bounds.Top);
                            line.End = new Point(gl.Location.x, bounds.Bottom);
                        }
                        break;
                    default:
                        break;
                }
            }
        }
    }

    public GraphRectangle GetExtent(Point viewSize)
    {
        GraphRectangle result = new GraphRectangle(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE);
        result.ExpandToEnclose(Axis.GetExtent(viewSize));
        if(Labels != null)
        {
            for(int i=0; i<Labels.size(); i++)
                result.ExpandToEnclose(Labels.get(i).GetExtent(viewSize));
            //for(int i=0; i<Ticks.size(); i++)
            //	result.ExpandToEnclose(Ticks.get(i).GetExtent());
        }

        return result;
    }

    public void Draw(Canvas canvas, GraphRectangle bounds, FloatRectangle dataBounds)
    {
        Build(bounds);
        if(Axis != null)
            Axis.Draw(canvas, bounds, dataBounds);
        if(Labels != null)
        {
            //Log.i("GraphAxis", String.format("Drawing %d labels for %s", Labels.size(), Edge));
            for(int i=0; i<Labels.size(); i++)
            {
                if(DrawLabels)
                    Labels.get(i).Draw(canvas, bounds, dataBounds);
                Ticks.get(i).Draw(canvas, bounds, dataBounds);
                if(DrawLines && i < Lines.size())
                    Lines.get(i).Draw(canvas, bounds, dataBounds);
            }
        }
    }

    public int GetColor()
    {
        return mPaint.getColor();
    }

    public void SetColor(int color)
    {
        mPaint.setColor(color);
    }

    public enum EdgeType
    {
        LEFT,
        TOP,
        RIGHT,
        BOTTOM,
        POLAR
    }

    @Override
    public String GetName()
    {
        return String.format("Axis-%s", Edge);
    }
}
