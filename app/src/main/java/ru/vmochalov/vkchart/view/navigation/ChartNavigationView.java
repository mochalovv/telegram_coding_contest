package ru.vmochalov.vkchart.view.navigation;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.util.AttributeSet;
import android.view.View;

import ru.vmochalov.vkchart.chart.Chart;
import ru.vmochalov.vkchart.view.detailed.ChartDrawDelegate;
import ru.vmochalov.vkchart.view.detailed.RedrawCallback;

public class ChartNavigationView extends View {

    private static final float INITIAL_FRAME_START_POSITION_PX = 0;
    private static final float INITIAL_FRAME_WIDTH_PX = 300;

    private Chart chart;

    // styleable attributes
    private float frameHorizontalBorderWidth = 10;
    private float frameVerticalBorderWidth = 4;

    private float lineWidth = 2;

    //todo: obtain from resources
    private int prefferedHeight = 75;

    private PeriodChangedListener periodChangedListener;

    private float frameStart = INITIAL_FRAME_START_POSITION_PX;
    private float frameWidth = INITIAL_FRAME_WIDTH_PX;

    private float topChartPadding = 3;
    private float bottomChartPadding = 3;

    private ChartDrawDelegate chartDrawDelegate;

    public interface FrameUpdatedListener {
        void onFrameUpdated(float start, float width);
    }

    private ChartNavigationTouchListener chartNavigationTouchListener = new ChartNavigationTouchListener(
            INITIAL_FRAME_START_POSITION_PX,
            INITIAL_FRAME_WIDTH_PX,
            new FrameUpdatedListener() {
                @Override
                public void onFrameUpdated(float start, float width) {
                    frameStart = start;
                    frameWidth = width;
                }
            }
    );

    public ChartNavigationView(Context context) {
        super(context);
        initTouchListener();

        initVariableForDrawing();
    }

    public ChartNavigationView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        initTouchListener();

        initVariableForDrawing();
    }

    public ChartNavigationView(Context context, AttributeSet attributeSet, int defStyleAttr) {
        super(context, attributeSet, defStyleAttr);
        initTouchListener();

        initVariableForDrawing();
    }

    private void initTouchListener() {
        setOnTouchListener(chartNavigationTouchListener);
    }

    private boolean initialValueIsSent = false;

    public void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        int width = right - left;
        int height = bottom - top;

        if (!initialValueIsSent) {
            if (periodChangedListener != null) {
                double frameStartInPercent = frameStart / width;
                double frameEndInPercent = (frameStart + frameWidth) / width;

                periodChangedListener.onPeriodChangedMoved(frameStartInPercent, frameEndInPercent);

                initialValueIsSent = true;
            }
        }

        chartDrawDelegate.onHeightChanged(height);
        updateHorizontalDrawingParams();
    }

    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int measuredHeight = MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.EXACTLY ? MeasureSpec.getSize(heightMeasureSpec) : prefferedHeight;
        int measuredWidth = MeasureSpec.getSize(widthMeasureSpec);

        measuredHeight = Math.max(measuredHeight, getSuggestedMinimumHeight());
        measuredWidth = Math.max(measuredWidth, getSuggestedMinimumWidth());

        setMeasuredDimension(measuredWidth, measuredHeight);
    }

    //todo: continue moving code from navigation view to its background delegate
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        drawBackground(canvas);
        chartDrawDelegate.drawChart(canvas);
        drawShadow(canvas);
    }

    private void drawShadow(Canvas canvas) {
        canvas.drawRect(0, 0, (int) frameStart - 1, getHeight(), duff);
        canvas.drawRect((int) (frameStart + frameWidth), 0, getWidth(), getHeight(), duff);
    }

//    private Paint activeBackgroundPaint = new Paint();
//    private Paint framePaint = new Paint();
    private Paint duff = new Paint();
    private Paint chartPaintActive = new Paint();

