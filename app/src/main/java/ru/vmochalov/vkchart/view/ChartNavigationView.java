package ru.vmochalov.vkchart.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.Arrays;
import java.util.List;

import ru.vmochalov.vkchart.dto.CombinedChart;
import timber.log.Timber;

public class ChartNavigationView extends View {

    private float width;
    private float height;

    private static final int firstDateIndex = 0;
    private int lastDateIndex;
    private int periodStartDateIndex;
    private int periodEndDateIndex;

    private int maxValue;

    private CombinedChart combinedChart;

    private boolean[] chartsVisibility;

    // styleable attributes
    private float lineStrokeWidth = 5;
    private float frameHorizontalBorderWidth = 10;
    private float frameVerticalBorderWidth = 4;

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
        initTouchListener();
    }

    public ChartNavigationView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        initTouchListener();
    }

    public ChartNavigationView(Context context, AttributeSet attributeSet, int defStyleAttr) {
        super(context, attributeSet, defStyleAttr);
        initTouchListener();
    }

    public ChartNavigationView(Context context, AttributeSet attributeSet, int defStyleAttr, int defStyleRes) {
        super(context, attributeSet, defStyleAttr, defStyleRes);
        initTouchListener();
    }

    private void initTouchListener() {
        setOnTouchListener(
                new OnTouchListener() {

                    private boolean isTouchHandled = false;

                    private float previousX;
                    private float dx;
                    private float x;

                    @Override
                    public boolean onTouch(View v, MotionEvent event) {

                        if (event.getAction() == MotionEvent.ACTION_DOWN) {
                            x = event.getX();
                            previousX = event.getX();

                            isTouchHandled = (x >= frameStart && x <= frameStart + frameWidth);
                        } else if (event.getAction() == MotionEvent.ACTION_UP) {
                            isTouchHandled = false;
                        } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
//                            x = (event.getX() < 0) ? 0 : event.getX();
                            if (isTouchHandled) {
                                if (event.getX() < 0) {
                                    x = 0;
                                } else if (event.getX() > width) {
                                    x = width;
                                } else {
                                    x = event.getX();
                                }

                                dx = x - previousX;
                                previousX = x;

                                frameStart += dx;

                                if (frameStart < 0) {
                                    frameStart = 0;
                                } else if (frameStart > width - frameWidth) {
                                    frameStart = width - frameWidth;
                                }
//                            secondPassiveStartPixel += dx;
                            }
                            Timber.d("onTouch; event: " + event.toString());

                            ChartNavigationView.this.invalidate();
                        }
                        return true;
                    }
                }
        );
//        float firstPassiveStartPixel = 0;
//        float activeStartPixel = 200;
//        float secondPassiveStartPixel = 500;


//        setOnDragListener(
//                new OnDragListener() {
//                    @Override
//                    public boolean onDrag(View v, DragEvent event) {
//                        Timber.d("onDrag; event: " + event.toString());
//                        return true;
//                    }
//                }
//        );

//        setOnGenericMotionListener(
//                new OnGenericMotionListener() {
//                    @Override
//                    public boolean onGenericMotion(View v, MotionEvent event) {
//                        Timber.d("onGenericMotion; event: " + event.toString());
//                        return false;
//                    }
//                }
//        );
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
        drawChart(canvas);
    }

    private Paint activeBackgroundPaint = new Paint();
    private Paint framePaint = new Paint();

    float firstPassiveStartPixel = 0;
    //    float activeStartPixel = 200;
    float frameStart = 200;
    float frameWidth = 300;
//    float secondPassiveStartPixel = 500;

    private int frameColor = Color.rgb(219, 231, 240);
    private int passiveBackgroundColor = Color.argb(0xa0, 245, 248, 249);

    private Rect firstPassiveBackground;
    private Rect secondPassiveBackground;

    private void drawBackground(Canvas canvas) {
        activeBackgroundPaint.setColor(Color.WHITE); //todo: not only white, but also gray
        activeBackgroundPaint.setStyle(Paint.Style.FILL_AND_STROKE);

        framePaint.setColor(frameColor);
        framePaint.setStyle(Paint.Style.FILL_AND_STROKE);

        // draw passive background later, after chart is drawn
        firstPassiveBackground = new Rect((int) firstPassiveStartPixel, 0, (int) frameStart - 1, (int) height);
        secondPassiveBackground = new Rect((int) (frameStart + frameWidth), 0, (int) width, (int) height);

        // draw active background
        canvas.drawRect(0, 0, width, height, activeBackgroundPaint);

        // draw frame
        // todo: should it be path?? or rect??
        canvas.drawRect(frameStart, 0, frameStart + frameWidth - 1, frameVerticalBorderWidth, framePaint);
        canvas.drawRect(frameStart, height - frameVerticalBorderWidth, frameStart + frameWidth - 1, height, framePaint);

        canvas.drawRect(frameStart, 0, frameStart + frameHorizontalBorderWidth, height, framePaint);
        canvas.drawRect(frameStart + frameWidth - 1 - frameHorizontalBorderWidth, 0, frameStart + frameWidth - 1, height, framePaint);
    }

    private float topChartPadding = 3;
    private float bottomChartPadding = 3;

    private Paint chartPaintActive = new Paint();
    private float lineWidth = 2;

    public void drawChart(Canvas canvas) {

        chartPaintActive.setStyle(Paint.Style.STROKE);
        chartPaintActive.setStrokeWidth(lineWidth);

        float xStep = width / lastDateIndex;
        float yStep = (height - topChartPadding - bottomChartPadding) / maxValue;

        Path activePath = new Path();

        for (int chartIndex = 0; chartIndex < combinedChart.getLineIds().size(); chartIndex++) {

            if (!chartsVisibility[chartIndex]) continue; // do not show muted charts

            int color = combinedChart.getColors().get(chartIndex);
            chartPaintActive.setColor(color);

            activePath.reset();

            List<Integer> ordinate = combinedChart.getOrdinates().get(chartIndex);

            //put first point
            float x;
            float y;

            for (int j = 0; j <= lastDateIndex; j++) {
                if (j == 0) {
                    x = 0;
                    y = 0 + height + topChartPadding - ordinate.get(0) * yStep;
                } else if (j == lastDateIndex) {
                    x = width; // todo: or width - 1??
                    y = 0 + height + topChartPadding - ordinate.get(lastDateIndex) * yStep;
                } else {
                    x = j * xStep;
                    y = height + topChartPadding - ordinate.get(j) * yStep;
                }

                if (j == 0) {
                    activePath.moveTo(x, y);
                } else {
                    activePath.lineTo(x, y);
                }
            }

            canvas.drawPath(activePath, chartPaintActive);
        }

        Paint duff = new Paint();
        duff.setStyle(Paint.Style.FILL_AND_STROKE);
        duff.setColor(passiveBackgroundColor);
        duff.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.OVERLAY));

        canvas.drawRect(firstPassiveBackground, duff);
        canvas.drawRect(secondPassiveBackground, duff);
    }

    public void setCombinedChart(CombinedChart combinedChart) {
        this.combinedChart = combinedChart;
        this.maxValue = combinedChart.getMaxValue();
        this.lastDateIndex = combinedChart.getAbscissa().size() - 1;

        this.lastDateIndex = combinedChart.getAbscissa().size() - 1;

        chartsVisibility = new boolean[combinedChart.getLineIds().size()];
        Arrays.fill(chartsVisibility, true);

        this.periodStartDateIndex = 20;
        this.periodEndDateIndex = 50;

        invalidate();
    }

}
