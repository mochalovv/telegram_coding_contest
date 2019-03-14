package ru.vmochalov.vkchart.view;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import ru.vmochalov.vkchart.R;
import ru.vmochalov.vkchart.dto.CombinedChart;
import timber.log.Timber;

/**
 * Created by Vladimir Mochalov on 10.03.2019.
 */
public class ChartView extends View {

    private static DateFormat dateFormat = new SimpleDateFormat("MMM d");
    private float height;

    private float width;

    private int backgroundColor = Color.WHITE;

    private int axesColor = Color.GRAY;

    private CombinedChart combinedChart;

    private boolean[] chartsVisibility;

    // styleable attributes
    private int axisTextSize;
    private int lineStrokeWidth;
    private int axisStrokeWidth;

    public ChartView(Context context) {
        super(context);
    }

    public ChartView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);

        handleAttributeSet(attributeSet);
    }

    public ChartView(Context context, AttributeSet attributeSet, int defStyleAttr) {
        super(context, attributeSet, defStyleAttr);

        handleAttributeSet(attributeSet);
    }

    public ChartView(Context context, AttributeSet attributeSet, int defStyleAttr, int defStyleRes) {
        super(context, attributeSet, defStyleAttr, defStyleRes);

        handleAttributeSet(attributeSet);
    }

    private void handleAttributeSet(AttributeSet attributeSet) {
        TypedArray attributes = getContext().obtainStyledAttributes(attributeSet, R.styleable.ChartView);
        Resources resources = getContext().getResources();

        axisTextSize = attributes.getDimensionPixelSize(
                R.styleable.ChartView_axisTextSize,
                resources.getDimensionPixelSize(R.dimen.chartViewDefaultTextSize)
        );

        lineStrokeWidth = attributes.getDimensionPixelSize(
                R.styleable.ChartView_chartLineWidth,
                resources.getDimensionPixelSize(R.dimen.chartViewDefaultLineStrokeWidth)
        );

        axisStrokeWidth = attributes.getDimensionPixelSize(
                R.styleable.ChartView_axisLineWidth,
                resources.getDimensionPixelSize(R.dimen.chartViewDefaultAxisStrokeWidth)
        );

        attributes.recycle();
    }

    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        Timber.d("onMeasure; width: " + MeasureSpec.toString(widthMeasureSpec) + " , height: " + MeasureSpec.toString(heightMeasureSpec));

