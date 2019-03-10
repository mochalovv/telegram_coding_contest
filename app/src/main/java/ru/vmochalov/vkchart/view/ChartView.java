package ru.vmochalov.vkchart.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import java.text.DateFormat;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import ru.vmochalov.vkchart.dto.CombinedChart;
import timber.log.Timber;

/**
 * Created by Vladimir Mochalov on 10.03.2019.
 */
public class ChartView extends View {

    private static DateFormat dateFormat = new SimpleDateFormat("MMM d");
    private int height;

    private int width;

    private int backgroundColor = Color.WHITE;

    private int axesColor = Color.GRAY;

    private CombinedChart combinedChart;

    private boolean[] chartsVisibility;

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

        drawAxes(canvas);
        drawChart(canvas);


    }

    Paint paint = new Paint();

    private void drawBackground(Canvas canvas) {
        paint.setColor(backgroundColor);

        canvas.drawRect(0, 0, width, height, paint);

    }

    private void drawAxes(Canvas canvas) {
        paint.setColor(axesColor);
        paint.setTextSize(axesTextSize);
        paint.setTextAlign(Paint.Align.LEFT);

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

        List<Date> abscissa = combinedChart.getAbscissa();
        int firstDateIndex = 0;
        int lastDateIndex = abscissa.size() - 1;

        int step = (lastDateIndex - firstDateIndex) / 5;

        canvas.drawText(dateFormat.format(abscissa.get(firstDateIndex)), 0 + axesTextMargin, height - axesTextMargin, paint);

        paint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText(dateFormat.format(abscissa.get(lastDateIndex)), width - axesTextMargin, height - axesTextMargin, paint);

        //todo: draw six time labels and then draw chart
//        canvas.drawText();
//        for(int i = 0; i < 6; i++) {
//        }
    }

    private int bottomAxisMargin = 20;
    private int topAxisMargin = 20;
    private int levelsCount = 6;
    private int axesTextSize = 20;
    private int axesTextMargin = 4;

    private void drawChart(Canvas canvas) {

        paint.setColor(Color.BLACK);

//        if (chart != null) {
//            canvas.drawText(chart.getName(), 100, 100, paint);
//        }

    }

    private int maxValue;

    private Date minDate;

    private Date maxDate;

    public void setChart(CombinedChart combinedChart) {
        this.combinedChart = combinedChart;

        this.chartsVisibility = new boolean[combinedChart.getLabels().size()];
        this.maxValue = getMaxValue(combinedChart.getOrdinates());

        List<Date> abscissa = combinedChart.getAbscissa();
        this.minDate = abscissa.get(0); //Collections.min(chart.getPoints().keySet());
        this.maxDate = abscissa.get(abscissa.size() - 1); //Collections.max(chart.getPoints().keySet());

        invalidate();

        Timber.d("maxValue: " + maxValue);
        Timber.d("minDate: " + minDate);
        Timber.d("maxDate: " + maxDate);
    }

    private Integer getMaxValue(List<List<Integer>> values) {
        Integer max = Integer.MIN_VALUE;

        for (List<Integer> list : values) {
            max = Math.max(max, Collections.max(list));
        }

        return max;
    }

}