//    private int activeBackgroundColor = Color.WHITE;
//    private int activeBackgroundColorNightMode = Color.rgb(29, 39, 51);
//    private int frameColor = Color.rgb(219, 231, 240);
//    private int frameColorNightMode = Color.rgb(43, 66, 86);
    private int passiveBackgroundColor = Color.argb(0xa0, 245, 248, 249);
    private int passiveBackgroundColorNightMode = Color.argb(0xa0, 25, 33, 46);

    private void initVariableForDrawing() {
//        activeBackgroundPaint.setColor(activeBackgroundColor);
//        activeBackgroundPaint.setStyle(Paint.Style.FILL_AND_STROKE);
//
//        framePaint.setColor(frameColor);
//        framePaint.setStyle(Paint.Style.FILL_AND_STROKE);

        chartPaintActive.setStyle(Paint.Style.STROKE);
        chartPaintActive.setStrokeWidth(lineWidth);

        duff.setStyle(Paint.Style.FILL_AND_STROKE);
        duff.setColor(passiveBackgroundColor);
        duff.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.OVERLAY));

        chartDrawDelegate = new ChartDrawDelegate(
                lineWidth,
                bottomChartPadding,
                topChartPadding,
                new RedrawCallback() {
                    @Override
                    public void onRedrawRequired() {
                        invalidate();
                    }
                },
                null
        );
    }

    private void updateHorizontalDrawingParams() {
        if (chart != null && getWidth() > 0 && getHeight() > 0) {
            int pointsCount = chart.getAbscissa().size() - 1;
            float xStep = getWidth() / pointsCount;

            chartDrawDelegate.onDrawingParamsChanged(
                    0,
                    0,
                    xStep,
                    pointsCount
            );
        }
    }

    private void updateVerticalDrawingParams() {
        chartDrawDelegate.updateVerticalDrawingParams(0, 1);
    }

    private void drawBackground(Canvas canvas) {
        // draw active background
        canvas.drawRect(0, 0, getWidth(), getHeight(), activeBackgroundPaint);

        // draw frame
        canvas.drawRect(frameStart, 0, frameStart + frameWidth - 1, frameVerticalBorderWidth, framePaint);
        canvas.drawRect(frameStart, getHeight() - frameVerticalBorderWidth, frameStart + frameWidth - 1, getHeight(), framePaint);
        canvas.drawRect(frameStart, 0, frameStart + frameHorizontalBorderWidth, getHeight(), framePaint);
        canvas.drawRect(frameStart + frameWidth - 1 - frameHorizontalBorderWidth, 0, frameStart + frameWidth - 1, getHeight(), framePaint);

//        drawShadow(canvas);
    }

    public void setChart(Chart chart) {
        this.chart = chart;

        chartDrawDelegate.onChartInited(chart.getLineIds().size(), chart.getColors(), chart.getOrdinates());

        updateVerticalDrawingParams();
        updateHorizontalDrawingParams();

        invalidate();
    }

    public void setLineVisibility(String lineId, boolean visible) {
        final int lineIndex = chart.getLineIds().indexOf(lineId);

        if (lineIndex != -1 && chartDrawDelegate.isLineVisible(lineIndex) != visible) {
            chartDrawDelegate.setLineVisibility(lineIndex, visible);
            updateVerticalDrawingParams();
        }
    }

    public void setPeriodChangedListener(PeriodChangedListener listener) {
        periodChangedListener = listener;
        chartNavigationTouchListener.setPeriodChangedListener(listener);
    }

    public void onNightModeChanged(boolean nightModeOn) {
        activeBackgroundPaint.setColor(nightModeOn ? activeBackgroundColorNightMode : activeBackgroundColor);
        duff.setColor(nightModeOn ? passiveBackgroundColorNightMode : passiveBackgroundColor);
        framePaint.setColor(nightModeOn ? frameColorNightMode : frameColor);

        invalidate();
    }

}