//        height = MeasureSpec.getSize(heightMeasureSpec);
//        width = MeasureSpec.getSize(widthMeasureSpec);
        //todo: send preferred size

    }

    public void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        Timber.d("onLayout; changed: " + changed + ", left: " + left + ", top: " + top + ", right: " + right + ", bottom: " + bottom);

        height = bottom - top; //MeasureSpec.getSize(heightMeasureSpec);
        width = right - left; //MeasureSpec.getSize(widthMeasureSpec);

    }

    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        Timber.d("onDraw");

        drawBackground(canvas);

        drawAxes(canvas);
        drawChart(canvas);

        //todo: update look&feel styles: texts, colors, stroke width, spaces, etc...

    }

    Paint paint = new Paint();

    private void drawBackground(Canvas canvas) {
        paint.setColor(backgroundColor);
        paint.setStyle(Paint.Style.FILL_AND_STROKE);

        canvas.drawRect(0, 0, width, height, paint);

    }

    private void drawAxes(Canvas canvas) {
        //todo: add axes changes when period is changed
        paint.setColor(axesColor);
//        paint.setTextSize(axesTextSize);
        paint.setTextSize(axisTextSize);
        paint.setStrokeWidth(axisStrokeWidth);
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setAntiAlias(true);
//        paint.setStyle();
        paint.setStyle(Paint.Style.FILL_AND_STROKE);

        int[] levelValues = new int[levelsCount]; // from bottom to top


        int levelDelta = maxValue / levelsCount;

        //calculationg background levels
        for (int i = 0; i < levelsCount; i++) {
            levelValues[i] = levelDelta * i;
        }

//        for (int i = levelsCount - 1; i >= 0; i--) {
//            levelValues[i] = maxValue / levelsCount * i;
//        }

        Timber.d("level values: " + Arrays.toString(levelValues));

        //drawing level lines
        float yDelta = (height - bottomAxisMargin - topAxisMargin) / levelsCount;

        Path path = new Path();
        for (int i = 0; i < levelsCount; i++) {
            float y = height - bottomAxisMargin - i * yDelta;
//            canvas.drawLine(0, y, width, y, paint);

            path.moveTo(0, y);
            path.lineTo(width, y);
            canvas.drawText(Integer.toString(levelValues[i]), 0 + axesTextMargin, y - axesTextMargin, paint);
        }

        canvas.drawPath(path, paint);

        List<Date> abscissa = combinedChart.getAbscissa();
        int firstDateIndex = 0;
        int lastDateIndex = abscissa.size() - 1;

        paint.setTextAlign(Paint.Align.CENTER);

        // draw first label
        String label = dateFormat.format(abscissa.get(firstDateIndex));
        float labelWidth = paint.measureText(label);
        float x = 0 + axesTextMargin + labelWidth / 2;
        float y = height - axesTextSize / 2;
        canvas.drawText(label, x, y, paint);


        // draw last label
        label = dateFormat.format(abscissa.get(lastDateIndex));
        labelWidth = paint.measureText(label);
        x = width - axesTextSize - labelWidth / 2;
        canvas.drawText(label, x, y, paint);

        // draw middle labels
        int indexStep = (lastDateIndex - firstDateIndex) / absLevelsCount;
        float xStep = (width - 2 * axesTextMargin) / (absLevelsCount - 1);

        for (int i = 1; i < absLevelsCount - 1; i++) {
            label = dateFormat.format(abscissa.get(i * indexStep)); //todo: handle last abscissa on chat properly
            labelWidth = paint.measureText(label);
            x = 0 + axesTextMargin + xStep * i - labelWidth / 2;
            canvas.drawText(label, x, y, paint);
        }
    }

    private int bottomAxisMargin = 40;
    private int topAxisMargin = 40;
    private int levelsCount = 6;
    private int absLevelsCount = 6;
    private int axesTextSize = 20;
    private int axesTextMargin = 4;


    private double startPercent = 0.5;
    private double endPercent = 1.0;

    public void setStartVisible(double startVisiblePercent) {
        //todo: add checks for 0.0 .. 1.0
        startPercent = startVisiblePercent;
        invalidate();
    }

    public void setEndVisible(double endVisiblePercent) {
        endPercent = endVisiblePercent;
        invalidate();
    }

    public void setVisibleRange(double startVisiblePercent, double endVisiblePercent) {
        startPercent = startVisiblePercent;
        endPercent = endVisiblePercent;
        invalidate();
    }

    private void drawChart(Canvas canvas) {

        double visibleWidth = width * (endPercent - startPercent);


        List<Date> abscissa = combinedChart.getAbscissa();
        int firstDateIndex = 0;
        int lastDateIndex = abscissa.size() - 1;

        Path path = new Path();

        float xStep = width / abscissa.size();
        xStep *= (width / visibleWidth);

        float enlargedWidth = (float) (width * width / visibleWidth);

        float x0 = (float) (-enlargedWidth * startPercent);


        float yStep = (height - bottomAxisMargin - topAxisMargin) / maxValue;

//        Timber.d("height: " + height);
//        Timber.d("width: " + width);
//        Timber.d("xStep: " + xStep);
//        Timber.d("yStep: " + yStep);

        paint.setStyle(Paint.Style.STROKE);

        for (int i = 0; i < combinedChart.getLineIds().size(); i++) {
            if (!chartsVisibility[i]) {
                continue; // skip muted charts
            }

            paint.setColor(combinedChart.getColors().get(i));
            paint.setStrokeWidth(lineStrokeWidth);
            path.reset();

            float previousX;
            float previousY;
            // put first point
            float x = x0 + 0;
            int value = combinedChart.getOrdinates().get(i).get(firstDateIndex);
            float y = height - bottomAxisMargin - value * yStep;
//            Timber.d("j: 0; x: " + x + " , y: " + y + " , value: " + value);


            if (x >= 0 && x <= width) {
                path.moveTo(x, y);
            }
            previousX = x;
            previousY = y;


            // put middle points
            for (int j = firstDateIndex + 1; j < lastDateIndex; j++) {
                x = x0 + j * xStep;
                value = combinedChart.getOrdinates().get(i).get(j);
                y = height - bottomAxisMargin - value * yStep;

                if (x >= 0 && x <= width) {
                    // put point
                    if (path.isEmpty()) {
                        path.moveTo(previousX, previousY);
                    }

                    path.lineTo(x, y);
                }

                if (x > width && previousX <= width) {
                    path.lineTo(x, y);
                }

//                path.lineTo(x, y);

//                Timber.d("j: " + j + "; x: " + x + " , y: " + y + " , value: " + value);

                previousX = x;
                previousY = y;
            }

            // put last point
            x = x0 + enlargedWidth; //width;
            value = combinedChart.getOrdinates().get(i).get(lastDateIndex);
            y = height - bottomAxisMargin - value * yStep;


            if (previousX <= width) {
                path.lineTo(x, y);
            }


//            path.lineTo(x, y);
//            Timber.d("j: " + lastDateIndex + "; x: " + x + " , y: " + y + " , value: " + value);

            canvas.drawPath(path, paint);
        }
    }

    private int maxValue;

    private Date minDate;

    private Date maxDate;

    public void setChart(CombinedChart combinedChart) {
        this.combinedChart = combinedChart;

        this.chartsVisibility = new boolean[combinedChart.getLabels().size()];

        Arrays.fill(chartsVisibility, true);
        this.maxValue = combinedChart.getMaxValue();

        List<Date> abscissa = combinedChart.getAbscissa();
        this.minDate = abscissa.get(0); //Collections.min(chart.getPoints().keySet());
        this.maxDate = abscissa.get(abscissa.size() - 1); //Collections.max(chart.getPoints().keySet());

        invalidate();

        Timber.d("maxValue: " + maxValue);
        Timber.d("minDate: " + minDate);
        Timber.d("maxDate: " + maxDate);
    }

}
