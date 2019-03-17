package ru.vmochalov.vkchart.view;

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
import ru.vmochalov.vkchart.dto.CombinedChart;

/**
 * Created by Vladimir Mochalov on 10.03.2019.
 */
public class ChartView extends View {

    private float height;

    private float width;

    private int backgroundColor = Color.WHITE;

    private int axesColor = Color.GRAY;

    private CombinedChart combinedChart;

    private boolean[] chartsVisibility;

    // styleable attributes
    private int axisTextSize;
    private int lineStrokeWidth;
    private int axisStrokeWidth;

    public ChartView(Context context) {
        super(context);

        initViewWideProperties();
    }

    public ChartView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);

        handleAttributeSet(attributeSet);
        initViewWideProperties();
    }

    public ChartView(Context context, AttributeSet attributeSet, int defStyleAttr) {
        super(context, attributeSet, defStyleAttr);

        handleAttributeSet(attributeSet);
        initViewWideProperties();
    }

    public ChartView(Context context, AttributeSet attributeSet, int defStyleAttr, int defStyleRes) {
        super(context, attributeSet, defStyleAttr, defStyleRes);

        handleAttributeSet(attributeSet);
        initViewWideProperties();
    }

    private void handleAttributeSet(AttributeSet attributeSet) {
        TypedArray attributes = getContext().obtainStyledAttributes(attributeSet, R.styleable.ChartView);
        Resources resources = getContext().getResources();

        axisTextSize = attributes.getDimensionPixelSize(
                R.styleable.ChartView_axisTextSize,
                resources.getDimensionPixelSize(R.dimen.chartViewDefaultTextSize)
        );

        lineStrokeWidth = attributes.getDimensionPixelSize(
                R.styleable.ChartView_chartLineWidth,
                resources.getDimensionPixelSize(R.dimen.chartViewDefaultLineStrokeWidth)
        );

        axisStrokeWidth = attributes.getDimensionPixelSize(
                R.styleable.ChartView_axisLineWidth,
                resources.getDimensionPixelSize(R.dimen.chartViewDefaultAxisStrokeWidth)
        );

        attributes.recycle();
    }

    private Paint chartPaint = new Paint();
    private Paint backgroundPaint = new Paint();
    private Paint debugPaint = new Paint();
    private Paint labelPaint = new Paint();
    private Paint verticalAxisPaint = new Paint();

    private int absLevelsCount;

    private float[] labelXCoords;

    private int bottomAxisMargin = 40;
    private int topAxisMargin = 40;
    private int levelsCount = 6;
    private int axesTextSize = 20;
    private int axesTextMargin = 4;

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

    private int[] verticalLevelValues;

    private String[] verticalLevelValuesAsStrings;

    private int verticalLevelDelta;

    private float[] verticalAxesLinesCoords;

    private int linesCount;

    private int firstVisiblePointIndex;
    private int lastVisiblePointIndex;

    private float horizontalLabelY;

    //drawing level lines
    float yDelta;

    private void initViewWideProperties() {
        backgroundPaint.setColor(backgroundColor);
        backgroundPaint.setStyle(Paint.Style.FILL_AND_STROKE);

        absLevelsCount = 6;

        labelXCoords = new float[absLevelsCount + 2];

        labelPaint.setTextAlign(Paint.Align.CENTER);
        labelPaint.setColor(axesColor);
        labelPaint.setTextSize(axisTextSize);
        labelPaint.setStrokeWidth(axisStrokeWidth);
        labelPaint.setAntiAlias(true);
        labelPaint.setStyle(Paint.Style.FILL_AND_STROKE);

        startPercent = 0;
        endPercent = 1;

        pointIndexesToDrawLabel = new int[absLevelsCount + 2]; // one extra point from every side; required for scrolling

        chartPaint.setStrokeWidth(lineStrokeWidth);
        chartPaint.setStyle(Paint.Style.STROKE);
        chartPaint.setAntiAlias(true);

        debugPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        debugPaint.setColor(Color.LTGRAY);

        verticalAxisPaint.setColor(axesColor);
        verticalAxisPaint.setTextSize(axisTextSize);
        verticalAxisPaint.setStrokeWidth(axisStrokeWidth);
        verticalAxisPaint.setTextAlign(Paint.Align.LEFT);
        verticalAxisPaint.setAntiAlias(true);
        verticalAxisPaint.setStyle(Paint.Style.FILL_AND_STROKE);
    }

    private void initVariablesForChartDrawing() {
        initVariablesForHorizontalChartDrawing();
        initVariablesForVerticalChartDrawing();
    }

    private void initVariablesForHorizontalChartDrawing() {
        visibleWidth = width * (endPercent - startPercent);
        abscissa = combinedChart.getAbscissa();

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

    private void initVariablesForVerticalChartDrawing() {
        yStep = (height - bottomAxisMargin - topAxisMargin) / getMaxVisibleValue();

        verticalLevelValues = new int[levelsCount]; // from bottom to top

        verticalLevelValuesAsStrings = new String[levelsCount];

        verticalLevelDelta = getMaxVisibleValue() / levelsCount;

        if (!areLinesVisible() || verticalLevelDelta == 0)
            verticalLevelDelta = 1; // in case user is confused

        //calculationg background levels
        for (int i = 0; i < levelsCount; i++) {
            verticalLevelValues[i] = verticalLevelDelta * i;
            verticalLevelValuesAsStrings[i] = Integer.toString(verticalLevelDelta * i);
        }


        verticalAxesLinesCoords = new float[levelsCount * 4];

        //drawing level lines
        yDelta = (height - bottomAxisMargin - topAxisMargin) / levelsCount;

        horizontalLabelY = height - axesTextSize / 2;
    }

    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        //todo: send preferred size
    }

    public void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        height = bottom - top;
        width = right - left;
    }

    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        drawBackground(canvas);
        drawVerticalAxis(canvas);

