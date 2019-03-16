package ru.vmochalov.vkchart.view;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import ru.vmochalov.vkchart.R;
import ru.vmochalov.vkchart.dto.CombinedChart;
import timber.log.Timber;

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

    Path chartPath = new Path();

    private double visibleWidth;
    private int firstDateIndex;
    private int lastDateIndex;
    private float xStep;
    private float enlargedWidth;
    private float x0;
    private float yStep;
    private List<Date> abscissa;

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
    }

    private void initVariablesForChartDrawing() {
        visibleWidth = width * (endPercent - startPercent);
        abscissa = combinedChart.getAbscissa();

        firstDateIndex = 0;
        lastDateIndex = abscissa.size() - 1;

        xStep = width / abscissa.size();
        xStep *= (width / visibleWidth);

        enlargedWidth = (float) (width * width / visibleWidth);

        x0 = (float) (-enlargedWidth * startPercent);

        yStep = (height - bottomAxisMargin - topAxisMargin) / getMaxVisibleValue();
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

        Timber.d("onDraw");

        drawBackground(canvas);
        drawVerticalAxis(canvas);

//        drawHorizontalAxisStable(canvas);
        drawChart(canvas);
        calculateXForLabels();
        drawHorizontalLabels(canvas);

        //todo: update look&feel styles: texts, colors, stroke width, spaces, etc...

    }


    private void drawBackground(Canvas canvas) {
        canvas.drawRect(0, 0, width, height, backgroundPaint);
    }

    private void drawVerticalAxis(Canvas canvas) {
        debugPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        debugPaint.setColor(Color.LTGRAY);

        //todo: add axes changes when period is changed
        verticalAxisPaint.setColor(axesColor);
        verticalAxisPaint.setTextSize(axisTextSize);
        verticalAxisPaint.setStrokeWidth(axisStrokeWidth);
        verticalAxisPaint.setTextAlign(Paint.Align.LEFT);
        verticalAxisPaint.setAntiAlias(true);
        verticalAxisPaint.setStyle(Paint.Style.FILL_AND_STROKE);

        int[] levelValues = new int[levelsCount]; // from bottom to top


        int levelDelta = getMaxVisibleValue() / levelsCount;

        //calculationg background levels
        for (int i = 0; i < levelsCount; i++) {
            levelValues[i] = levelDelta * i;
        }

        //drawing level lines
        float yDelta = (height - bottomAxisMargin - topAxisMargin) / levelsCount;

        Path path = new Path();
        for (int i = 0; i < levelsCount; i++) {
            float y = height - bottomAxisMargin - i * yDelta;
            path.moveTo(0, y);
            path.lineTo(width, y);
            canvas.drawText(Integer.toString(levelValues[i]), 0 + axesTextMargin, y - axesTextMargin, verticalAxisPaint);
        }

        canvas.drawPath(path, verticalAxisPaint);
    }

    // todo: Then add support of changing chart visibility from the outside.
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

    private void calculateXForLabels() {
        float x;

        for (int i = 0; i < pointIndexesToDrawLabel.length; i++) {
            x = x0 + xStep * pointIndexesToDrawLabel[i];

            labelXCoords[i] = x;
        }
    }

    private void drawHorizontalLabels(Canvas canvas) {
        float labelY = height - axesTextSize / 2;

        for (int i = 0; i < labelXCoords.length; i++) {
            int index = pointIndexesToDrawLabel[i];

            if (isPointIndexValid(index)) {
                String label = combinedChart.getAbscissaAsString().get(index);

                canvas.drawText(label, labelXCoords[i], labelY, labelPaint);
            }
        }
    }

    private void drawChart(Canvas canvas) {
        for (int i = 0; i < combinedChart.getLineIds().size(); i++) {
            if (!chartsVisibility[i]) {
                continue; // skip muted charts
            }

            chartPaint.setColor(combinedChart.getColors().get(i));
            chartPath.reset();

            float previousX;
            float previousY;

            // put first point
            float x = x0 + 0;
            int value = combinedChart.getOrdinates().get(i).get(firstDateIndex);
            float y = height - bottomAxisMargin - value * yStep;

            if (x >= 0 && x <= width) {
                chartPath.moveTo(x, y);
            }
            previousX = x;
            previousY = y;

            // put middle points
            for (int j = firstDateIndex + 1; j < lastDateIndex; j++) {
                x = x0 + j * xStep;
                value = combinedChart.getOrdinates().get(i).get(j);
                y = height - bottomAxisMargin - value * yStep;

                if (x >= 0 && x <= width) {
                    // put point
                    if (chartPath.isEmpty()) {
                        chartPath.moveTo(previousX, previousY);
                    }

                    chartPath.lineTo(x, y);
                }

                if (x > width && previousX <= width) {
                    chartPath.lineTo(x, y);
                }

                previousX = x;
                previousY = y;
            }

            // put last point
            x = x0 + enlargedWidth;
            value = combinedChart.getOrdinates().get(i).get(lastDateIndex);
            y = height - bottomAxisMargin - value * yStep;

            if (previousX <= width) {
                chartPath.lineTo(x, y);
            }

            canvas.drawPath(chartPath, chartPaint);
        }
    }

    public void setChart(CombinedChart combinedChart) {
        this.combinedChart = combinedChart;

        this.chartsVisibility = new boolean[combinedChart.getLabels().size()];

        Arrays.fill(chartsVisibility, true);

        initVariablesForChartDrawing();

        initHorizontalLabelsToDraw();

        invalidate();
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

        return getMaxValue(visiblePointValues);
    }

    private int getMaxValue(List<List<Integer>> lists) {
        int max = Integer.MIN_VALUE;

        for (List<Integer> list : lists) {
            max = Math.max(max, Collections.max(list));
        }

        return max;
    }

}
