package ru.vmochalov.vkchart.view;

import android.animation.ValueAnimator;
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

import ru.vmochalov.vkchart.dto.Chart;

class ChartNavigationView extends View {

    private float width;
    private float height;

    private static final int firstDateIndex = 0;
    private int lastDateIndex;
    private int periodStartDateIndex;
    private int periodEndDateIndex;

    private int maxValue;

    private Chart chart;

    private boolean[] lineVisibility;
    private int[] lineAlpha;

    // styleable attributes
    private float lineStrokeWidth = 5;
    private float frameHorizontalBorderWidth = 10;
    private float frameVerticalBorderWidth = 4;

    private int linesCount;

    //todo: obtain from resources
    private int prefferedHeight = 75;

    private PeriodChangedListener periodChangedListener;

    public interface PeriodChangedListener {
        //  0.0 <= x <= 1.0
        void onPeriodLengthChanged(double periodStart, double periodEnd, boolean startIsStable); // only startIsStable - end is dragged

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

    private float borderTouchArea = 20; //px
    private float minimumFrameWidth = 40; // px

    private void initTouchListener() {
        setOnTouchListener(
                new OnTouchListener() {

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
//                                    } else if (touchType == TouchType.LEFT_BORDER_TOUCH || touchType == TouchType.RIGHT_BORDER_TOUCH) {
//                                        periodChangedListener.onPeriodLengthChanged(frameStartInPercent, frameEndInPercent);
                                    } else if (touchType == TouchType.LEFT_BORDER_TOUCH) {
                                        periodChangedListener.onPeriodLengthChanged(frameStartInPercent, frameEndInPercent, false);
                                    } else if (touchType == TouchType.RIGHT_BORDER_TOUCH) {
                                        periodChangedListener.onPeriodLengthChanged(frameStartInPercent, frameEndInPercent, true);
                                    }
                                }
                            }

                            if (dx != 0) {
                                ChartNavigationView.this.invalidate();
                            }
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

                periodChangedListener.onPeriodLengthChanged(frameStartInPercent, frameEndInPercent, true);

                initialValueIsSent = true;

            }
        }
    }

    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int measuredHeight = MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.EXACTLY ? MeasureSpec.getSize(heightMeasureSpec) : prefferedHeight;
        int measuredWidth = MeasureSpec.getSize(widthMeasureSpec);

        measuredHeight = Math.max(measuredHeight, getSuggestedMinimumHeight());
        measuredWidth = Math.max(measuredWidth, getSuggestedMinimumWidth());

        setMeasuredDimension(measuredWidth, measuredHeight);
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

    private int activeBackgroundColor = Color.WHITE;
    private int activeBackgroundColorNightMode = Color.rgb(29, 39, 51);
    private int frameColor = Color.rgb(219, 231, 240);
    private int frameColorNightMode = Color.rgb(43, 66, 86);
    private int passiveBackgroundColor = Color.argb(0xa0, 245, 248, 249);
    private int passiveBackgroundColorNightMode = Color.argb(0xa0, 25, 33, 46);

    private float xStep;
    private float yStep;

    private float[] chartPoints;

    private void initVariableForDrawing() {
        activeBackgroundPaint.setColor(activeBackgroundColor); //todo: not only white, but also gray
        activeBackgroundPaint.setStyle(Paint.Style.FILL_AND_STROKE);

        framePaint.setColor(frameColor);
        framePaint.setStyle(Paint.Style.FILL_AND_STROKE);

        chartPaintActive.setStyle(Paint.Style.STROKE);
        chartPaintActive.setStrokeWidth(lineWidth);

        duff.setStyle(Paint.Style.FILL_AND_STROKE);
        duff.setColor(passiveBackgroundColor);
        duff.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.OVERLAY));
    }

    private ValueAnimator maxValueAnimator;

    private static int ANIMATION_DURATION = 300;

    private void onLinesChanged() {
        int newMaxValue = getMaxVisibleValue();

        if (newMaxValue != maxValue) {
            if (maxValueAnimator != null) {
                maxValueAnimator.pause();
            }

            maxValueAnimator = ValueAnimator.ofInt(maxValue, newMaxValue);
            maxValueAnimator.setDuration(ANIMATION_DURATION);
            maxValueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    int value = (int) animation.getAnimatedValue();

                    updateVerticalDrawingParams(value);
                    invalidate();
                }
            });
            maxValueAnimator.start();
        }
    }

    private void updateVerticalDrawingParams(int maxValue) {
        if (height != 0) {
            yStep = (height - topChartPadding - bottomChartPadding) / maxValue;
        }
        this.maxValue = maxValue;
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
            if (lineAlpha[lineIndex] == 0) continue; // do not show muted lines

            chartOrdinate = chart.getOrdinates().get(lineIndex);

            int tempColor = chart.getColors().get(lineIndex);

            chartPaintActive.setColor(
                    Color.argb(lineAlpha[lineIndex], Color.red(tempColor), Color.green(tempColor), Color.blue(tempColor))
            );

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

    public void setChart(Chart chart) {
        this.chart = chart;
        this.lastDateIndex = chart.getAbscissa().size() - 1;

        lineVisibility = new boolean[chart.getLineIds().size()];
        Arrays.fill(lineVisibility, true);

        lineAlpha = new int[chart.getLineIds().size()];
        Arrays.fill(lineAlpha, 0xff);

        this.periodStartDateIndex = 20;
        this.periodEndDateIndex = 50;

        linesCount = chart.getLineIds().size();
        chartPoints = new float[4 * chart.getAbscissa().size()];

        xStep = width / lastDateIndex;

        onLinesChanged();

        invalidate();
    }

    private ValueAnimator alphaValueAnimator;

    public void setLineVisibility(String lineId, boolean visible) {
        final int lineIndex = chart.getLineIds().indexOf(lineId);
        if (lineIndex > -1) {

            if (lineVisibility[lineIndex] != visible) {
                if (alphaValueAnimator != null) {
                    alphaValueAnimator.end();
                }

                alphaValueAnimator = ValueAnimator.ofInt(visible ? 0 : 0xff, visible ? 0xff : 0);
                alphaValueAnimator.setDuration(ANIMATION_DURATION);
                alphaValueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        int value = (int) animation.getAnimatedValue();
                        lineAlpha[lineIndex] = value;
                        invalidate();
                    }
                });
                alphaValueAnimator.start();

            }

            lineVisibility[lineIndex] = visible;

            onLinesChanged();
        }

        invalidate();
    }

    public void setPeriodChangedListener(PeriodChangedListener listener) {
        this.periodChangedListener = listener;
    }

    private int getMaxVisibleValue() {
        List<List<Integer>> visibleLines = new ArrayList<>();

        for (int i = 0; i < chart.getLineIds().size(); i++) {
            if (lineVisibility[i]) {
                visibleLines.add(chart.getOrdinates().get(i));
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

    public void setNightMode(boolean nightModeOn) {
        activeBackgroundPaint.setColor(nightModeOn ? activeBackgroundColorNightMode : activeBackgroundColor);
        duff.setColor(nightModeOn ? passiveBackgroundColorNightMode : passiveBackgroundColor);
        framePaint.setColor(nightModeOn ? frameColorNightMode : frameColor);

        invalidate();
    }

}