//        drawHorizontalAxisStable(canvas);
        drawChart(canvas);
        drawHorizontalLabels(canvas);

        //todo: update look&feel styles: texts, colors, stroke width, spaces, etc...

    }


    private void drawBackground(Canvas canvas) {
        canvas.drawRect(0, 0, width, height, backgroundPaint);
    }

    //to use inside the method only
    private float verticalYAxisCoord;

    private void drawVerticalAxis(Canvas canvas) {
        for (int i = 0; i < levelsCount; i++) {
            verticalYAxisCoord = height - bottomAxisMargin - i * yDelta;
            verticalAxesLinesCoords[4 * i] = 0;
            verticalAxesLinesCoords[4 * i + 1] = verticalYAxisCoord;
            verticalAxesLinesCoords[4 * i + 2] = width;
            verticalAxesLinesCoords[4 * i + 3] = verticalYAxisCoord;

            canvas.drawText(verticalLevelValuesAsStrings[i], axesTextMargin, verticalYAxisCoord - axesTextMargin, verticalAxisPaint);
        }

        canvas.drawLines(verticalAxesLinesCoords, verticalAxisPaint);
    }

    private boolean areLinesVisible() {
        for (boolean visibility : chartsVisibility) {
            if (visibility) {
                return true;
            }
        }
        return false;
    }

    // todo: then clean code and optimize as possible
    // todo: then unite all in a single ViewGroup
    //todo: then add animations

    private void drawHorizontalAxisStable(Canvas canvas) {
        int indexOfFirstVisiblePoint = (int) (abscissa.size() * startPercent);
        int indexOfLastVisiblePoint = (int) ((abscissa.size() - 1) * endPercent);

        float y = height - axesTextSize / 2;

        String firstLabel = combinedChart.getAbscissaAsString().get(indexOfFirstVisiblePoint);
        float firstLabelWidth = labelPaint.measureText(firstLabel);
        float firstLabelX = 0 + firstLabelWidth / 2;

        canvas.drawRect(
                firstLabelX - firstLabelWidth / 2,
                y - axesTextSize / 2,
                firstLabelX + firstLabelWidth / 2,
                y + axesTextSize / 2,
                debugPaint);

        canvas.drawText(firstLabel, firstLabelX, y, labelPaint);


        String lastLabel = combinedChart.getAbscissaAsString().get(indexOfLastVisiblePoint);
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
            String label = combinedChart.getAbscissaAsString().get(pointIndexToDraw);
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

    public void onVisibleRangeScaleChanged(double startVisiblePercent, double endVisiblePercent) {
        startPercent = startVisiblePercent;
        endPercent = endVisiblePercent;

        initVariablesForChartDrawing();

        initHorizontalLabelsToDraw();

        invalidate();
    }


    private void initHorizontalLabelsToDraw() {
        int indexOfFirstVisiblePoint = (int) (abscissa.size() * startPercent);
        int indexOfLastVisiblePoint = (int) ((abscissa.size() - 1) * endPercent);

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
            String label = combinedChart.getAbscissaAsString().get(pointIndex);

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
            String label = combinedChart.getAbscissaAsString().get(pointIndex);

            float labelWidth = labelPaint.measureText(label);

            float labelStartX = x0 + xStep * pointIndex - labelWidth / 2;

            return (labelStartX > width);
        } else {
            return true;
        }
    }

    private boolean isPointIndexValid(int index) {
        return index >= firstDateIndex && index <= lastDateIndex;
    }

    private void drawHorizontalLabels(Canvas canvas) {
        //calculating labels coordinates
        for (int i = 0; i < pointIndexesToDrawLabel.length; i++) {
            labelXCoords[i] = x0 + xStep * pointIndexesToDrawLabel[i];
        }

        //drawing
        for (int i = 0; i < labelXCoords.length; i++) {
            if (isPointIndexValid(pointIndexesToDrawLabel[i])) {
                canvas.drawText(
                        combinedChart.getAbscissaAsString().get(pointIndexesToDrawLabel[i]),
                        labelXCoords[i],
                        horizontalLabelY,
                        labelPaint
                );
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

    private void drawChart(Canvas canvas) {
        for (int i = 0; i < linesCount; i++) {
            if (!chartsVisibility[i]) {
                continue; // skip muted charts
            }

            chartPointsIndex = 0;
            chartPaint.setColor(combinedChart.getColors().get(i));
            chartOrdinate = combinedChart.getOrdinates().get(i);

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

    public void setChart(CombinedChart combinedChart) {
        this.combinedChart = combinedChart;

        this.chartsVisibility = new boolean[combinedChart.getLabels().size()];

        Arrays.fill(chartsVisibility, true);

        linesCount = combinedChart.getLineIds().size();

        initVariablesForChartDrawing();

        initHorizontalLabelsToDraw();

        invalidate();
    }

    public void setLineVisibility(String lineId, boolean visible) {
        int lineIndex = combinedChart.getLineIds().indexOf(lineId);

        if (lineIndex != -1) {
            chartsVisibility[lineIndex] = visible;
            initVariablesForVerticalChartDrawing();
            invalidate();
        }
    }

    private List<List<Integer>> visiblePointValues = new ArrayList<>();

    private int getMaxVisibleValue() {

        int absSize = combinedChart.getAbscissa().size();

        int firstVisiblePointIndex = (int) (absSize * startPercent);
        int lastVisiblePointIndex = (int) Math.ceil(absSize * endPercent);

        if (firstVisiblePointIndex > 0 && firstVisiblePointIndex == absSize) {
            firstVisiblePointIndex = absSize - 1;
        }

        visiblePointValues.clear();

        for (int i = 0; i < combinedChart.getLineIds().size(); i++) {
            if (chartsVisibility[i]) {
                visiblePointValues.add(combinedChart.getOrdinates().get(i).subList(firstVisiblePointIndex, lastVisiblePointIndex));
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

}
