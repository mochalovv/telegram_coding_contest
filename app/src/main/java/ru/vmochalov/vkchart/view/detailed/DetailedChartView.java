package ru.vmochalov.vkchart.view.detailed;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

import ru.vmochalov.vkchart.R;
import ru.vmochalov.vkchart.chart.Chart;

/**
 * Created by Vladimir Mochalov on 10.03.2019.
 */
public class DetailedChartView extends View {

    private final int TOP_AXIS_MARGIN_PX = 40;
    private final int BOTTOM_AXIS_MARGIN_PX = 60;

    private Chart chart;

    private double startPercent = 0;
    private double endPercent = 1;

    // styleable attributes
    private int axisTextSize;
    private int lineStrokeWidth;
    private int axisStrokeWidth;

    private OnChartClickedListener onChartClickedListener;

    private BackgroundDrawDelegate backgroundDrawDelegate;
    private VerticalAxisDrawDelegate verticalAxisDrawDelegate;
    private HorizontalLabelsDrawDelegate horizontalLabelsDrawDelegate;
    private ChartDrawDelegate chartDrawDelegate;

    public interface OnChartClickedListener {
        void onTouch(float x, int pointIndex, List<Integer> values);

        void onMove(float x, int pointIndex, List<Integer> values);

        void onButtonUp();

        void onMovementDirectionChanged(boolean isHorizontal);
    }

    public DetailedChartView(Context context) {
        super(context);

        init();
    }

