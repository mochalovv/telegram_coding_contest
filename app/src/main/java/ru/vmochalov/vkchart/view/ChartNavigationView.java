package ru.vmochalov.vkchart.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import ru.vmochalov.vkchart.dto.CombinedChart;

public class ChartNavigationView extends View {

    private float width;
    private float height;

    private static final int firstDateIndex = 0;
    private int lastDateIndex;
    private int periodStartDateIndex;
    private int periodEndDateIndex;

    private int maxValue;

    private CombinedChart combinedChart;

    private boolean[] lineVisibility;

    // styleable attributes
    private float lineStrokeWidth = 5;
    private float frameHorizontalBorderWidth = 10;
    private float frameVerticalBorderWidth = 4;

    private int linesCount;

    private PeriodChangedListener periodChangedListener;

    public interface PeriodChangedListener {
        //  0.0 <= x <= 1.0
        void onPeriodLengthChanged(double periodStart, double periodEnd);

        // 0.0 <= x <= 1.0
        void onPeriodMoved(double periodStart, double periodEnd);

        void onPeriodModifyFinished();
    }

    public ChartNavigationView(Context context) {
        super(context);
        initTouchListener();

        initVariableForDrawing();
    }

    public ChartNavigationView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        initTouchListener();

        initVariableForDrawing();
    }

    public ChartNavigationView(Context context, AttributeSet attributeSet, int defStyleAttr) {
        super(context, attributeSet, defStyleAttr);
        initTouchListener();

        initVariableForDrawing();
    }

    public ChartNavigationView(Context context, AttributeSet attributeSet, int defStyleAttr, int defStyleRes) {
        super(context, attributeSet, defStyleAttr, defStyleRes);
        initTouchListener();

        initVariableForDrawing();
    }

    private enum TouchType {
        LEFT_BORDER_TOUCH, FRAME_TOUCH, RIGHT_BORDER_TOUCH, UNHANDLED_TOUCH
    }

    private void initTouchListener() {
        setOnTouchListener(
                new OnTouchListener() {

                    private float borderTouchArea = 20; //px

                    private float minimumFrameWidth = 40; // px

                    //                    private boolean isTouchHandled = false;
                    private TouchType touchType;

                    private float previousX;
                    private float dx;
                    private float x;

                    @Override
                    public boolean onTouch(View v, MotionEvent event) {

                        if (event.getAction() == MotionEvent.ACTION_DOWN) {
                            x = event.getX();
                            previousX = event.getX();

                            if (x >= frameStart - borderTouchArea && x <= frameStart + borderTouchArea) {
                                touchType = TouchType.LEFT_BORDER_TOUCH;
                            } else if (x >= frameStart + frameWidth - borderTouchArea && x <= frameStart + frameWidth + borderTouchArea) {
                                touchType = TouchType.RIGHT_BORDER_TOUCH;
                            } else if (x >= frameStart && x <= frameStart + frameWidth) {
                                touchType = TouchType.FRAME_TOUCH;
                            } else {
                                touchType = TouchType.UNHANDLED_TOUCH;
                            }

//                            Timber.d("touchType: " + touchType);
                        } else if (event.getAction() == MotionEvent.ACTION_UP) {

                            if (touchType == TouchType.LEFT_BORDER_TOUCH || touchType == TouchType.FRAME_TOUCH || touchType == TouchType.RIGHT_BORDER_TOUCH) {
                                if (periodChangedListener != null) {
                                    periodChangedListener.onPeriodModifyFinished();
                                }
                            }

                            touchType = null;
                        } else if (event.getAction() == MotionEvent.ACTION_MOVE) {

                            if (touchType != TouchType.UNHANDLED_TOUCH) {

                                //do not handle points outside the view
                                if (event.getX() < 0) {
                                    x = 0;
                                } else if (event.getX() > width) {
                                    x = width;
                                } else {
                                    x = event.getX();
                                }

                                dx = x - previousX;
                                previousX = x;

                                // do not allow frame move outside the view
                                if ((touchType == TouchType.FRAME_TOUCH || touchType == TouchType.LEFT_BORDER_TOUCH) && frameStart + dx < 0) {
                                    dx = -frameStart;
                                } else if ((touchType == TouchType.FRAME_TOUCH || touchType == TouchType.RIGHT_BORDER_TOUCH) && frameStart + frameWidth + dx > width) {
                                    dx = width - frameStart - frameWidth;
                                }

                                //do not allow frame be "left side right"
                                if (touchType == TouchType.LEFT_BORDER_TOUCH) {
                                    if (frameStart + dx + minimumFrameWidth > frameStart + frameWidth) {
                                        dx = frameWidth - minimumFrameWidth;
                                    }
                                } else if (touchType == TouchType.RIGHT_BORDER_TOUCH) {
                                    if (frameStart + frameWidth + dx < frameStart + minimumFrameWidth) {
                                        dx = minimumFrameWidth - frameWidth;
                                    }
                                }

                                // consume dx according to current event
                                if (touchType == TouchType.FRAME_TOUCH) {
                                    frameStart += dx;
                                } else if (touchType == TouchType.LEFT_BORDER_TOUCH) {
                                    frameStart += dx;
                                    frameWidth -= dx;
                                } else if (touchType == TouchType.RIGHT_BORDER_TOUCH) {
                                    frameWidth += dx;
                                }

                                double frameStartInPercent = frameStart / width;
                                double frameEndInPercent = (frameStart + frameWidth) / width;

                                if (frameStartInPercent < 0) {
                                    frameStartInPercent = 0;
                                }

                                if (frameEndInPercent > 1) {
                                    frameEndInPercent = 1;
                                }


                                if (periodChangedListener != null & dx != 0) {
                                    if (touchType == TouchType.FRAME_TOUCH) {
                                        periodChangedListener.onPeriodMoved(frameStartInPercent, frameEndInPercent);
                                    } else if (touchType == TouchType.LEFT_BORDER_TOUCH || touchType == TouchType.RIGHT_BORDER_TOUCH) {
                                        periodChangedListener.onPeriodLengthChanged(frameStartInPercent, frameEndInPercent);
                                    }
                                }
                            }

                            ChartNavigationView.this.invalidate();
                        }
                        return true;
                    }
                }
        );

    }

    private boolean initialValueIsSent = false;

    public void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        width = right - left;
        height = bottom - top;

        if (!initialValueIsSent) {
            if (periodChangedListener != null) {
                double frameStartInPercent = frameStart / width;
                double frameEndInPercent = (frameStart + frameWidth) / width;

                periodChangedListener.onPeriodLengthChanged(frameStartInPercent, frameEndInPercent);

                initialValueIsSent = true;

            }
        }
    }

    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        //todo: send prefered size as in arrival
    }

    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        drawBackground(canvas);
        drawChart(canvas);
    }

    private Paint activeBackgroundPaint = new Paint();
    private Paint framePaint = new Paint();
    private Paint duff = new Paint();
    private Paint chartPaintActive = new Paint();

    float firstPassiveStartPixel = 0;
    float frameStart = 200;
    float frameWidth = 300;

    private int frameColor = Color.rgb(219, 231, 240);
    private int passiveBackgroundColor = Color.argb(0xa0, 245, 248, 249);

    private float xStep;
    private float yStep;

    private float[] chartPoints;

    private void initVariableForDrawing() {
        activeBackgroundPaint.setColor(Color.WHITE); //todo: not only white, but also gray
        activeBackgroundPaint.setStyle(Paint.Style.FILL_AND_STROKE);

        framePaint.setColor(frameColor);
        framePaint.setStyle(Paint.Style.FILL_AND_STROKE);

        chartPaintActive.setStyle(Paint.Style.STROKE);
        chartPaintActive.setStrokeWidth(lineWidth);

        duff.setStyle(Paint.Style.FILL_AND_STROKE);
        duff.setColor(passiveBackgroundColor);
        duff.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.OVERLAY));
    }

    private void onLinesChanged() {
        if (height != 0) {
            yStep = (height - topChartPadding - bottomChartPadding) / maxValue;
        }
    }

    private void drawBackground(Canvas canvas) {
        // draw active background
        canvas.drawRect(0, 0, width, height, activeBackgroundPaint);

        // draw frame
        canvas.drawRect(frameStart, 0, frameStart + frameWidth - 1, frameVerticalBorderWidth, framePaint);
        canvas.drawRect(frameStart, height - frameVerticalBorderWidth, frameStart + frameWidth - 1, height, framePaint);
        canvas.drawRect(frameStart, 0, frameStart + frameHorizontalBorderWidth, height, framePaint);
        canvas.drawRect(frameStart + frameWidth - 1 - frameHorizontalBorderWidth, 0, frameStart + frameWidth - 1, height, framePaint);
    }

    private float topChartPadding = 3;
    private float bottomChartPadding = 3;

    private float lineWidth = 2;

    private float previousX;
    private float previousY;
    private float nextX;
    private float nextY;
    private int chartPointsIndex;
    private List<Integer> chartOrdinate;

    public void drawChart(Canvas canvas) {

        if (xStep == 0 && width != 0) {
            xStep = width / lastDateIndex;
        }

        if (yStep == 0 && height != 0) {
            yStep = (height - topChartPadding - bottomChartPadding) / maxValue;
        }

        for (int lineIndex = 0; lineIndex < linesCount; lineIndex++) {
            if (!lineVisibility[lineIndex]) continue; // do not show muted lines

            chartOrdinate = combinedChart.getOrdinates().get(lineIndex);
            chartPaintActive.setColor(combinedChart.getColors().get(lineIndex));

            chartPointsIndex = 0;

            for (int j = 0; j <= lastDateIndex; j++) {
                if (j == 0) {
                    previousX = 0;
                    previousY = height + topChartPadding - chartOrdinate.get(0) * yStep;
                } else if (j == lastDateIndex) {
                    nextX = width; // todo: or width - 1??
                    nextY = height + topChartPadding - chartOrdinate.get(lastDateIndex) * yStep;

                    chartPoints[chartPointsIndex++] = previousX;
                    chartPoints[chartPointsIndex++] = previousY;
                    chartPoints[chartPointsIndex++] = nextX;
                    chartPoints[chartPointsIndex++] = nextY;
                } else {
                    nextX = j * xStep;
                    nextY = height + topChartPadding - chartOrdinate.get(j) * yStep;

                    chartPoints[chartPointsIndex++] = previousX;
                    chartPoints[chartPointsIndex++] = previousY;
                    chartPoints[chartPointsIndex++] = nextX;
                    chartPoints[chartPointsIndex++] = nextY;

                    previousX = nextX;
                    previousY = nextY;
                }
            }
            canvas.drawLines(chartPoints, chartPaintActive);
        }

        // draw semitransparent background
        canvas.drawRect((int) firstPassiveStartPixel, 0, (int) frameStart - 1, (int) height, duff);
        canvas.drawRect((int) (frameStart + frameWidth), 0, (int) width, (int) height, duff);
    }

    public void setCombinedChart(CombinedChart combinedChart) {
        this.combinedChart = combinedChart;
        this.lastDateIndex = combinedChart.getAbscissa().size() - 1;

        lineVisibility = new boolean[combinedChart.getLineIds().size()];
        Arrays.fill(lineVisibility, true);

        this.maxValue = getMaxVisibleValue();
        this.periodStartDateIndex = 20;
        this.periodEndDateIndex = 50;

        linesCount = combinedChart.getLineIds().size();
        chartPoints = new float[4 * combinedChart.getAbscissa().size()];

        xStep = width / lastDateIndex;

        onLinesChanged();

        invalidate();
    }

    public void setLineVisibility(String lineId, boolean visible) {
        int lineIndex = combinedChart.getLineIds().indexOf(lineId);
        if (lineIndex > -1) {
            lineVisibility[lineIndex] = visible;

            maxValue = getMaxVisibleValue();
            onLinesChanged();
        }

        invalidate();
    }

    public void setPeriodChangedListener(PeriodChangedListener listener) {
        this.periodChangedListener = listener;
    }

    private int getMaxVisibleValue() {
        List<List<Integer>> visibleLines = new ArrayList<>();

        for (int i = 0; i < combinedChart.getLineIds().size(); i++) {
            if (lineVisibility[i]) {
                visibleLines.add(combinedChart.getOrdinates().get(i));
            }
        }

        return (visibleLines.isEmpty()) ? 0 : getMaxValue(visibleLines);
    }

    private int getMaxValue(List<List<Integer>> lists) {
        int max = Integer.MIN_VALUE;

        for (List<Integer> list : lists) {
            max = Math.max(max, Collections.max(list));
        }

        return max;
    }

}
