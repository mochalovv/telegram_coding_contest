package ru.vmochalov.vkchart.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

import ru.vmochalov.vkchart.dto.Chart;
import timber.log.Timber;

/**
 * Created by Vladimir Mochalov on 10.03.2019.
 */
public class ChartView extends View {

    private int height;

    private int width;

    private int backgroundColor = Color.WHITE;

    private int axesColor = Color.GRAY;

    private Chart chart;

    public ChartView(Context context) {
        super(context);
    }

    public ChartView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    public ChartView(Context context, AttributeSet attributeSet, int defStyleAttr) {
        super(context, attributeSet, defStyleAttr);
    }

    public ChartView(Context context, AttributeSet attributeSet, int defStyleAttr, int defStyleRes) {
        super(context, attributeSet, defStyleAttr, defStyleRes);
    }

    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        Timber.d("onMeasure; width: " + MeasureSpec.toString(widthMeasureSpec) + " , height: " + MeasureSpec.toString(heightMeasureSpec));

        height = MeasureSpec.getSize(heightMeasureSpec);
        width = MeasureSpec.getSize(widthMeasureSpec);

    }

    public void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        Timber.d("onLayout");
    }

    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        Timber.d("onDraw");

        drawBackground(canvas);

        if (chart != null) {
            drawAxes(canvas, chart);
            drawChart(canvas, chart);
        }


    }

    Paint paint = new Paint();

    private void drawBackground(Canvas canvas) {
        paint.setColor(backgroundColor);

        canvas.drawRect(0, 0, width, height, paint);

    }

    private void drawAxes(Canvas canvas, Chart chart) {
        paint.setColor(axesColor);
        paint.setTextSize(axesTextSize);

        int[] levelValues = new int[levelsCount];

        for (int i = levelsCount - 1; i >= 0; i--) {
            levelValues[i] = maxValue / levelsCount * i;
        }

        Timber.d("level values: " + Arrays.toString(levelValues));
        for (int i = 1; i <= levelsCount; i++) {

            int y = topAxisMargin + (height - topAxisMargin - bottomAxisMargin) / levelsCount * i;
            canvas.drawLine(0, y, width, y, paint);

            canvas.drawText(Integer.toString(levelValues[levelsCount - i]), 0 + axesTextMargin, y - axesTextMargin, paint);

        }
    }

    private int bottomAxisMargin = 20;
    private int topAxisMargin = 20;
    private int levelsCount = 6;
    private int axesTextSize = 20;
    private int axesTextMargin = 4;

    private void drawChart(Canvas canvas, Chart chart) {

        paint.setColor(Color.BLACK);

        if (chart != null) {
            canvas.drawText(chart.getName(), 100, 100, paint);
        }

    }

    private int maxValue;

    private Date minDate;

    private Date maxDate;

    public void setChart(Chart chart) {
        this.chart = chart;
        this.maxValue = Collections.max(chart.getPoints().values());

        this.minDate = Collections.min(chart.getPoints().keySet());
        this.maxDate = Collections.max(chart.getPoints().keySet());

        invalidate();
    }

}
