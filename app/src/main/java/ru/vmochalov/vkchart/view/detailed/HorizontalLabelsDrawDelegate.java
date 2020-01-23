package ru.vmochalov.vkchart.view.detailed;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import java.util.ArrayList;
import java.util.List;

import ru.vmochalov.vkchart.R;

import static ru.vmochalov.vkchart.utils.CalculationUtil.getPowOfTwo;

/**
 * Created by Vladimir Mochalov on 19.01.2020.
 */
class HorizontalLabelsDrawDelegate {

    interface Callback {
        void onRedrawRequired();
    }

    private final int AXES_TEXT_SIZE_PX = 20;
    private final int FIRST_DATE_INDEX = 0;
    private final int LABELS_MAX_DISTANCE_LIMIT_PX = 200;

    private Resources resources;

    private boolean labelsAlphaAnimationInProgress;
    private float horizontalLabelsAlpha = 1;

    private float x0;
    private float xStep;

    private int lastDateIndex;
    private int firstVisiblePointIndex;
    private int lastVisiblePointIndex;

    private float horizontalLabelY;

    private int currentLabelsScale;

    private boolean[] labelsVisibility;

    private List<String> chartAbscissaLabels;

    private List<Integer> fadePointIndexes = new ArrayList<>();

    private ValueAnimator horizontalLabelsAnimator = null;

    private Callback callback;

    private Paint labelPaint = new Paint();
    private Paint labelPaintAnimation = new Paint();

    HorizontalLabelsDrawDelegate(Resources resources, float axisTextSize, float axisStrokeWidth, Callback callback) {
        this.resources = resources;
        this.callback = callback;

        labelPaint.setTextAlign(Paint.Align.CENTER);
        labelPaint.setColor(resources.getColor(R.color.lightThemeLabelText));
        labelPaint.setTextSize(axisTextSize);
        labelPaint.setStrokeWidth(axisStrokeWidth);
        labelPaint.setAntiAlias(true);
        labelPaint.setStyle(Paint.Style.FILL);

        labelPaintAnimation.setTextAlign(Paint.Align.CENTER);
        labelPaintAnimation.setColor(resources.getColor(R.color.lightThemeLabelText));
        labelPaintAnimation.setTextSize(axisTextSize);
        labelPaintAnimation.setStrokeWidth(axisStrokeWidth);
        labelPaintAnimation.setAntiAlias(true);
        labelPaintAnimation.setStyle(Paint.Style.FILL);

    }

    void onChartInited(List<String> labels) {
        chartAbscissaLabels = labels;
        labelsVisibility = new boolean[labels.size()];
    }

    void onDrawingParamsChanged(int lastDateIndex, float x0, float xStep, int firstVisiblePointIndex, int lastVisiblePointIndex) {
        this.lastDateIndex = lastDateIndex;
        this.x0 = x0;
        this.xStep = xStep;
        this.firstVisiblePointIndex = firstVisiblePointIndex;
        this.lastVisiblePointIndex = lastVisiblePointIndex;
    }

    void onNightModeChanged(boolean nightModeOn) {
        int labelsColor = resources.getColor(nightModeOn ? R.color.darkThemeLabelText : R.color.lightThemeLabelText);

        labelPaint.setColor(labelsColor);
    }

    void onHeightChanged(float height) {
        horizontalLabelY = height - AXES_TEXT_SIZE_PX / 2;
    }

    void drawHorizontalLabels(Canvas canvas) {

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
                if (isPointIndexValid(i) && i != FIRST_DATE_INDEX && i != lastDateIndex) {
                    canvas.drawText(
                            chartAbscissaLabels.get(i),
                            x0 + xStep * i,
                            horizontalLabelY,
                            labelPaintAnimation
                    );
                }
            }
        }

        for (int i = firstVisiblePointIndex; i <= lastVisiblePointIndex; i++) {
            if (labelsVisibility[i] && i != FIRST_DATE_INDEX && i != lastDateIndex) {
                if (!animationInProgress || !fadePointIndexes.contains(i)) {
                    canvas.drawText(
                            chartAbscissaLabels.get(i),
                            x0 + xStep * i,
                            horizontalLabelY,
                            labelPaint
                    );
                }
            }
        }
    }

    void updatedHorizontalLabelsScale() {
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

                callback.onRedrawRequired();
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

    // 0 - all are visible, 1 - every second, 3 - every 4th, 4 - every 8th and so on
    private void setScaleForHorizontalLabels(int scale) {
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
        // Find out, if two points overlap. If not, then this is the preffered scale.
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

    private boolean areNeightborPointsTooClose(int firstPointIndex, int secondPointIndex) {
        if (isPointIndexValid(firstPointIndex) && isPointIndexValid(secondPointIndex)) {

            String labelOne = chartAbscissaLabels.get(firstPointIndex);
            float labelOneWidth = labelPaint.measureText(labelOne);
            float labelOneStartX = x0 + xStep * firstPointIndex - labelOneWidth / 2;
            float labelOneEndX = labelOneStartX + labelOneWidth;

            String labelTwo = chartAbscissaLabels.get(secondPointIndex);
            float labelTwoWidth = labelPaint.measureText(labelTwo);
            float labelTwoStartX = x0 + xStep * secondPointIndex - labelTwoWidth / 2;

            return labelOneEndX - labelTwoStartX > -30;

        } else {
            return false;
        }
    }

    private boolean areNeighbourPointsTooFar(int firstPointIndex, int secondPointIndex) {
        if (isPointIndexValid(firstPointIndex) && isPointIndexValid(secondPointIndex)) {

            String labelOne = chartAbscissaLabels.get(firstPointIndex);
            float labelOneWidth = labelPaint.measureText(labelOne);
            float labelOneStartX = x0 + xStep * firstPointIndex - labelOneWidth / 2;
            float labelOneEndX = labelOneStartX + labelOneWidth;

            String labelTwo = chartAbscissaLabels.get(secondPointIndex);
            float labelTwoWidth = labelPaint.measureText(labelTwo);
            float labelTwoStartX = x0 + xStep * secondPointIndex - labelTwoWidth / 2;

            return labelTwoStartX - labelOneEndX > LABELS_MAX_DISTANCE_LIMIT_PX;

        } else {
            return false;
        }
    }

    private boolean isPointIndexValid(int index) {
        return index >= FIRST_DATE_INDEX && index <= lastDateIndex;
    }

    int getClosestPointIndex(float x) {
        int result = Math.round((x - x0) / xStep);
        if (result < FIRST_DATE_INDEX) result = FIRST_DATE_INDEX;
        if (result > lastDateIndex) result = lastDateIndex;
        return result;
    }

}
