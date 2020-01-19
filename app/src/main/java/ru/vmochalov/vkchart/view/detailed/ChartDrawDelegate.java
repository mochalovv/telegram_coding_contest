package ru.vmochalov.vkchart.view.detailed;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import java.util.Arrays;
import java.util.List;

/**
 * Created by Vladimir Mochalov on 19.01.2020.
 */
class ChartDrawDelegate {

    private int linesCount;

    private int[] linesAlphas;

    private List<Integer> colors;
    private List<List<Integer>> chartOrdinates;

    private float x0;
    private int firstVisiblePointIndex;
    private float xStep;
    private float yStep;
    private int lastVisiblePointIndex;
    private float[] chartPoints;

    private float height;

    private float bottomMarginAxisPx;
    private float topMarginAxisPx;

    private Paint chartPaint = new Paint();
    private Paint selectedPointsPaint = new Paint();


    ChartDrawDelegate(float lineStrokeWidth, float bottomMarginAxisPx, float topMarginAxisPx) {
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
        Arrays.fill(linesAlphas, 0xff);

        this.colors = colors;

        this.chartOrdinates = chartOrdinates;
    }

    void onHeightChanged(float height) {
        this.height = height;
    }

    void setLineAlpha(int lineIndex, int alpha) {
        linesAlphas[lineIndex] = alpha;
    }

    void onDrawingParamsChanged(float x0, int firstVisiblePointIndex, float xStep, int lastVisiblePointIndex) {
        this.x0 = x0;
        this.firstVisiblePointIndex = firstVisiblePointIndex;
        this.xStep = xStep;
        this.lastVisiblePointIndex = lastVisiblePointIndex;

        chartPoints = new float[(lastVisiblePointIndex - firstVisiblePointIndex + 1) * 4];
    }

    void onMaxVisibleValueChanged(int maxVisibleValue) {
        yStep = (height - bottomMarginAxisPx - topMarginAxisPx) / maxVisibleValue;
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
            int lastSelectedPointIndex,
            boolean[] linesVisibility
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
            if (linesVisibility[i]) {

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
}
