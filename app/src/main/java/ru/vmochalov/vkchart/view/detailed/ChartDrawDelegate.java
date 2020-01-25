package ru.vmochalov.vkchart.view.detailed;

import android.animation.ValueAnimator;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static ru.vmochalov.vkchart.utils.CalculationUtil.getMaxValue;

/**
 * Created by Vladimir Mochalov on 19.01.2020.
 */
class ChartDrawDelegate {
    private final int ALPHA_ANIMATION_DURATION = 300;
    private final int LINES_HEIGHT_ANIMATION_DURATION = 500;

    interface MaxVisibleValueListener {
        void onMaxVisibleValueChanged(int previousMaxValue, int newMaxValue);
    }

    private int linesCount;

    private int[] linesAlphas;
    private boolean[] lineVisibilities;

    private List<Integer> colors;
    private List<List<Integer>> chartOrdinates;

    private float x0;
    private int firstVisiblePointIndex;
    private float xStep;
    private float yStep;
    private int lastVisiblePointIndex;
    private float[] chartPoints;

    private float height;

    private int maxVisibleValue;

    private float bottomMarginAxisPx;
    private float topMarginAxisPx;

    private Paint chartPaint = new Paint();
    private Paint selectedPointsPaint = new Paint();

    private ValueAnimator linesAlphaAnimator;
    private ValueAnimator maxVisibleValueAnimator;

    private RedrawCallback redrawCallback;
    private MaxVisibleValueListener maxVisibleValueListener;

    ChartDrawDelegate(
            float lineStrokeWidth,
            float bottomMarginAxisPx,
            float topMarginAxisPx,
            RedrawCallback redrawCallback,
            MaxVisibleValueListener maxVisibleValueListener
    ) {
        this.redrawCallback = redrawCallback;
        this.maxVisibleValueListener = maxVisibleValueListener;

        chartPaint.setStrokeWidth(lineStrokeWidth);
        chartPaint.setStyle(Paint.Style.STROKE);
        chartPaint.setAntiAlias(true);

        selectedPointsPaint.setStrokeWidth(lineStrokeWidth);
        selectedPointsPaint.setStyle(Paint.Style.STROKE);
        selectedPointsPaint.setAntiAlias(true);

        this.bottomMarginAxisPx = bottomMarginAxisPx;
        this.topMarginAxisPx = topMarginAxisPx;
    }

    void onChartInited(int linesCount, List<Integer> colors, List<List<Integer>> chartOrdinates) {
        this.linesCount = linesCount;

        linesAlphas = new int[linesCount];
        lineVisibilities = new boolean[linesCount];

        Arrays.fill(linesAlphas, 0xff);
        Arrays.fill(lineVisibilities, true);

        this.colors = colors;

        this.chartOrdinates = chartOrdinates;
    }

    void onHeightChanged(float height) {
        this.height = height;
    }

    void setLineAlpha(int lineIndex, int alpha) {
        linesAlphas[lineIndex] = alpha;
    }

    boolean isLineVisible(int lineIndex) {
        return lineVisibilities[lineIndex];
    }

    boolean areLinesVisible() {
        for (boolean visibility : lineVisibilities) {
            if (visibility) {
                return true;
            }
        }
        return false;
    }

