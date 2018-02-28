package com.viggi.lib_graph;

import android.app.ProgressDialog;
import android.graphics.Color;
import android.graphics.PointF;
import android.util.FloatMath;
import android.view.MotionEvent;
import android.view.View;

import com.androidplot.Plot;
import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.PanZoom;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.StepModelFit;
import com.androidplot.xy.XYGraphWidget;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.ZoomEstimator;

import java.text.DecimalFormat;

/**
 * Created by dave on 8/27/17.
 */

public class APZoomListener /* implements View.OnTouchListener */
{
    private static final int NUM_GRIDLINES = 5;
    private XYPlot plot;
    private PanZoom panZoom;

    public APZoomListener(XYPlot xyPlot, double xMax, double yMax)
    {
        plot = xyPlot;

        // set a fixed origin and a "by-value" step mode so that grid lines will
        // move dynamically with the data when the users pans or zooms:
        plot.setUserDomainOrigin(0);
        plot.setUserRangeOrigin(0);

        // predefine the stepping of both axis
        // increment will be chosen from list to best fit NUM_GRIDLINES grid lines
        double[] inc_domain = new double[]{0.1,0.5,1,2,5,10,20,50,100,500};
        double[] inc_range  = new double[]{0.1,0.5,1,2,5,10,20,50,100,500};
        plot.setDomainStepModel(new StepModelFit(plot.getBounds().getxRegion(),inc_domain,NUM_GRIDLINES));
        plot.setRangeStepModel( new StepModelFit(plot.getBounds().getyRegion(),inc_range,NUM_GRIDLINES));

        //plot.getGraph().setLinesPerRangeLabel(2);
        //plot.getGraph().setLinesPerDomainLabel(2);
        //plot.getGraph().getBackgroundPaint().setColor(Color.TRANSPARENT);
//        plot.getGraph().getLineLabelStyle(XYGraphWidget.Edge.LEFT).
//                setFormat(new DecimalFormat("#####"));
//        plot.getGraph().getLineLabelStyle(XYGraphWidget.Edge.BOTTOM).
//                setFormat(new DecimalFormat("#####.#"));

        //plot.setBorderStyle(Plot.BorderStyle.NONE, null, null);

        panZoom = PanZoom.attach(plot, PanZoom.Pan.BOTH, PanZoom.Zoom.STRETCH_BOTH, PanZoom.ZoomLimit.MIN_TICKS);
        panZoom.setPan(PanZoom.Pan.BOTH);
        panZoom.setZoom(PanZoom.Zoom.STRETCH_BOTH);

        plot.getOuterLimits().set(0, xMax, 0, yMax);

        // enable autoselect of sampling level based on visible boundaries:
        plot.getRegistry().setEstimator(new ZoomEstimator());

        reset();
    }

    private void reset()
    {
        plot.setDomainBoundaries(0, 10000, BoundaryMode.FIXED);
        plot.setRangeBoundaries(0, 1000, BoundaryMode.FIXED);
        plot.redraw();
    }
}
