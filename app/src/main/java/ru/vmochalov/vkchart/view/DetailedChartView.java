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
import android.view.View;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import ru.vmochalov.vkchart.R;
import ru.vmochalov.vkchart.dto.Chart;
import timber.log.Timber;

/**
 * Created by Vladimir Mochalov on 10.03.2019.
 */
class DetailedChartView extends View {

    private float height;
    private float width;

    private Chart chart;

    private boolean[] linesVisibility;
    private int[] linesAlphas; // 0 - 255

    private int ANIMATION_DURATION = 500; //ms
    private int ALPHA_ANIMATION_DURATION = 300;

    // styleable attributes
    private int axisTextSize;
    private int lineStrokeWidth;
    private int axisStrokeWidth;

    private List<Integer> fadePointIndexes = new ArrayList<>();

    public DetailedChartView(Context context) {
        super(context);

        initViewWideProperties();
    }

    public DetailedChartView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);

        handleAttributeSet(attributeSet);
        initViewWideProperties();
    }

    public DetailedChartView(Context context, AttributeSet attributeSet, int defStyleAttr) {
        super(context, attributeSet, defStyleAttr);

        handleAttributeSet(attributeSet);
        initViewWideProperties();
    }

    public DetailedChartView(Context context, AttributeSet attributeSet, int defStyleAttr, int defStyleRes) {
        super(context, attributeSet, defStyleAttr, defStyleRes);

        handleAttributeSet(attributeSet);
        initViewWideProperties();
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

    private int absLevelsCount;

    private float[] labelXCoords;

    private int bottomAxisMargin = 40 + 20;
    private int topAxisMargin = 40;
    private int levelsCount = 6;
    private int axesTextSize = 20;
    private int axesTextMargin = 12;

    private double startPercent;
    private double endPercent;

    private int[] pointIndexesToDrawLabel;
    private float pointsInOnePartition;

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

        absLevelsCount = 6;

        labelXCoords = new float[absLevelsCount + 2];

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

        pointIndexesToDrawLabel = new int[absLevelsCount + 2]; // one extra point from every side; required for scrolling

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

            initHorizontalLabelsToDraw();
        }
    }

    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        drawBackground(canvas);
        drawVerticalAxis(canvas);

        drawChart(canvas);

        drawVerticalLabels(canvas);
        drawHorizontalLabels(canvas);
    }


    private void drawBackground(Canvas canvas) {
        canvas.drawRect(0, 0, width, height, backgroundPaint);
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

    //todo: then add animations
    //todo: handle chartview touches and show points info
    // todo: clean code, slight renames and refactorings; add some comments

    private void drawHorizontalAxisStable(Canvas canvas) {
        int indexOfFirstVisiblePoint = (int) (abscissa.size() * startPercent);
        int indexOfLastVisiblePoint = (int) ((abscissa.size() - 1) * endPercent);

        float y = height - axesTextSize / 2;

        String firstLabel = chart.getAbscissaAsString().get(indexOfFirstVisiblePoint);
        float firstLabelWidth = labelPaint.measureText(firstLabel);
        float firstLabelX = 0 + firstLabelWidth / 2;

        canvas.drawRect(
                firstLabelX - firstLabelWidth / 2,
                y - axesTextSize / 2,
                firstLabelX + firstLabelWidth / 2,
                y + axesTextSize / 2,
                debugPaint);

        canvas.drawText(firstLabel, firstLabelX, y, labelPaint);


        String lastLabel = chart.getAbscissaAsString().get(indexOfLastVisiblePoint);
        float lastLabelWidth = labelPaint.measureText(lastLabel);
        float lastLabelX = width - lastLabelWidth / 2;

        canvas.drawRect(
                lastLabelX - lastLabelWidth / 2,
                y - axesTextSize / 2,
                lastLabelX + lastLabelWidth / 2,
                y + axesTextSize / 2,
                debugPaint
        );

        canvas.drawText(lastLabel, lastLabelX, y, labelPaint);

        float totalPointsVisible = indexOfLastVisiblePoint - indexOfFirstVisiblePoint + 1;

        float pointsInOnePartition = totalPointsVisible / (absLevelsCount - 1);

        //todo: check if it's more than two points on the screen. Otherwise it'ss be division by 0
        float middleLabelSpaceWidth = (width - firstLabelWidth - lastLabelWidth) / (absLevelsCount - 2);

        //todo: check out how to adjust label points to updated scale in order to make all of the fully visible
        float middleLabelSpaceWidthUpdated = (width - firstLabelWidth - (middleLabelSpaceWidth / 2 - firstLabelWidth / 2) - lastLabelWidth - (middleLabelSpaceWidth / 2 - lastLabelWidth / 2)) / (absLevelsCount - 2);

        int pointIndexToDraw;

        for (int i = 0; i < absLevelsCount - 2; i++) {
            pointIndexToDraw = (int) (indexOfFirstVisiblePoint + pointsInOnePartition * (i + 1));
            String label = chart.getAbscissaAsString().get(pointIndexToDraw);
            float labelWidth = labelPaint.measureText(label);
            float x = firstLabelWidth + (middleLabelSpaceWidth / 2 - firstLabelWidth / 2) + middleLabelSpaceWidthUpdated * i + (middleLabelSpaceWidthUpdated / 2);

            canvas.drawRect(x - labelWidth / 2, y - axesTextSize / 2, x + labelWidth / 2, y + axesTextSize / 2, debugPaint);
            canvas.drawText(label, x, y, labelPaint);
        }
    }

    public void onVisibleRangeMoved(double startVisiblePercent, double endVisiblePercent) {
        startPercent = startVisiblePercent;
        endPercent = endVisiblePercent;

        initVariablesForChartDrawing();
        reorganizeHorizontalLabelsForDrawing();

        invalidate();
    }

    public void onVisibleRangeScaleChanged(double startVisiblePercent, double endVisiblePercent, boolean startIsStable) {
        startPercent = startVisiblePercent;
        endPercent = endVisiblePercent;

        initVariablesForChartDrawing();

        this.startIsStable = startIsStable;
//        initHorizontalLabelsToDraw();

        invalidate();
    }

    private boolean startIsStable = true;

    private int getFirstFullyVisiblePoint() {
        int indexOfFirstVisiblePoint = (int) (abscissa.size() * startPercent);

        while (!isPointLabelFullyVisible(indexOfFirstVisiblePoint)) {
            indexOfFirstVisiblePoint++;
        }

        return indexOfFirstVisiblePoint;
    }

    private int getLastFullyVisiblePoint() {
        int indexOfLastVisiblePoint = (int) ((abscissa.size() - 1) * endPercent);

        while (!isPointLabelFullyVisible(indexOfLastVisiblePoint)) {
            indexOfLastVisiblePoint--;
        }

        return indexOfLastVisiblePoint;
    }

    private int getFirstVisibleNamedPoint() {
        for (int i = 0; i < pointIndexesToDrawLabel.length; i++) {
            if (isPointLabelFullyVisible(pointIndexesToDrawLabel[i])) {
                return pointIndexesToDrawLabel[i];
            }
        }
        Timber.d("!!!=== return 0 from getFirstVisibleNamedPoint(); ");
        return 0;
    }

    private int getLastVisibleNamedPoint() {
        for (int i = pointIndexesToDrawLabel.length - 1; i >= 0; i--) {
            if (isPointLabelFullyVisible(pointIndexesToDrawLabel[i])) {
                return pointIndexesToDrawLabel[i];
            }
        }

        Timber.d("!!!=== return 0 from getLastVisibleNamedPoint(); ");

        return 0;
    }

    private int currentGapBetweenPoints;

    private void initHorizontalLabelsToDraw() {
        int indexOfFirstVisiblePoint = getFirstFullyVisiblePoint();
        int indexOfLastVisiblePoint = getLastFullyVisiblePoint();

        int firstVisibleLabelIndex = 1;
        int lastVisibleLabelIndex = pointIndexesToDrawLabel.length - 2;

        pointIndexesToDrawLabel[firstVisibleLabelIndex] = indexOfFirstVisiblePoint;
        pointIndexesToDrawLabel[lastVisibleLabelIndex] = indexOfLastVisiblePoint;

        float totalPointsVisible = indexOfLastVisiblePoint - indexOfFirstVisiblePoint + 1;


        pointsInOnePartition = totalPointsVisible / (lastVisibleLabelIndex - firstVisibleLabelIndex);

        int pointIndexToDraw;

        for (int i = 0; i < pointIndexesToDrawLabel.length; i++) {
            if (i == 0) {
                pointIndexToDraw = indexOfFirstVisiblePoint - Math.round(pointsInOnePartition);
                pointIndexesToDrawLabel[i] = pointIndexToDraw;
            } else {
                pointIndexToDraw = indexOfFirstVisiblePoint + Math.round(pointsInOnePartition * (i - 1));
                pointIndexesToDrawLabel[i] = pointIndexToDraw;
            }
        }
    }

    // убрать средние точки
    private void makePointsRare(int pointIndexInLabelArray) {

        int indexInArrayForDrawing = 0;

        for (int i = 0; i < pointIndexesToDrawLabel.length; i++) {
            if (pointIndexesToDrawLabel[i] == pointIndexInLabelArray) {
                indexInArrayForDrawing = i;
                break;
            }
        }

        int existingPointsGap = pointIndexesToDrawLabel[4] - pointIndexesToDrawLabel[3];

        pointsInOnePartition = existingPointsGap * 2;

        int newPointsToDraw[] = new int[pointIndexesToDrawLabel.length];
        List<Integer> pointsToFade = new ArrayList<>();

        newPointsToDraw[indexInArrayForDrawing] = pointIndexInLabelArray;
        Timber.d("!!!=== pointIndexInLabelArray: " + pointIndexInLabelArray + ", indexInArrayForDrawing: " + indexInArrayForDrawing);

        // убрать каждую вторую точку, при этом оставить точку на indexArrayForDrawing на месте

        // make previous points rare
        int nextIndexToInsertTo = indexInArrayForDrawing - 1; // nextIndexToInsertTo
//        for (int i = indexInArrayForDrawing - 1; i >= indexInArrayForDrawing && nextIndexToInsertTo >= 0; i--) {
        for (int i = indexInArrayForDrawing - 1; i >= 0 && nextIndexToInsertTo >= 0; i--) {

            if ((indexInArrayForDrawing - i) % 2 == 0) {
                newPointsToDraw[nextIndexToInsertTo--] = pointIndexesToDrawLabel[i];
            } else {
                pointsToFade.add(pointIndexesToDrawLabel[i]);
            }
        }

//            if (nextIndexToInsertTo > 0) {
        while (nextIndexToInsertTo >= 0) {
            newPointsToDraw[nextIndexToInsertTo] = newPointsToDraw[nextIndexToInsertTo + 1] - existingPointsGap * 2;
            nextIndexToInsertTo--;
        }
//            }

        // make next points rare
        nextIndexToInsertTo = indexInArrayForDrawing + 1;
        for (int i = indexInArrayForDrawing + 1; i < pointIndexesToDrawLabel.length && nextIndexToInsertTo < pointIndexesToDrawLabel.length; i++) {
            if ((i - indexInArrayForDrawing) % 2 == 0) {
                newPointsToDraw[nextIndexToInsertTo++] = pointIndexesToDrawLabel[i];
            } else {
                pointsToFade.add(pointIndexesToDrawLabel[i]);
            }
        }

        while (nextIndexToInsertTo < pointIndexesToDrawLabel.length) {
            newPointsToDraw[nextIndexToInsertTo] = newPointsToDraw[nextIndexToInsertTo - 1] + existingPointsGap * 2;
            nextIndexToInsertTo++;
        }
//        }

        fadePointIndexes.clear();
        fadePointIndexes.addAll(pointsToFade);
//        Timber.d("!!!=== makePointsRare(); all points: " + Arrays.toString(pointIndexesToDrawLabel) + ", points to fade: " + pointsToFade.toString() + ", new points to draw: " + Arrays.toString(newPointsToDraw) + ", existing gap: " + existingPointsGap);

        pointIndexesToDrawLabel = newPointsToDraw;
    }

    private void makePointsCloser(int pointIndexInLabelArray) {

        int indexInArrayForDrawing = 0;

        for (int i = 0; i < pointIndexesToDrawLabel.length; i++) {
            if (pointIndexesToDrawLabel[i] == pointIndexInLabelArray) {
                indexInArrayForDrawing = i;
                break;
            }
        }

        int existingPointsGap = pointIndexesToDrawLabel[4] - pointIndexesToDrawLabel[3];

        if (existingPointsGap <= 1) return;

        pointsInOnePartition = existingPointsGap / 2;

        int newPointsToDraw[] = new int[pointIndexesToDrawLabel.length];
        List<Integer> pointsToFade = new ArrayList<>();

        newPointsToDraw[indexInArrayForDrawing] = pointIndexInLabelArray;

        // между существующими точками вставить еще одну точку посередине, при этом оставить точку на indexArrayForDrawing на месте

        // добавить точек в начале//todo: start from here: add some points between the existing ones
        int nextIndexToInsertTo = indexInArrayForDrawing - 1;

//        for (int i = indexInArrayForDrawing - 1; i >= indexInArrayForDrawing && nextIndexToInsertTo >= 0; i--) {

//        Timber.d("stop");
//        for (int i = indexInArrayForDrawing - 1; i >= 0 && nextIndexToInsertTo >= 0; i--) {
//            if ((indexInArrayForDrawing - i) % 2 == 1) {
//                newPointsToDraw[nextIndexToInsertTo] = newPointsToDraw[nextIndexToInsertTo + 1] - existingPointsGap / 2; //pointIndexesToDrawLabel[i];
//                pointsToFade.add(newPointsToDraw[nextIndexToInsertTo]);
//            } else {
//                newPointsToDraw[nextIndexToInsertTo] = pointIndexesToDrawLabel[(indexInArrayForDrawing - i) / 2];
//            }
//            nextIndexToInsertTo--;
//        }

        Timber.d("stop");
        for (int i = 0; i < indexInArrayForDrawing && nextIndexToInsertTo >= 0; i++) {
            if (i % 2 == 0) {
                int newPointIndex = newPointsToDraw[nextIndexToInsertTo + 1] - existingPointsGap / 2;
                if (Arrays.binarySearch(pointIndexesToDrawLabel, newPointIndex) != -1) {
                    newPointsToDraw[nextIndexToInsertTo] = newPointIndex; //newPointsToDraw[nextIndexToInsertTo + 1] - existingPointsGap / 2; //pointIndexesToDrawLabel[i];
                    pointsToFade.add(newPointsToDraw[nextIndexToInsertTo]);
                }
            } else {
                newPointsToDraw[nextIndexToInsertTo] = pointIndexesToDrawLabel[indexInArrayForDrawing - i / 2 - 1];
            }
            nextIndexToInsertTo--;
        }


//        while (nextIndexToInsertTo >= 0) {
//            newPointsToDraw[nextIndexToInsertTo] = newPointsToDraw[nextIndexToInsertTo + 1] - existingPointsGap / 2;
//            nextIndexToInsertTo--;
//        }
//            }

        // make next points rare
        nextIndexToInsertTo = indexInArrayForDrawing + 1;
        for (int i = indexInArrayForDrawing + 1; i < pointIndexesToDrawLabel.length && nextIndexToInsertTo < pointIndexesToDrawLabel.length; i++) {
            if ((i - indexInArrayForDrawing) % 2 == 1) {
                newPointsToDraw[nextIndexToInsertTo] = newPointsToDraw[nextIndexToInsertTo - 1] + existingPointsGap / 2; //pointIndexesToDrawLabel[i];
                pointsToFade.add(newPointsToDraw[nextIndexToInsertTo]);
            } else {
                newPointsToDraw[nextIndexToInsertTo] = pointIndexesToDrawLabel[(indexInArrayForDrawing + i) / 2];
//                pointsToFade.add(pointIndexesToDrawLabel[i]);
            }
            nextIndexToInsertTo++;
        }

//        while (nextIndexToInsertTo < pointIndexesToDrawLabel.length) {
//            newPointsToDraw[nextIndexToInsertTo] = newPointsToDraw[nextIndexToInsertTo - 1] + existingPointsGap * 2;
//            nextIndexToInsertTo++;
//        }
//        }

        Timber.d("!!!=== makePointsCloser(); all points: " + Arrays.toString(pointIndexesToDrawLabel) + ", points to fade: " + pointsToFade.toString() + ", new points to draw: " + Arrays.toString(newPointsToDraw) + ", existing gap: " + existingPointsGap + ", pointIndexInLabelArray: " + pointIndexInLabelArray);

        fadePointIndexes.clear();

        fadePointIndexes.addAll(pointsToFade);

        pointIndexesToDrawLabel = newPointsToDraw;

    }

    private void reorganizeHorizontalLabelsForDrawing() {
        while (isPointLabelNotVisibleYet(pointIndexesToDrawLabel[1])) { // как только 1-ая уходит за ноль
            moveFirstPointToTheTail(); // , то перенести 0-ую в конец
        }

        while (isPointLabelNotVisibleAlready(pointIndexesToDrawLabel[pointIndexesToDrawLabel.length - 2])) { // как только предпоследняя уходит за границу
            moveLastPointToTheBeginning(); // то перенести последнюю в начало
        }
    }

    private void moveFirstPointToTheTail() {
        for (int i = 0; i < pointIndexesToDrawLabel.length - 1; i++) {
            pointIndexesToDrawLabel[i] = pointIndexesToDrawLabel[i + 1];
        }

        int newIndex = pointIndexesToDrawLabel[pointIndexesToDrawLabel.length - 2] + Math.round(pointsInOnePartition);
        pointIndexesToDrawLabel[pointIndexesToDrawLabel.length - 1] = newIndex;
    }

    private void moveLastPointToTheBeginning() {
        for (int i = pointIndexesToDrawLabel.length - 1; i > 0; i--) {
            pointIndexesToDrawLabel[i] = pointIndexesToDrawLabel[i - 1];
        }

        int newIndex = pointIndexesToDrawLabel[1] - Math.round(pointsInOnePartition);
        pointIndexesToDrawLabel[0] = newIndex;
    }

    private boolean isPointLabelNotVisibleYet(int pointIndex) {
        if (isPointIndexValid(pointIndex)) {
            String label = chart.getAbscissaAsString().get(pointIndex);

            float labelWidth = labelPaint.measureText(label);

            float labelStartX = x0 + xStep * pointIndex - labelWidth / 2;
            float labelEndX = labelStartX + labelWidth;

            return (labelEndX < 0);
        } else {
            return true;
        }
    }

    private boolean isPointLabelNotVisibleAlready(int pointIndex) {
        if (isPointIndexValid(pointIndex)) {
            String label = chart.getAbscissaAsString().get(pointIndex);

            float labelWidth = labelPaint.measureText(label);

            float labelStartX = x0 + xStep * pointIndex - labelWidth / 2;

            return (labelStartX > width);
        } else {
            return true;
        }
    }

    private boolean isPointLabelFullyVisible(int pointIndex) {
        if (isPointIndexValid(pointIndex)) {
            String label = chart.getAbscissaAsString().get(pointIndex);
            float labelWidth = labelPaint.measureText(label);
            float labelStartX = x0 + xStep * pointIndex - labelWidth / 2;
            float labelEndX = labelStartX + labelWidth;

            return labelStartX > 0 && labelEndX < width;
        } else {
            return false;
        }
    }

//    private int minimumLimitBetweenLabels = 30;
//    private int maximumLimitBetweenLabels = 100;

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

    private boolean isTooMuchSpaceOnStart() {
        int firstVisible = getFirstVisibleNamedPoint();

        String labelOne = chart.getAbscissaAsString().get(firstVisible);
        float labelOneWidth = labelPaint.measureText(labelOne);
        float labelOneStartX = x0 + xStep * firstVisible - labelOneWidth / 2;

        return labelOneStartX > TOO_FAR_CONSTANT * 2;
    }

    private boolean isTooMuchSpaceOnEnd() {
        int lastVisible = getLastVisibleNamedPoint();

        String label = chart.getAbscissaAsString().get(lastVisible);
        float labelOneWidth = labelPaint.measureText(label);
        float labelOneStartX = x0 + xStep * lastVisible - labelOneWidth / 2;
        float labelOneEndX = labelOneStartX + labelOneWidth;

        return width - TOO_FAR_CONSTANT * 2 > labelOneEndX;

    }

    private boolean isPointIndexValid(int index) {
        return index >= firstDateIndex && index <= lastDateIndex;
    }

    private void onPointsTooClose() {
        makePointsRare(startIsStable ? getFirstVisibleNamedPoint() : getLastVisibleNamedPoint());
        animateHorizontalLabels(false);
    }

    private void onPointsTooFar() {
        makePointsCloser(startIsStable ? getFirstVisibleNamedPoint() : getLastVisibleNamedPoint());
        animateHorizontalLabels(true);
    }

    private ValueAnimator horizontalLabelsAnimator = null;
    private float horizontalLabelsAlpha = 1;

    private boolean labelsAlphaAnimationInProgress = false;

    private void animateHorizontalLabels(boolean appear) {
        if (horizontalLabelsAnimator != null) {
            horizontalLabelsAnimator.end();
        }

        horizontalLabelsAnimator = ValueAnimator.ofFloat(appear ? 0.0f : 1.0f, appear ? 1.0f : 0.0f);

        horizontalLabelsAnimator.setDuration(ANIMATION_DURATION);
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

        boolean animationInProgress = labelsAlphaAnimationInProgress; //!(horizontalLabelsAlpha == 0.0f || horizontalLabelsAlpha == 1.0f);

        if (animationInProgress) {
            int color = Color.argb(
                    (int) (0xff * horizontalLabelsAlpha),
                    Color.red(labelPaint.getColor()),
                    Color.green(labelPaint.getColor()),
                    Color.blue(labelPaint.getColor())
            );

            labelPaintAnimation.setColor(color);

            for (int i : fadePointIndexes) {

                if (isPointIndexValid(i)) {
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

        //calculating labels coordinates
        for (int i = 0; i < pointIndexesToDrawLabel.length; i++) {
            labelXCoords[i] = x0 + xStep * pointIndexesToDrawLabel[i];
        }

        boolean isFirstPoint = true;

        int firstPointIndex = getFirstVisibleNamedPoint();

        int indexInArrayForDrawing = 0;

        for (int i = 0; i < pointIndexesToDrawLabel.length; i++) {
            if (pointIndexesToDrawLabel[i] == firstPointIndex) {
                indexInArrayForDrawing = i;
                break;
            }
        }

        if (indexInArrayForDrawing == pointIndexesToDrawLabel.length - 1) {
            isFirstPoint = false;

            int lastPointIndex = getLastVisibleNamedPoint();

            for (int i = pointIndexesToDrawLabel.length - 1; i >= 0; i--) {
                if (pointIndexesToDrawLabel[i] == lastPointIndex) {
                    indexInArrayForDrawing = i;
                    break;
                }
            }
        }

        int secondIndex = isFirstPoint ? indexInArrayForDrawing + 1 : indexInArrayForDrawing - 1;

        if (isTooMuchSpaceOnStart() || isTooMuchSpaceOnEnd()) {
            onPointsTooFar();
        } else if (areNeightborPointsTooClose(pointIndexesToDrawLabel[indexInArrayForDrawing], pointIndexesToDrawLabel[secondIndex])) {
            onPointsTooClose();
        } else if (
                areNeighbourPointsTooFar(pointIndexesToDrawLabel[indexInArrayForDrawing], pointIndexesToDrawLabel[secondIndex])

        ) {
            onPointsTooFar();
        }
//        boolean close = ;

//        boolean far = ;
//        Timber.d("close: " + close + ", far: " + far);


        //drawing
        for (int i = 0; i < labelXCoords.length; i++) {
            if (isPointIndexValid(pointIndexesToDrawLabel[i])) {
                if (!animationInProgress || !fadePointIndexes.contains(pointIndexesToDrawLabel[i])) {
                    canvas.drawText(
                            chart.getAbscissaAsString().get(pointIndexesToDrawLabel[i]),
                            labelXCoords[i],
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

        this.linesVisibility = new boolean[chart.getLabels().size()];
        Arrays.fill(linesVisibility, true);

        linesAlphas = new int[chart.getLabels().size()];
        Arrays.fill(linesAlphas, 0xff);

        linesCount = chart.getLineIds().size();


        onChartAndWidthReady();
//        initHorizontalLabelsToDraw();

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