    void setLineVisibility(final int lineIndex, boolean visible) {
        if (linesAlphaAnimator != null) {
            linesAlphaAnimator.end();
        }
        linesAlphaAnimator = ValueAnimator.ofInt(visible ? 0 : 0xff, visible ? 0xff : 0);
        linesAlphaAnimator.setDuration(ALPHA_ANIMATION_DURATION);

        linesAlphaAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                setLineAlpha(lineIndex, (int) animation.getAnimatedValue());

                redrawCallback.onRedrawRequired();
            }
        });
        linesAlphaAnimator.start();

        lineVisibilities[lineIndex] = visible;
    }

    void onDrawingParamsChanged(float x0, int firstVisiblePointIndex, float xStep, int lastVisiblePointIndex) {
        this.x0 = x0;
        this.firstVisiblePointIndex = firstVisiblePointIndex;
        this.xStep = xStep;
        this.lastVisiblePointIndex = lastVisiblePointIndex;

        chartPoints = new float[(lastVisiblePointIndex - firstVisiblePointIndex + 1) * 4];
    }

    private void onMaxVisibleValueChanged(int newMaxVisibleValue) {
        yStep = (height - bottomMarginAxisPx - topMarginAxisPx) / newMaxVisibleValue;
    }

    void drawChart(Canvas canvas) {
        int chartPointsIndex;
        int tempColor;
        List<Integer> chartOrdinate;
        float previousX;
        float previousY;
        int pointValue;
        float nextX;
        float nextY;

        for (int i = 0; i < linesCount; i++) {
            if (linesAlphas[i] == 0) {
                continue; // skip muted charts
            }

            chartPointsIndex = 0;

            tempColor = colors.get(i);

            int color = Color.argb(
                    linesAlphas[i],
                    Color.red(tempColor),
                    Color.green(tempColor),
                    Color.blue(tempColor)
            );

            chartPaint.setColor(color);
            chartOrdinate = chartOrdinates.get(i);

            previousX = x0 + firstVisiblePointIndex * xStep;
            pointValue = chartOrdinate.get(firstVisiblePointIndex);
            previousY = height - bottomMarginAxisPx - pointValue * yStep;

            for (int j = firstVisiblePointIndex + 1; j < lastVisiblePointIndex; j++) {
                nextX = x0 + j * xStep;
                pointValue = chartOrdinate.get(j);
                nextY = height - bottomMarginAxisPx - pointValue * yStep;

                chartPoints[chartPointsIndex++] = previousX;
                chartPoints[chartPointsIndex++] = previousY;
                chartPoints[chartPointsIndex++] = nextX;
                chartPoints[chartPointsIndex++] = nextY;

                previousX = nextX;
                previousY = nextY;
            }

            nextX = x0 + lastVisiblePointIndex * xStep;
            pointValue = chartOrdinate.get(lastVisiblePointIndex);
            nextY = height - bottomMarginAxisPx - pointValue * yStep;

            chartPoints[chartPointsIndex++] = previousX;
            chartPoints[chartPointsIndex++] = previousY;
            chartPoints[chartPointsIndex++] = nextX;
            chartPoints[chartPointsIndex++] = nextY;

            canvas.drawLines(chartPoints, chartPaint);
        }
    }

    void drawSelectedPoints(
            Canvas canvas,
            Paint verticalAxisPaint,
            Paint backgroundPaint,
            int lastSelectedPointIndex
    ) {
        if (lastSelectedPointIndex < 0) return;

        int pointValue;

        float nextX = x0 + xStep * lastSelectedPointIndex;
        float nextY;
        int tempColor;

        canvas.drawLine(
                nextX,
                0,
                nextX,
                height - bottomMarginAxisPx,
                verticalAxisPaint
        );

        for (int i = 0; i < linesCount; i++) {
            if (lineVisibilities[i]) {

                tempColor = colors.get(i);

                int color = Color.argb(
                        linesAlphas[i],
                        Color.red(tempColor),
                        Color.green(tempColor),
                        Color.blue(tempColor)
                );

                selectedPointsPaint.setColor(color);

                pointValue = chartOrdinates.get(i).get(lastSelectedPointIndex);
                nextY = height - bottomMarginAxisPx - pointValue * yStep;

                canvas.drawCircle(nextX, nextY, 10, backgroundPaint);
                canvas.drawCircle(nextX, nextY, 10, selectedPointsPaint);
            }
        }
    }

    private int getMaxVisibleValue(double startPercent, double endPercent) {
        int totalPointsNumber = chartOrdinates.get(0).size();

        int firstVisiblePointIndex = (int) (totalPointsNumber * startPercent);
        int lastVisiblePointIndex = (int) Math.ceil(totalPointsNumber * endPercent);

        if (firstVisiblePointIndex > 0 && firstVisiblePointIndex == totalPointsNumber) {
            firstVisiblePointIndex = totalPointsNumber - 1;
        }

        List<List<Integer>> visiblePointValues = new ArrayList<>();

        for (int i = 0; i < linesCount; i++) {
            if (isLineVisible(i)) {
                visiblePointValues.add(chartOrdinates.get(i).subList(firstVisiblePointIndex, lastVisiblePointIndex));
            }
        }

        return (visiblePointValues.isEmpty()) ? 0 : getMaxValue(visiblePointValues);
    }

    void updateVerticalDrawingParams(double startPercent, double endPercent) {
        int newMaxVisibleValue = getMaxVisibleValue(startPercent, endPercent);

        if (newMaxVisibleValue != maxVisibleValue) {
            maxVisibleValueListener.onMaxVisibleValueChanged(maxVisibleValue, newMaxVisibleValue);

            if (maxVisibleValueAnimator != null) {
                maxVisibleValueAnimator.pause();
            }

            maxVisibleValueAnimator = ValueAnimator.ofInt(maxVisibleValue, newMaxVisibleValue);
            maxVisibleValueAnimator.setDuration(LINES_HEIGHT_ANIMATION_DURATION);
            maxVisibleValueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    onMaxVisibleValueChanged((int) animation.getAnimatedValue());

                    redrawCallback.onRedrawRequired();
                }
            });
            maxVisibleValueAnimator.start();

            maxVisibleValue = newMaxVisibleValue;
        }
    }
}