    public DetailedChartView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);

        handleAttributeSet(attributeSet);
        init();
    }

    public DetailedChartView(Context context, AttributeSet attributeSet, int defStyleAttr) {
        super(context, attributeSet, defStyleAttr);

        handleAttributeSet(attributeSet);
        init();
    }

    public DetailedChartView(Context context, AttributeSet attributeSet, int defStyleAttr, int defStyleRes) {
        super(context, attributeSet, defStyleAttr, defStyleRes);

        handleAttributeSet(attributeSet);
        init();
    }

    private void handleAttributeSet(AttributeSet attributeSet) {
        TypedArray attributes = getContext().obtainStyledAttributes(attributeSet, R.styleable.DetailedChartView);
        Resources resources = getContext().getResources();

        axisTextSize = attributes.getDimensionPixelSize(
                R.styleable.DetailedChartView_axisTextSize,
                resources.getDimensionPixelSize(R.dimen.chartViewDefaultTextSize)
        );

        lineStrokeWidth = attributes.getDimensionPixelSize(
                R.styleable.DetailedChartView_chartLineWidth,
                resources.getDimensionPixelSize(R.dimen.chartViewDefaultLineStrokeWidth)
        );

        axisStrokeWidth = attributes.getDimensionPixelSize(
                R.styleable.DetailedChartView_axisLineWidth,
                resources.getDimensionPixelSize(R.dimen.chartViewDefaultAxisStrokeWidth)
        );

        attributes.recycle();
    }

    private void init() {
        initTouchListener();
        initDelegates();
    }

    private void initDelegates() {
        backgroundDrawDelegate = new BackgroundDrawDelegate(getResources());
        verticalAxisDrawDelegate = new VerticalAxisDrawDelegate(
                getResources(),
                axisStrokeWidth,
                axisTextSize,
                BOTTOM_AXIS_MARGIN_PX,
                TOP_AXIS_MARGIN_PX,
                new RedrawCallback() {
                    @Override
                    public void onRedrawRequired() {
                        invalidate();
                    }
                }
        );

        horizontalLabelsDrawDelegate = new HorizontalLabelsDrawDelegate(
                getResources(),
                axisTextSize,
                axisStrokeWidth,
                new RedrawCallback() {
                    @Override
                    public void onRedrawRequired() {
                        invalidate();
                    }
                }
        );

        chartDrawDelegate = new ChartDrawDelegate(
                lineStrokeWidth,
                BOTTOM_AXIS_MARGIN_PX,
                TOP_AXIS_MARGIN_PX,
                new RedrawCallback() {
                    @Override
                    public void onRedrawRequired() {
                        invalidate();
                    }
                },
                new ChartDrawDelegate.MaxVisibleValueListener() {
                    @Override
                    public void onMaxVisibleValueChanged(int previousMaxValue, int newMaxValue) {
                        verticalAxisDrawDelegate.onLinesVisibilityUpdated(
                                chartDrawDelegate.areLinesVisible(),
                                newMaxValue
                        );

                        verticalAxisDrawDelegate.animateVerticalAxis(
                                previousMaxValue != 0 && newMaxValue < previousMaxValue
                        );
                    }
                }
        );
    }

    public void setOnChartClickedListener(OnChartClickedListener listener) {
        this.onChartClickedListener = listener;
    }

    public void initTouchListener() {
        setOnTouchListener(new OnTouchListener() {

            private float initialX;
            private float initialY;

            private boolean isHorizontalMovement;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    initialX = event.getX();
                    initialY = event.getY();

                    lastSelectedPointIndex = horizontalLabelsDrawDelegate.getClosestPointIndex(event.getX());

                    collectVisibleSelectedValues(lastSelectedPointIndex);

                    if (onChartClickedListener != null) {
                        onChartClickedListener.onTouch(event.getX(), lastSelectedPointIndex, visibleSelectedValues);
                    }
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    lastSelectedPointIndex = -1;
                    isHorizontalMovement = false;
                    if (onChartClickedListener != null) {
                        onChartClickedListener.onButtonUp();
                        onChartClickedListener.onMovementDirectionChanged(isHorizontalMovement);
                    }

                } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
                    lastSelectedPointIndex = horizontalLabelsDrawDelegate.getClosestPointIndex(event.getX());
                    collectVisibleSelectedValues(lastSelectedPointIndex);

                    boolean isHorizontal = isHorizontalMovement(event.getX(), event.getY());
                    if (isHorizontal != isHorizontalMovement) {
                        isHorizontalMovement = isHorizontal;

                        if (onChartClickedListener != null) {
                            onChartClickedListener.onMovementDirectionChanged(isHorizontalMovement);
                        }

                    }

                    if (onChartClickedListener != null) {
                        onChartClickedListener.onTouch(event.getX(), lastSelectedPointIndex, visibleSelectedValues);
                    }
                } else {
                    isHorizontalMovement = false;
                    lastSelectedPointIndex = -1;

                    if (onChartClickedListener != null) {
                        onChartClickedListener.onMovementDirectionChanged(isHorizontalMovement);
                    }
                }

                invalidate();

                return true;
            }

            private boolean isHorizontalMovement(float updatedX, float updatedY) {
                if (initialX == updatedX) return false;

                double tg = (updatedY - initialY) / (updatedX - initialX);

                return Math.abs(tg) < 1;
            }

        });
    }

    private List<Integer> visibleSelectedValues = new ArrayList<>();
    private int lastSelectedPointIndex = -1;

    private List<Integer> collectVisibleSelectedValues(int index) {
        visibleSelectedValues.clear();

        for (int i = 0; i < chart.getLineIds().size(); i++) {
            if (chartDrawDelegate.isLineVisible(i)) {
                visibleSelectedValues.add(chart.getOrdinates().get(i).get(index));
            }
        }

        return visibleSelectedValues;
    }

    private void initVariablesForHorizontalChartDrawing(float width) {
        double visibleWidth = width * (endPercent - startPercent);

        int firstDateIndex = 0;
        int lastDateIndex = chart.getAbscissa().size() - 1;

        float xStep = width / lastDateIndex;
        xStep *= (width / visibleWidth);

        float enlargedWidth = (float) (width * width / visibleWidth);

        float x0 = (float) (-enlargedWidth * startPercent);

        int firstVisiblePointIndex = (int) Math.floor(-x0 / xStep);

        if (firstVisiblePointIndex < firstDateIndex) {
            firstVisiblePointIndex = firstDateIndex;
        }

        int lastVisiblePointIndex = (int) Math.ceil((width - x0) / xStep);

        if (lastVisiblePointIndex > lastDateIndex) {
            lastVisiblePointIndex = lastDateIndex;
        }

        horizontalLabelsDrawDelegate.onDrawingParamsChanged(lastDateIndex, x0, xStep, firstVisiblePointIndex, lastVisiblePointIndex);
        chartDrawDelegate.onDrawingParamsChanged(x0, firstVisiblePointIndex, xStep, lastVisiblePointIndex);
    }

    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int measuredWidth = MeasureSpec.getSize(widthMeasureSpec);
        measuredWidth = Math.max(measuredWidth, getSuggestedMinimumWidth());

        int measuredHeight = MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.EXACTLY ? MeasureSpec.getSize(heightMeasureSpec) : (int) (measuredWidth * 0.85);
        measuredHeight = Math.max(measuredHeight, getSuggestedMinimumHeight());

        setMeasuredDimension(measuredWidth, measuredHeight);
    }

    public void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        float height = bottom - top;
        float width = right - left;

        backgroundDrawDelegate.setCanvasSize(width, height);
        verticalAxisDrawDelegate.setCanvasSize(width, height);

        onHeightChanged(height);

        updateDrawingParams();
    }

    private void onHeightChanged(float height) {
        horizontalLabelsDrawDelegate.onHeightChanged(height);
        verticalAxisDrawDelegate.onHeightChanged(height);
        chartDrawDelegate.onHeightChanged(height);
    }

    private void updateDrawingParams() {
        if (chart != null && getWidth() > 0 && getHeight() > 0) {
            chartDrawDelegate.updateVerticalDrawingParams(startPercent, endPercent);
            initVariablesForHorizontalChartDrawing(getWidth());
            horizontalLabelsDrawDelegate.updatedHorizontalLabelsScale();
        }
    }

    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        backgroundDrawDelegate.drawBackground(canvas);
        verticalAxisDrawDelegate.drawVerticalAxis(canvas);
        chartDrawDelegate.drawChart(canvas);
        chartDrawDelegate.drawSelectedPoints(
                canvas,
                verticalAxisDrawDelegate.getVerticalAxisPaint(),
                backgroundDrawDelegate.getBackgroundPaint(),
                lastSelectedPointIndex
        );
        verticalAxisDrawDelegate.drawVerticalLabels(canvas);
        horizontalLabelsDrawDelegate.drawHorizontalLabels(canvas);
    }

    public void onVisibleRangeMoved(double startVisiblePercent, double endVisiblePercent) {
        startPercent = startVisiblePercent;
        endPercent = endVisiblePercent;

        updateDrawingParams();

        invalidate();
    }

    public void onVisibleRangeScaleChanged(double startVisiblePercent, double endVisiblePercent, boolean startIsStable) {
        startPercent = startVisiblePercent;
        endPercent = endVisiblePercent;

        updateDrawingParams();

        invalidate();
    }

    public void setChart(Chart chart) {
        this.chart = chart;

        horizontalLabelsDrawDelegate.onChartInited(chart.getAbscissaAsString());
        chartDrawDelegate.onChartInited(chart.getLineIds().size(), chart.getColors(), chart.getOrdinates());

        updateDrawingParams();

        invalidate();
    }

    public void setLineVisibility(String lineId, boolean visible) {
        final int lineIndex = chart.getLineIds().indexOf(lineId);

        if (lineIndex != -1 && chartDrawDelegate.isLineVisible(lineIndex) != visible) {
            chartDrawDelegate.setLineVisibility(lineIndex, visible);
            chartDrawDelegate.updateVerticalDrawingParams(startPercent, endPercent);
        }
    }

    public void setNightMode(boolean nightModeOn) {
        backgroundDrawDelegate.onNightModeChanged(nightModeOn);
        verticalAxisDrawDelegate.onNightModeChanged(nightModeOn);
        horizontalLabelsDrawDelegate.onNightModeChanged(nightModeOn);

        invalidate();
    }

}
