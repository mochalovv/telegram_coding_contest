package ru.vmochalov.vkchart.view;

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
import ru.vmochalov.vkchart.dto.Chart;

/**
 * Created by Vladimir Mochalov on 10.03.2019.
 */
class DetailedChartView extends View {

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


    public interface OnChartClickedListener {
        void onTouch(float x, int pointIndex, List<Integer> values);

        void onMove(float x, int pointIndex, List<Integer> values);

        void onButtonUp();
    }

    public DetailedChartView(Context context) {
        super(context);

        initViewWideProperties();
        initTouchListener();
    }

    public DetailedChartView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);

        handleAttributeSet(attributeSet);
        initViewWideProperties();
        initTouchListener();
    }

    public DetailedChartView(Context context, AttributeSet attributeSet, int defStyleAttr) {
        super(context, attributeSet, defStyleAttr);

        handleAttributeSet(attributeSet);
        initViewWideProperties();
        initTouchListener();
    }

    public DetailedChartView(Context context, AttributeSet attributeSet, int defStyleAttr, int defStyleRes) {
        super(context, attributeSet, defStyleAttr, defStyleRes);

        handleAttributeSet(attributeSet);
        initViewWideProperties();
        initTouchListener();
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

    private Paint chartPaint = new Paint();
    private Paint backgroundPaint = new Paint();
    private Paint debugPaint = new Paint();
    private Paint labelPaint = new Paint();
    private Paint labelPaintAnimation = new Paint();
    private Paint verticalAxisPaint = new Paint();
    private Paint verticalAxisPaintAnimation = new Paint();
    private Paint verticalLabelsPaint = new Paint();
    private Paint verticalLabelsPaintAnimation = new Paint();

    private int bottomAxisMargin = 40 + 20;
    private int topAxisMargin = 40;
    private int levelsCount = 6;
    private int axesTextSize = 20;
    private int axesTextMargin = 12;

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

    private String[] verticalLevelValuesAsStrings = new String[levelsCount];
    private String[] oldVerticalLevelValuesAsStrings = new String[levelsCount];

    private float[] verticalAxesLinesCoords = new float[levelsCount * 4];

    private int linesCount;

    private int firstVisiblePointIndex;
    private int lastVisiblePointIndex;

    private float horizontalLabelY;

    //drawing level lines
    float yDelta;

    private void initViewWideProperties() {
        backgroundPaint.setColor(getResources().getColor(R.color.lightThemeChartBackground));
        backgroundPaint.setStyle(Paint.Style.FILL_AND_STROKE);

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

        verticalAxisPaint.setColor(getResources().getColor(R.color.lightThemeAxis));
        verticalAxisPaint.setStrokeWidth(axisStrokeWidth);
        verticalAxisPaint.setAntiAlias(true);
        verticalAxisPaint.setStyle(Paint.Style.FILL_AND_STROKE);

        verticalAxisPaintAnimation.setStrokeWidth(axisStrokeWidth);
        verticalAxisPaintAnimation.setAntiAlias(true);
        verticalAxisPaintAnimation.setStyle(Paint.Style.FILL_AND_STROKE);

        verticalLabelsPaint.setColor(getResources().getColor(R.color.lightThemeLabelText));
        verticalLabelsPaint.setTextSize(axisTextSize);
        verticalLabelsPaint.setStrokeWidth(axisStrokeWidth);
        verticalLabelsPaint.setTextAlign(Paint.Align.LEFT);
        verticalLabelsPaint.setAntiAlias(true);
        verticalLabelsPaint.setStyle(Paint.Style.FILL);

        verticalLabelsPaintAnimation.setTextSize(axisTextSize);
        verticalLabelsPaintAnimation.setStrokeWidth(axisStrokeWidth);
        verticalLabelsPaintAnimation.setTextAlign(Paint.Align.LEFT);
        verticalLabelsPaintAnimation.setAntiAlias(true);
        verticalLabelsPaintAnimation.setStyle(Paint.Style.FILL);
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

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    lastSelectedPointIndex = getNearestPointIndex(event.getX());

                    collectVisibleSelectedValues(lastSelectedPointIndex);

                    if (onChartClickedListener != null) {
                        onChartClickedListener.onTouch(event.getX(), lastSelectedPointIndex, visibleSelectedValues);
                    }
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    lastSelectedPointIndex = -1;

                    if (onChartClickedListener != null) {
                        onChartClickedListener.onButtonUp();
                    }

                } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
                    lastSelectedPointIndex = getNearestPointIndex(event.getX());
                    collectVisibleSelectedValues(lastSelectedPointIndex);

                    if (onChartClickedListener != null) {
                        onChartClickedListener.onTouch(event.getX(), lastSelectedPointIndex, visibleSelectedValues);
                    }
                }

                invalidate();

                return true;
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

    private void updateVerticalLabelsDrawingParams(int newMaxVisibleValue) {
        int verticalLevelDelta = (newMaxVisibleValue / levelsCount);

        if (!areLinesVisible() || verticalLevelDelta == 0)
            verticalLevelDelta = 1; // in case user is confused

        if (oldVerticalLevelValuesAsStrings[0] == null) {
            for (int i = 0; i < levelsCount; i++) {
                oldVerticalLevelValuesAsStrings[i] = Integer.toString(verticalLevelDelta * i);
            }
        }
        //calculationg background levels
        for (int i = 0; i < levelsCount; i++) {
            verticalLevelValuesAsStrings[i] = Integer.toString(verticalLevelDelta * i);
        }
    }

    private float axisAnimationFraction;
    private boolean axisAnimationDirectionAppearFromBottom;

    private ValueAnimator verticalAxisValueAnimator;

    private void animateVerticalAxis(boolean maxVisibleValueDecreased) {
        if (verticalAxisValueAnimator != null) {
            verticalAxisValueAnimator.pause();
        }

        axisAnimationDirectionAppearFromBottom = maxVisibleValueDecreased;

        verticalAxisValueAnimator = ValueAnimator.ofFloat(1.0f, 0.0f);

        verticalAxisValueAnimator.setDuration(ANIMATION_DURATION);
        verticalAxisValueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                axisAnimationFraction = (float) animation.getAnimatedValue();
                invalidate();
            }
        });

        verticalAxisValueAnimator.start();
    }


    private void updateVerticalLinesDrawingParams(int maxVisibleValue) {
        yStep = (height - bottomAxisMargin - topAxisMargin) / maxVisibleValue;
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

            updateVerticalLabelsDrawingParams(newMaxVisibleValue);
            updateVerticalLinesDrawingParams(newMaxVisibleValue);
        } else {

            updateVerticalLabelsDrawingParams(newMaxVisibleValue);

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
                    oldVerticalLevelValuesAsStrings = verticalLevelValuesAsStrings.clone();
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

        onHeightChanged(height);

        onChartAndWidthReady();
    }

    private void onHeightChanged(float height) {
        horizontalLabelY = height - axesTextSize / 2;
        yDelta = (height - bottomAxisMargin - topAxisMargin) / levelsCount;
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

        drawBackground(canvas);
        drawVerticalAxis(canvas);

        drawChart(canvas);

        drawSelectedPoints(canvas);
        drawVerticalLabels(canvas);
        drawHorizontalLabels(canvas);
    }


    private void drawBackground(Canvas canvas) {
        canvas.drawRect(0, 0, width, height, backgroundPaint);
    }

    private void drawSelectedPoints(Canvas canvas) {
        if (lastSelectedPointIndex < 0) return;

        nextX = x0 + xStep * lastSelectedPointIndex;

        canvas.drawLine(
                nextX,
                0,
                nextX,
                height - bottomAxisMargin,
                verticalAxisPaint
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
                nextY = height - bottomAxisMargin - pointValue * yStep;

                canvas.drawCircle(nextX, nextY, 10, backgroundPaint);
                canvas.drawCircle(nextX, nextY, 10, chartPaint);
            }
        }
    }

    //to use inside the method only
    private float verticalYAxisCoord;

    private void drawVerticalAxis(Canvas canvas) {
        boolean isAnimationHappening = axisAnimationFraction != 0.0f && axisAnimationFraction != 1.0f;

        if (isAnimationHappening) {
            drawVerticalAxis(canvas, axisAnimationFraction, axisAnimationDirectionAppearFromBottom, true); // верно для новых уровней. нужно написать для старых
            drawVerticalAxis(canvas, axisAnimationFraction, !axisAnimationDirectionAppearFromBottom, false);
        } else {

            for (int i = 0; i < levelsCount; i++) {
                verticalYAxisCoord = height - bottomAxisMargin - i * yDelta;
                verticalAxesLinesCoords[4 * i] = 0;
                verticalAxesLinesCoords[4 * i + 1] = verticalYAxisCoord;
                verticalAxesLinesCoords[4 * i + 2] = width;
                verticalAxesLinesCoords[4 * i + 3] = verticalYAxisCoord;
            }

            canvas.drawLines(verticalAxesLinesCoords, verticalAxisPaint);
        }
    }

    private float[] firstVerticalLIneAnimationCoords = new float[4];

    private void drawVerticalAxis(Canvas canvas, float fraction, boolean appearFromBottom, boolean appearing) {

        int alpha = appearing ? Math.min((int) (0xff * ((1 - fraction) * (1 - fraction))), 0xff) : Math.min((int) (0xff * (fraction * fraction)), 0xff);

        int color = Color.argb(alpha,
                Color.red(verticalAxisPaint.getColor()),
                Color.green(verticalAxisPaint.getColor()),
                Color.blue(verticalAxisPaint.getColor())
        );

        verticalAxisPaintAnimation.setColor(color);

        float animationFraction = appearing ? (1 - fraction) : fraction;

        // do not animate last line
        verticalYAxisCoord = height - bottomAxisMargin;

        firstVerticalLIneAnimationCoords[0] = 0;
        firstVerticalLIneAnimationCoords[1] = verticalYAxisCoord;
        firstVerticalLIneAnimationCoords[2] = width;
        firstVerticalLIneAnimationCoords[3] = verticalYAxisCoord;

        for (int i = 1; i < levelsCount; i++) {
            if (appearFromBottom) {
                verticalYAxisCoord = height - bottomAxisMargin - i * yDelta * animationFraction;
            } else {
                verticalYAxisCoord = height - bottomAxisMargin - (levelsCount) * yDelta + i * yDelta * animationFraction;
            }
            verticalAxesLinesCoords[4 * i] = 0;
            verticalAxesLinesCoords[4 * i + 1] = verticalYAxisCoord;
            verticalAxesLinesCoords[4 * i + 2] = width;
            verticalAxesLinesCoords[4 * i + 3] = verticalYAxisCoord;
        }

        canvas.drawLines(verticalAxesLinesCoords, verticalAxisPaintAnimation);
        canvas.drawLines(firstVerticalLIneAnimationCoords, verticalAxisPaint);
    }


    private void drawVerticalLabels(Canvas canvas) {
        boolean animationIsHappening = axisAnimationFraction != 0.0f && axisAnimationFraction != 1.0f;

        if (animationIsHappening) {
            drawVerticalLabels(canvas, axisAnimationFraction, axisAnimationDirectionAppearFromBottom, true);
            drawVerticalLabels(canvas, axisAnimationFraction, !axisAnimationDirectionAppearFromBottom, false);

        } else {
            for (int i = 0; i < levelsCount; i++) {
                verticalYAxisCoord = height - bottomAxisMargin - i * yDelta;
                canvas.drawText(oldVerticalLevelValuesAsStrings[i], 0, verticalYAxisCoord - axesTextMargin, verticalLabelsPaint);
            }
        }
    }

    private float verticalYAxisCoordAnimation;

    private void drawVerticalLabels(Canvas canvas, float fraction, boolean appearFromBottom, boolean appearing) {
        int alpha = appearing ? Math.min((int) (0xff * ((1 - fraction) * (1 - fraction))), 0xff) : Math.min((int) (0xff * (fraction * fraction)), 0xff);

        int color = Color.argb(alpha,
                Color.red(verticalLabelsPaint.getColor()),
                Color.green(verticalLabelsPaint.getColor()),
                Color.blue(verticalLabelsPaint.getColor())
        );

        verticalLabelsPaintAnimation.setColor(color);

        float animationFraction = appearing ? (1 - fraction) : fraction;

        String[] labelsToUse = appearing ? verticalLevelValuesAsStrings : oldVerticalLevelValuesAsStrings;

        for (int i = 1; i < levelsCount; i++) {
            if (appearFromBottom) {
                verticalYAxisCoordAnimation = height - bottomAxisMargin - (i * yDelta) * animationFraction;
                canvas.drawText(labelsToUse[i], 0, verticalYAxisCoordAnimation - axesTextMargin, verticalLabelsPaintAnimation);
            } else {
                verticalYAxisCoordAnimation = height - bottomAxisMargin - (levelsCount) * yDelta + ((levelsCount - i) * yDelta) * animationFraction;
                canvas.drawText(labelsToUse[i], 0, verticalYAxisCoordAnimation - axesTextMargin, verticalLabelsPaintAnimation);
            }
        }

        verticalYAxisCoordAnimation = height - bottomAxisMargin;
        canvas.drawText(labelsToUse[0], 0, verticalYAxisCoordAnimation - axesTextMargin, verticalLabelsPaint);
    }

    private boolean areLinesVisible() {
        for (boolean visibility : linesVisibility) {
            if (visibility) {
                return true;
            }
        }
        return false;
    }

    //todo: handle chartview touches and show points info

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
            previousY = height - bottomAxisMargin - pointValue * yStep;

            for (int j = firstVisiblePointIndex + 1; j < lastVisiblePointIndex; j++) {
                nextX = x0 + j * xStep;
                pointValue = chartOrdinate.get(j);
                nextY = height - bottomAxisMargin - pointValue * yStep;

                chartPoints[chartPointsIndex++] = previousX;
                chartPoints[chartPointsIndex++] = previousY;
                chartPoints[chartPointsIndex++] = nextX;
                chartPoints[chartPointsIndex++] = nextY;

                previousX = nextX;
                previousY = nextY;
            }

            nextX = x0 + lastVisiblePointIndex * xStep;
            pointValue = chartOrdinate.get(lastVisiblePointIndex);
            nextY = height - bottomAxisMargin - pointValue * yStep;

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

        initVariablesForVerticalChartDrawing();

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
        int backgroundColor = getResources().getColor(nightModeOn ? R.color.darkThemeChartBackground : R.color.lightThemeChartBackground);
        int verticalAxisColor = getResources().getColor(nightModeOn ? R.color.darkThemeAxis : R.color.lightThemeAxis);
        int labelsColor = getResources().getColor(nightModeOn ? R.color.darkThemeLabelText : R.color.lightThemeLabelText);
        backgroundPaint.setColor(backgroundColor);
        verticalAxisPaint.setColor(verticalAxisColor);
        labelPaint.setColor(labelsColor);
        verticalLabelsPaint.setColor(labelsColor);

        invalidate();
    }

}
