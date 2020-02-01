package ru.vmochalov.vkchart.chart.view.secondary;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;

import ru.vmochalov.vkchart.chart.data.Chart;
import ru.vmochalov.vkchart.chart.view.common.delegates.ChartDrawDelegate;
import ru.vmochalov.vkchart.chart.view.common.PeriodChangedListener;
import ru.vmochalov.vkchart.chart.view.common.RedrawCallback;
import ru.vmochalov.vkchart.chart.view.secondary.delegates.FrameDrawDelegate;

public class SecondaryChartView extends View {

    private static final float INITIAL_FRAME_START_POSITION_PX = 0;
    private static final float INITIAL_FRAME_WIDTH_PX = 300;
    private static final float LINE_STROKE_WIDTH_PX = 2;
    private static final int PREFERRED_HEIGHT_PX = 75;
    private static final float VERTICAL_CHART_PADDING_PX = 3;

    private Chart chart;

    private PeriodChangedListener periodChangedListener;

    private ChartDrawDelegate chartDrawDelegate;
    private FrameDrawDelegate frameDrawDelegate;

    public interface FrameUpdatedListener {
        void onFrameUpdated(float start, float width);
    }

    private SecondaryChartOnTouchListener secondaryChartOnTouchListener = new SecondaryChartOnTouchListener(
            INITIAL_FRAME_START_POSITION_PX,
            INITIAL_FRAME_WIDTH_PX,
            new FrameUpdatedListener() {
                @Override
                public void onFrameUpdated(float start, float width) {
                    frameDrawDelegate.onFrameUpdated(start, width);
                }
            }
    );

    public SecondaryChartView(Context context) {
        super(context);
        initTouchListener();

        initVariableForDrawing();
    }

    public SecondaryChartView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        initTouchListener();

        initVariableForDrawing();
    }

    public SecondaryChartView(Context context, AttributeSet attributeSet, int defStyleAttr) {
        super(context, attributeSet, defStyleAttr);
        initTouchListener();

        initVariableForDrawing();
    }

    private void initTouchListener() {
        setOnTouchListener(secondaryChartOnTouchListener);
    }

    private boolean initialValueIsSent = false;

    public void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        int width = right - left;
        int height = bottom - top;

        if (!initialValueIsSent) {
            if (periodChangedListener != null) {
                double frameStartInPercent = frameDrawDelegate.getFrameStart() / width;
                double frameEndInPercent = (frameDrawDelegate.getFrameEnd()) / width;

                periodChangedListener.onPeriodChangedMoved(frameStartInPercent, frameEndInPercent);

                initialValueIsSent = true;
            }
        }

        chartDrawDelegate.onHeightChanged(height);
        updateHorizontalDrawingParams();
    }

    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int measuredHeight = MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.EXACTLY ? MeasureSpec.getSize(heightMeasureSpec) : PREFERRED_HEIGHT_PX;
        int measuredWidth = MeasureSpec.getSize(widthMeasureSpec);

        measuredHeight = Math.max(measuredHeight, getSuggestedMinimumHeight());
        measuredWidth = Math.max(measuredWidth, getSuggestedMinimumWidth());

        setMeasuredDimension(measuredWidth, measuredHeight);
    }

    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        frameDrawDelegate.drawFrame(canvas);
        chartDrawDelegate.drawChart(canvas);
        frameDrawDelegate.drawShadow(canvas);
    }

    private void initVariableForDrawing() {
        chartDrawDelegate = new ChartDrawDelegate(
                LINE_STROKE_WIDTH_PX,
                VERTICAL_CHART_PADDING_PX,
                VERTICAL_CHART_PADDING_PX,
                new RedrawCallback() {
                    @Override
                    public void onRedrawRequired() {
                        invalidate();
                    }
                },
                null
        );

        frameDrawDelegate = new FrameDrawDelegate(
                INITIAL_FRAME_START_POSITION_PX,
                INITIAL_FRAME_WIDTH_PX
        );
    }

    private void updateHorizontalDrawingParams() {
        if (chart != null && getWidth() > 0 && getHeight() > 0) {
            int pointsCount = chart.getAbscissa().size() - 1;
            float xStep = (float) getWidth() / pointsCount;

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
        secondaryChartOnTouchListener.setPeriodChangedListener(listener);
    }

    public void onNightModeChanged(boolean nightModeOn) {
        frameDrawDelegate.onNightModeChanged(nightModeOn);

        invalidate();
    }

}
