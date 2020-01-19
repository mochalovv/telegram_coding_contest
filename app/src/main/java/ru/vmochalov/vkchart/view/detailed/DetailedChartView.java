package ru.vmochalov.vkchart.view.detailed;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import ru.vmochalov.vkchart.R;
import ru.vmochalov.vkchart.chart.Chart;

/**
 * Created by Vladimir Mochalov on 10.03.2019.
 */
public class DetailedChartView extends View {

    private final int TOP_AXIS_PAINT_PX = 40;
    private final int BOTTOM_AXIS_MARGIN_PX = TOP_AXIS_PAINT_PX + 20;

    private float height;
    private float width;

    private Chart chart;

    private boolean[] linesVisibility;

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
        initViewWideProperties();
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
                TOP_AXIS_PAINT_PX
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

        chartDrawDelegate = new ChartDrawDelegate(lineStrokeWidth, BOTTOM_AXIS_MARGIN_PX);
    }

    private Paint selectedPointsPaint = new Paint();

    private double startPercent;
    private double endPercent;

    private double visibleWidth;
    private int firstDateIndex;
    private int lastDateIndex;
    private float xStep;
    private float enlargedWidth;
    private float x0;
    private float yStep;
    private List<Date> abscissa;

    private int firstVisiblePointIndex;
    private int lastVisiblePointIndex;

    private void initViewWideProperties() {
        startPercent = 0;
        endPercent = 1;

        selectedPointsPaint.setStrokeWidth(lineStrokeWidth);
        selectedPointsPaint.setStyle(Paint.Style.STROKE);
        selectedPointsPaint.setAntiAlias(true);

        verticalAxisValueAnimator = ValueAnimator.ofFloat(1.0f, 0.0f);
        verticalAxisValueAnimator.setDuration(ANIMATION_DURATION);
        verticalAxisValueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                axisAnimationFraction = (float) animation.getAnimatedValue();
                invalidate();
            }
        });
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

                    lastSelectedPointIndex = getNearestPointIndex(event.getX());

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
                    lastSelectedPointIndex = getNearestPointIndex(event.getX());
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

    private int getNearestPointIndex(float x) {
        int result = Math.round((x - x0) / xStep);
        if (result < firstDateIndex) result = firstDateIndex;
        if (result > lastDateIndex) result = lastDateIndex;
        return result;
    }

    private void initVariablesForHorizontalChartDrawing() {
        visibleWidth = width * (endPercent - startPercent);
        abscissa = chart.getAbscissa();

        firstDateIndex = 0;
        lastDateIndex = abscissa.size() - 1;

        xStep = width / lastDateIndex;
        xStep *= (width / visibleWidth);

        enlargedWidth = (float) (width * width / visibleWidth);

        x0 = (float) (-enlargedWidth * startPercent);

        firstVisiblePointIndex = (int) Math.floor(-x0 / xStep);

        if (firstVisiblePointIndex < firstDateIndex) {
            firstVisiblePointIndex = firstDateIndex;
        }

        lastVisiblePointIndex = (int) Math.ceil((width - x0) / xStep);

        if (lastVisiblePointIndex > lastDateIndex) {
            lastVisiblePointIndex = lastDateIndex;
        }

        horizontalLabelsDrawDelegate.onDrawingParamsChanged(lastDateIndex, x0, xStep, firstVisiblePointIndex, lastVisiblePointIndex);
        chartDrawDelegate.onDrawingParamsChanged(x0, firstVisiblePointIndex, xStep, yStep, lastVisiblePointIndex);
    }

    private float axisAnimationFraction;
    private boolean axisAnimationDirectionAppearFromBottom;

    private ValueAnimator verticalAxisValueAnimator;

    private void animateVerticalAxis(boolean maxVisibleValueDecreased) {
        if (verticalAxisValueAnimator != null) {
            verticalAxisValueAnimator.pause();
        }

        axisAnimationDirectionAppearFromBottom = maxVisibleValueDecreased;
        verticalAxisValueAnimator.start();
    }

    private void updateVerticalLinesDrawingParams(int maxVisibleValue) {
        yStep = (height - BOTTOM_AXIS_MARGIN_PX - TOP_AXIS_PAINT_PX) / maxVisibleValue;
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
            updateVerticalLinesDrawingParams(newMaxVisibleValue);
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
                    int value = (int) animation.getAnimatedValue();

                    updateVerticalLinesDrawingParams(value);

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

            animateVerticalAxis(newMaxVisibleValue < oldFixedMaxVisibleValue);
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

        verticalAxisDrawDelegate.drawVerticalAxis(canvas, axisAnimationFraction, axisAnimationDirectionAppearFromBottom);

        chartDrawDelegate.drawChart(canvas);

        drawSelectedPoints(canvas);
        verticalAxisDrawDelegate.drawVerticalLabels(canvas, axisAnimationFraction, axisAnimationDirectionAppearFromBottom);
        horizontalLabelsDrawDelegate.drawHorizontalLabels(canvas);
    }

    private void drawSelectedPoints(Canvas canvas) {
        if (lastSelectedPointIndex < 0) return;

        int pointValue;

        float nextX = x0 + xStep * lastSelectedPointIndex;
        float nextY;
        int tempColor;

        canvas.drawLine(
                nextX,
                0,
                nextX,
                height - BOTTOM_AXIS_MARGIN_PX,
                verticalAxisDrawDelegate.getVerticalAxisPaint()
        );

        for (int i = 0; i < chart.getLineIds().size(); i++) {
            if (linesVisibility[i]) {

                tempColor = chart.getColors().get(i);

                int color = Color.argb(
                        chartDrawDelegate.getLineAlpha(i),
                        Color.red(tempColor),
                        Color.green(tempColor),
                        Color.blue(tempColor)
                );

                selectedPointsPaint.setColor(color);

                pointValue = chart.getOrdinates().get(i).get(lastSelectedPointIndex);
                nextY = height - BOTTOM_AXIS_MARGIN_PX - pointValue * yStep;

                canvas.drawCircle(nextX, nextY, 10, backgroundDrawDelegate.backgroundPaint);
                canvas.drawCircle(nextX, nextY, 10, selectedPointsPaint);
            }
        }
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

    private List<List<Integer>> visiblePointValues = new ArrayList<>();

    private int getMaxVisibleValue() {

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

    private int getMaxValue(List<List<Integer>> lists) {
        int max = Integer.MIN_VALUE;

        for (List<Integer> list : lists) {
            max = Math.max(max, Collections.max(list));
        }

        return max;
    }

    public void setNightMode(boolean nightModeOn) {
        backgroundDrawDelegate.onNightModeChanged(nightModeOn);
        verticalAxisDrawDelegate.onNightModeChanged(nightModeOn);
        horizontalLabelsDrawDelegate.onNightModeChanged(nightModeOn);

        invalidate();
    }

}
