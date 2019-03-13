package ru.vmochalov.vkchart.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import ru.vmochalov.vkchart.dto.CombinedChart;
import timber.log.Timber;

public class ChartNavigationView extends View {

    private float width;
    private float height;

    private static final int firstDateIndex = 0;
    private int lastDateIndex;
    private int periodStartDateIndex;
    private int periodEndDateIndex;

    private CombinedChart combinedChart;

    private boolean[] chartsVisibility;

    // styleable attributes
    private float lineStrokeWidth = 5;
    private float frameHorizontalBorderWidth = 10;
    private float frameVerticalBorderWidth = 2;

    private PeriodChangedListener periodChangedListener = new PeriodChangedListener() {
        @Override
        public void onPeriodLengthChanged(int periodStart, int periodEnd) {
            Timber.d("onPeriodLengthChanged; periodStart: " + periodStart + ", periodEnd: " + periodEnd);
        }

        @Override
        public void onPeriodMoved(int periodStart, int periodEnd) {
            Timber.d("onPeriodMoved; periodStart: " + periodStart + ", periodEnd: " + periodEnd);
        }
    };

    public interface PeriodChangedListener {
        void onPeriodLengthChanged(int periodStart, int periodEnd);

        void onPeriodMoved(int periodStart, int periodEnd);
    }

    public ChartNavigationView(Context context) {
        super(context);
    }

    public ChartNavigationView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    public ChartNavigationView(Context context, AttributeSet attributeSet, int defStyleAttr) {
        super(context, attributeSet, defStyleAttr);
    }

    public ChartNavigationView(Context context, AttributeSet attributeSet, int defStyleAttr, int defStyleRes) {
        super(context, attributeSet, defStyleAttr, defStyleRes);
    }

    public void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        width = right - left;
        height = bottom - top;
    }

    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        //todo: send prefered size as in arrival
    }

    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        drawBackground(canvas);
    }

    private Paint activeBackgroundPaint = new Paint();
    private Paint passiveBackgroundPaint = new Paint();
    private Paint framePaint = new Paint();
//    private Path framePath = new Path();

    int firstPassiveStartPixel = 0;
    int activeStartPixel = 200;
    int secondPassiveStartPixel = 500;

    private void drawBackground(Canvas canvas) {
        activeBackgroundPaint.setColor(Color.WHITE); //todo: not only white, but also gray
        activeBackgroundPaint.setStyle(Paint.Style.FILL_AND_STROKE);

        passiveBackgroundPaint.setColor(Color.GRAY);
        passiveBackgroundPaint.setStyle(Paint.Style.FILL_AND_STROKE);

        framePaint.setColor(Color.RED);
        framePaint.setStyle(Paint.Style.FILL_AND_STROKE);


        // draw passive background
        canvas.drawRect(firstPassiveStartPixel, 0, activeStartPixel - 1, height, passiveBackgroundPaint);
        canvas.drawRect(secondPassiveStartPixel, 0, width, height, passiveBackgroundPaint);

        // draw active background
        canvas.drawRect(activeStartPixel, 0, secondPassiveStartPixel - 1, height, activeBackgroundPaint);

        // draw frame
        // todo: should it be path?? or rect??
//        framePaint.setStrokeWidth(frameHorizontalBorderWidth);
        canvas.drawRect(activeStartPixel, 0, secondPassiveStartPixel - 1, frameVerticalBorderWidth, framePaint);
        canvas.drawRect(activeStartPixel, height - frameVerticalBorderWidth, secondPassiveStartPixel - 1, height, framePaint);

        canvas.drawRect(activeStartPixel, 0, activeStartPixel + frameHorizontalBorderWidth, height, framePaint);
        canvas.drawRect(secondPassiveStartPixel - 1 - frameHorizontalBorderWidth, 0, secondPassiveStartPixel - 1, height, framePaint);
    }

    public void drawChart(Canvas canvas) {
        //todo: implement drawing canvas
    }

    public void setCombinedChart(CombinedChart combinedChart) {
        this.combinedChart = combinedChart;

        this.lastDateIndex = combinedChart.getAbscissa().size() - 1;

        this.periodStartDateIndex = 20;
        this.periodEndDateIndex = 50;

        invalidate();
    }

}
