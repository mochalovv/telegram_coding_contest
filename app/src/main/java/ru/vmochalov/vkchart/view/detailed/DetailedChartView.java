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
    private final int AXES_TEXT_SIZE_PX = 20;

    private float height;
    private float width;

    private Chart chart;

    private boolean[] linesVisibility;
    private int[] linesAlphas; // 0 - 255

    private boolean[] labelsVisibility;

    private int ANIMATION_DURATION = 500; //ms
    private int ALPHA_ANIMATION_DURATION = 300;

    // styleable attributes
    private int axisTextSize;
    private int lineStrokeWidth;
    private int axisStrokeWidth;

    private List<Integer> fadePointIndexes = new ArrayList<>();

    private OnChartClickedListener onChartClickedListener;

    private BackgroundDrawDelegate backgroundDrawDelegate;
    private VerticalAxisDrawDelegate verticalAxisDrawDelegate;

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
    }

    private Paint chartPaint = new Paint();
    private Paint debugPaint = new Paint();
    private Paint labelPaint = new Paint();
    private Paint labelPaintAnimation = new Paint();

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

    private int linesCount;

    private int firstVisiblePointIndex;
    private int lastVisiblePointIndex;

    private float horizontalLabelY;

    private void initViewWideProperties() {
        labelPaint.setTextAlign(Paint.Align.CENTER);
        labelPaint.setColor(getResources().getColor(R.color.lightThemeLabelText));
        labelPaint.setTextSize(axisTextSize);
        labelPaint.setStrokeWidth(axisStrokeWidth);
        labelPaint.setAntiAlias(true);
        labelPaint.setStyle(Paint.Style.FILL);

        labelPaintAnimation.setTextAlign(Paint.Align.CENTER);
        labelPaintAnimation.setColor(getResources().getColor(R.color.lightThemeLabelText));
        labelPaintAnimation.setTextSize(axisTextSize);
        labelPaintAnimation.setStrokeWidth(axisStrokeWidth);
        labelPaintAnimation.setAntiAlias(true);
        labelPaintAnimation.setStyle(Paint.Style.FILL);

        startPercent = 0;
        endPercent = 1;

        chartPaint.setStrokeWidth(lineStrokeWidth);
        chartPaint.setStyle(Paint.Style.STROKE);
        chartPaint.setAntiAlias(true);

        debugPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        debugPaint.setColor(Color.LTGRAY);

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

        chartPoints = new float[(lastVisiblePointIndex - firstVisiblePointIndex + 1) * 4];
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
        horizontalLabelY = height - AXES_TEXT_SIZE_PX / 2;

        verticalAxisDrawDelegate.onHeightChanged(height);
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
            updatedHorizontalLabelsScale();
        }
    }

    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        backgroundDrawDelegate.drawBackground(canvas);

        verticalAxisDrawDelegate.drawVerticalAxis(canvas, axisAnimationFraction, axisAnimationDirectionAppearFromBottom);

        drawChart(canvas);

        drawSelectedPoints(canvas);
        verticalAxisDrawDelegate.drawVerticalLabels(canvas, axisAnimationFraction, axisAnimationDirectionAppearFromBottom);
        drawHorizontalLabels(canvas);
    }

    private void drawSelectedPoints(Canvas canvas) {
        if (lastSelectedPointIndex < 0) return;

        nextX = x0 + xStep * lastSelectedPointIndex;

        canvas.drawLine(
                nextX,
                0,
                nextX,
                height - BOTTOM_AXIS_MARGIN_PX,
                verticalAxisDrawDelegate.getVerticalAxisPaint()
        );

        for (int i = 0; i < linesCount; i++) {
            if (linesVisibility[i]) {

                tempColor = chart.getColors().get(i);

                int color = Color.argb(
                        linesAlphas[i],
                        Color.red(tempColor),
                        Color.green(tempColor),
                        Color.blue(tempColor)
                );

                chartPaint.setColor(color);

                pointValue = chart.getOrdinates().get(i).get(lastSelectedPointIndex);
                nextY = height - BOTTOM_AXIS_MARGIN_PX - pointValue * yStep;

                canvas.drawCircle(nextX, nextY, 10, backgroundDrawDelegate.backgroundPaint);
                canvas.drawCircle(nextX, nextY, 10, chartPaint);
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

        updatedHorizontalLabelsScale();

        invalidate();
    }

    private int getPowOfTwo(int pow) {
        int result = 1;

        for (int i = 0; i < pow; i++) {
            result *= 2;
        }

        return result;
    }

    private int currentLabelsScale;

    private void setScaleForHorizontalLabels(int scale) { // 0 - all are visible, 1 - every second, 3 - every 4th, 4 - every 8th and so on
        fadePointIndexes.clear();

        int indexToMakeVisible = getPowOfTwo(scale);
        boolean newVisibility;

        for (int i = 0; i < labelsVisibility.length; i++) {
            newVisibility = (i % indexToMakeVisible == 0);
            if (labelsVisibility[i] != newVisibility) {
                labelsVisibility[i] = newVisibility;
                fadePointIndexes.add(i);
            }
        }
    }

    private int getInitialScale() {
        //find out, do two points overlaps. If no, then this is preffered scale.
        int scale = 0;

        int firstPointIndex = 0;
        int lastPointIndex = 0;

        while (lastPointIndex < labelsVisibility.length && areNeightborPointsTooClose(firstPointIndex, lastPointIndex)) {
            scale++;
            lastPointIndex = getPowOfTwo(scale);
        }

        while (lastPointIndex < labelsVisibility.length && areNeighbourPointsTooFar(firstPointIndex, lastPointIndex)) {
            scale--;
            lastPointIndex = getPowOfTwo(scale);
        }

        return scale;
    }

    private void updatedHorizontalLabelsScale() {
        int newScale = getInitialScale();

        if (newScale != currentLabelsScale) {
            if (newScale < currentLabelsScale) {
                animateHorizontalLabels(true);
            } else {
                animateHorizontalLabels(false);
            }

            currentLabelsScale = newScale;

            setScaleForHorizontalLabels(newScale);
        }
    }

    private boolean areNeightborPointsTooClose(int firstPointIndex, int secondPointIndex) {
        if (isPointIndexValid(firstPointIndex) && isPointIndexValid(secondPointIndex)) {

            String labelOne = chart.getAbscissaAsString().get(firstPointIndex);
            float labelOneWidth = labelPaint.measureText(labelOne);
            float labelOneStartX = x0 + xStep * firstPointIndex - labelOneWidth / 2;
            float labelOneEndX = labelOneStartX + labelOneWidth;

            String labelTwo = chart.getAbscissaAsString().get(secondPointIndex);
            float labelTwoWidth = labelPaint.measureText(labelTwo);
            float labelTwoStartX = x0 + xStep * secondPointIndex - labelTwoWidth / 2;

            return labelOneEndX - labelTwoStartX > -30;

        } else {
            return false;
        }
    }

    private int TOO_FAR_CONSTANT = 200;

    private boolean areNeighbourPointsTooFar(int firstPointIndex, int secondPointIndex) {

        if (isPointIndexValid(firstPointIndex) && isPointIndexValid(secondPointIndex)) {

            String labelOne = chart.getAbscissaAsString().get(firstPointIndex);
            float labelOneWidth = labelPaint.measureText(labelOne);
            float labelOneStartX = x0 + xStep * firstPointIndex - labelOneWidth / 2;
            float labelOneEndX = labelOneStartX + labelOneWidth;

            String labelTwo = chart.getAbscissaAsString().get(secondPointIndex);
            float labelTwoWidth = labelPaint.measureText(labelTwo);
            float labelTwoStartX = x0 + xStep * secondPointIndex - labelTwoWidth / 2;

            return labelTwoStartX - labelOneEndX > TOO_FAR_CONSTANT;

        } else {
            return false;
        }
    }

    private boolean isPointIndexValid(int index) {
        return index >= firstDateIndex && index <= lastDateIndex;
    }

    private ValueAnimator horizontalLabelsAnimator = null;
    private float horizontalLabelsAlpha = 1;

    private boolean labelsAlphaAnimationInProgress = false;

    private void animateHorizontalLabels(boolean appear) {
        if (horizontalLabelsAnimator != null) {
            horizontalLabelsAnimator.end();
        }

        horizontalLabelsAnimator = ValueAnimator.ofFloat(appear ? 0.0f : 1.0f, appear ? 1.0f : 0.0f);

        horizontalLabelsAnimator.setDuration(200);
        horizontalLabelsAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                horizontalLabelsAlpha = (float) animation.getAnimatedValue();
                invalidate();
            }
        });
        horizontalLabelsAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                labelsAlphaAnimationInProgress = false;
            }

            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                labelsAlphaAnimationInProgress = true;
            }
        });
        horizontalLabelsAnimator.start();
    }

    private void drawHorizontalLabels(Canvas canvas) {

        boolean animationInProgress = labelsAlphaAnimationInProgress;

        if (animationInProgress) {
            int color = Color.argb(
                    (int) (0xff * horizontalLabelsAlpha),
                    Color.red(labelPaint.getColor()),
                    Color.green(labelPaint.getColor()),
                    Color.blue(labelPaint.getColor())
            );

            labelPaintAnimation.setColor(color);

            for (int i : fadePointIndexes) {

                if (isPointIndexValid(i) && i != firstDateIndex && i != lastDateIndex) {
                    float x = x0 + xStep * i;

                    canvas.drawText(
                            chart.getAbscissaAsString().get(i),
                            x,
                            horizontalLabelY,
                            labelPaintAnimation
                    );

                }

            }
        }

        for (int i = firstVisiblePointIndex; i <= lastVisiblePointIndex; i++) {
            if (labelsVisibility[i] && i != firstDateIndex && i != lastDateIndex) {
                if (!animationInProgress || !fadePointIndexes.contains(i)) {
                    canvas.drawText(
                            chart.getAbscissaAsString().get(i),
                            x0 + xStep * i,
                            horizontalLabelY,
                            labelPaint
                    );
                }
            }
        }
    }

    private float previousX;
    private float previousY;
    private float nextX;
    private int pointValue;
    private float nextY;
    private float[] chartPoints;
    private int chartPointsIndex;
    private List<Integer> chartOrdinate;

    private int tempColor;

    private void drawChart(Canvas canvas) {
        for (int i = 0; i < linesCount; i++) {
            if (linesAlphas[i] == 0) {
                continue; // skip muted charts
            }

            chartPointsIndex = 0;

            tempColor = chart.getColors().get(i);

            int color = Color.argb(
                    linesAlphas[i],
                    Color.red(tempColor),
                    Color.green(tempColor),
                    Color.blue(tempColor)
            );

            chartPaint.setColor(color);
            chartOrdinate = chart.getOrdinates().get(i);

            previousX = x0 + firstVisiblePointIndex * xStep;
            pointValue = chartOrdinate.get(firstVisiblePointIndex);
            previousY = height - BOTTOM_AXIS_MARGIN_PX - pointValue * yStep;

            for (int j = firstVisiblePointIndex + 1; j < lastVisiblePointIndex; j++) {
                nextX = x0 + j * xStep;
                pointValue = chartOrdinate.get(j);
                nextY = height - BOTTOM_AXIS_MARGIN_PX - pointValue * yStep;

                chartPoints[chartPointsIndex++] = previousX;
                chartPoints[chartPointsIndex++] = previousY;
                chartPoints[chartPointsIndex++] = nextX;
                chartPoints[chartPointsIndex++] = nextY;

                previousX = nextX;
                previousY = nextY;
            }

            nextX = x0 + lastVisiblePointIndex * xStep;
            pointValue = chartOrdinate.get(lastVisiblePointIndex);
            nextY = height - BOTTOM_AXIS_MARGIN_PX - pointValue * yStep;

            chartPoints[chartPointsIndex++] = previousX;
            chartPoints[chartPointsIndex++] = previousY;
            chartPoints[chartPointsIndex++] = nextX;
            chartPoints[chartPointsIndex++] = nextY;

            canvas.drawLines(chartPoints, chartPaint);
        }
    }

    public void setChart(Chart chart) {
        this.chart = chart;

        this.labelsVisibility = new boolean[chart.getAbscissa().size()];
        Arrays.fill(labelsVisibility, false);

        this.linesVisibility = new boolean[chart.getLabels().size()];
        Arrays.fill(linesVisibility, true);

        linesAlphas = new int[chart.getLabels().size()];
        Arrays.fill(linesAlphas, 0xff);

        linesCount = chart.getLineIds().size();

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
                        int value = (int) animation.getAnimatedValue();

                        linesAlphas[lineIndex] = value;
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
        int labelsColor = getResources().getColor(nightModeOn ? R.color.darkThemeLabelText : R.color.lightThemeLabelText);

        labelPaint.setColor(labelsColor);

        backgroundDrawDelegate.onNightModeChanged(nightModeOn);
        verticalAxisDrawDelegate.onNightModeChanged(nightModeOn);

        invalidate();
    }

}
