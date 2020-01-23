package ru.vmochalov.vkchart.view.detailed;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ru.vmochalov.vkchart.R;
import ru.vmochalov.vkchart.chart.Chart;

import static ru.vmochalov.vkchart.utils.CalculationUtil.getMaxValue;

/**
 * Created by Vladimir Mochalov on 10.03.2019.
 */
public class DetailedChartView extends View {

    private final int TOP_AXIS_MARGIN_PX = 40;
    private final int BOTTOM_AXIS_MARGIN_PX = TOP_AXIS_MARGIN_PX + 20;

    private float height;
    private float width;

    private Chart chart;

    private boolean[] linesVisibility;

    private double startPercent = 0;
    private double endPercent = 1;

    private int ANIMATION_DURATION = 500; //ms
    private int ALPHA_ANIMATION_DURATION = 300;

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
                new VerticalAxisDrawDelegate.Callback() {
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
                new HorizontalLabelsDrawDelegate.Callback() {
                    @Override
                    public void onRedrawRequired() {
                        invalidate();
                    }
                }
        );

        chartDrawDelegate = new ChartDrawDelegate(
                lineStrokeWidth,
                BOTTOM_AXIS_MARGIN_PX,
                TOP_AXIS_MARGIN_PX
        );
    }

    private void initVariablesForChartDrawing() {
        initVariablesForHorizontalChartDrawing();
        initVariablesForVerticalChartDrawing();
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
            if (linesVisibility[i]) {
                visibleSelectedValues.add(chart.getOrdinates().get(i).get(index));
            }
        }

        return visibleSelectedValues;
    }

    private void initVariablesForHorizontalChartDrawing() {
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

    private boolean maxVisibleValueChangedOnStart;

    private ValueAnimator maxVisibleValueAnimator;

    private int oldFixedMaxVisibleValue;

    private void initVariablesForVerticalChartDrawing() {
        int newMaxVisibleValue = getMaxVisibleValue();

        if (newMaxVisibleValue == oldFixedMaxVisibleValue) {
            return;
        }

        if (!maxVisibleValueChangedOnStart) {
            maxVisibleValueChangedOnStart = true;

            verticalAxisDrawDelegate.onLinesVisibilityUpdated(areLinesVisible(), newMaxVisibleValue);
            chartDrawDelegate.onMaxVisibleValueChanged(newMaxVisibleValue);
        } else {

            verticalAxisDrawDelegate.onLinesVisibilityUpdated(areLinesVisible(), newMaxVisibleValue);

            if (maxVisibleValueAnimator != null) {
                maxVisibleValueAnimator.pause();
            }

            maxVisibleValueAnimator = ValueAnimator.ofInt(oldFixedMaxVisibleValue, newMaxVisibleValue);
            maxVisibleValueAnimator.setDuration(ANIMATION_DURATION);
            maxVisibleValueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    chartDrawDelegate.onMaxVisibleValueChanged((int) animation.getAnimatedValue());

                    DetailedChartView.this.invalidate();
                }
            });
            maxVisibleValueAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    verticalAxisDrawDelegate.onMaxVisibleValueAnimationEnd();
                }
            });

            maxVisibleValueAnimator.start();

            verticalAxisDrawDelegate.animateVerticalAxis(newMaxVisibleValue < oldFixedMaxVisibleValue);
        }

        oldFixedMaxVisibleValue = newMaxVisibleValue;
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

        height = bottom - top;
        width = right - left;

        backgroundDrawDelegate.setCanvasSize(width, height);
        verticalAxisDrawDelegate.setCanvasSize(width, height);

        onHeightChanged(height);

        onChartAndHeightReady();
        onChartAndWidthReady();
    }

    private void onHeightChanged(float height) {
        horizontalLabelsDrawDelegate.onHeightChanged(height);
        verticalAxisDrawDelegate.onHeightChanged(height);
        chartDrawDelegate.onHeightChanged(height);
    }

    private void onChartAndHeightReady() {
        if (height > 0 && chart != null) {
            initVariablesForVerticalChartDrawing();
        }
    }

    private void onChartAndWidthReady() {
        if (width > 0 && chart != null) {
            initVariablesForChartDrawing();
            initVariablesForHorizontalChartDrawing();
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
                backgroundDrawDelegate.backgroundPaint,
                lastSelectedPointIndex,
                linesVisibility
        );
        verticalAxisDrawDelegate.drawVerticalLabels(canvas);
        horizontalLabelsDrawDelegate.drawHorizontalLabels(canvas);
    }

    private boolean areLinesVisible() {
        for (boolean visibility : linesVisibility) {
            if (visibility) {
                return true;
            }
        }
        return false;
    }

    public void onVisibleRangeMoved(double startVisiblePercent, double endVisiblePercent) {
        startPercent = startVisiblePercent;
        endPercent = endVisiblePercent;

        initVariablesForChartDrawing();

        invalidate();
    }

    public void onVisibleRangeScaleChanged(double startVisiblePercent, double endVisiblePercent, boolean startIsStable) {
        startPercent = startVisiblePercent;
        endPercent = endVisiblePercent;

        initVariablesForChartDrawing();

        horizontalLabelsDrawDelegate.updatedHorizontalLabelsScale();

        invalidate();
    }

    public void setChart(Chart chart) {
        this.chart = chart;

        this.linesVisibility = new boolean[chart.getLabels().size()];
        Arrays.fill(linesVisibility, true);

        horizontalLabelsDrawDelegate.onChartInited(chart.getAbscissaAsString());
        chartDrawDelegate.onChartInited(chart.getLineIds().size(), chart.getColors(), chart.getOrdinates());

        onChartAndWidthReady();
        onChartAndHeightReady();

        invalidate();
    }

    private ValueAnimator linesAlphaAnimator;

    public void setLineVisibility(String lineId, boolean visible) {
        final int lineIndex = chart.getLineIds().indexOf(lineId);

        if (lineIndex != -1) {

            if (linesVisibility[lineIndex] != visible) {

                if (linesAlphaAnimator != null) {
                    linesAlphaAnimator.end();
                }
                linesAlphaAnimator = ValueAnimator.ofInt(visible ? 0 : 0xff, visible ? 0xff : 0);
                linesAlphaAnimator.setDuration(ALPHA_ANIMATION_DURATION);

                linesAlphaAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        chartDrawDelegate.setLineAlpha(lineIndex, (int) animation.getAnimatedValue());
                        DetailedChartView.this.invalidate();
                    }
                });
                linesAlphaAnimator.start();
            }
            linesVisibility[lineIndex] = visible;


            initVariablesForVerticalChartDrawing();
            invalidate();
        }
    }

    private int getMaxVisibleValue() {
        List<List<Integer>> visiblePointValues = new ArrayList<>();

        int absSize = chart.getAbscissa().size();

        int firstVisiblePointIndex = (int) (absSize * startPercent);
        int lastVisiblePointIndex = (int) Math.ceil(absSize * endPercent);

        if (firstVisiblePointIndex > 0 && firstVisiblePointIndex == absSize) {
            firstVisiblePointIndex = absSize - 1;
        }

        visiblePointValues.clear();

        for (int i = 0; i < chart.getLineIds().size(); i++) {
            if (linesVisibility[i]) {
                visiblePointValues.add(chart.getOrdinates().get(i).subList(firstVisiblePointIndex, lastVisiblePointIndex));
            }
        }

        return (visiblePointValues.isEmpty()) ? 0 : getMaxValue(visiblePointValues);
    }

    public void setNightMode(boolean nightModeOn) {
        backgroundDrawDelegate.onNightModeChanged(nightModeOn);
        verticalAxisDrawDelegate.onNightModeChanged(nightModeOn);
        horizontalLabelsDrawDelegate.onNightModeChanged(nightModeOn);

        invalidate();
    }

}
