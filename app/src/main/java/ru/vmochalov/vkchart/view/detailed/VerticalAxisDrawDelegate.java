package ru.vmochalov.vkchart.view.detailed;

import android.animation.ValueAnimator;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import ru.vmochalov.vkchart.R;

/**
 * Created by Vladimir Mochalov on 18.01.2020.
 */
public class VerticalAxisDrawDelegate {

    private final int AXIS_TEXT_MARGIN_PX = 12;
    private final int AXIS_LEVELS_COUNT = 6;
    private final int ANIMATION_DURATION = 500; //ms

    interface Callback {
        void onRedrawRequired();
    }

    private Resources resources;

    private float[] verticalAxesLinesCoords = new float[AXIS_LEVELS_COUNT * 4];
    private float[] firstVerticalLineAnimationCoords = new float[4];

    private String[] verticalLevelValuesAsStrings = new String[AXIS_LEVELS_COUNT];
    private String[] oldVerticalLevelValuesAsStrings = new String[AXIS_LEVELS_COUNT];

    private int bottomAxisMargin;
    private int topAxisMargin;

    private float height;
    private float width;

    private float yDelta;

    private Paint verticalAxisPaint = new Paint();
    private Paint verticalAnimatedAxisPaint = new Paint();

    private Paint verticalLabelsPaint = new Paint();
    private Paint verticalAnimatedLabelsPaint = new Paint();

    private ValueAnimator verticalAxisValueAnimator;
    private boolean axisAnimationDirectionAppearFromBottom;
    private float axisAnimationFraction;


    VerticalAxisDrawDelegate(
            Resources resources,
            float axisStrokeWidth,
            float axisTextSize,
            int bottomAxisMargin,
            int topAxisMargin,
            final Callback callback
    ) {
        this.resources = resources;

        this.bottomAxisMargin = bottomAxisMargin;
        this.topAxisMargin = topAxisMargin;

        verticalAxisPaint.setColor(resources.getColor(R.color.lightThemeAxis));
        verticalAxisPaint.setStrokeWidth(axisStrokeWidth);
        verticalAxisPaint.setAntiAlias(true);
        verticalAxisPaint.setStyle(Paint.Style.FILL_AND_STROKE);

        verticalAnimatedAxisPaint.setStrokeWidth(axisStrokeWidth);
        verticalAnimatedAxisPaint.setAntiAlias(true);
        verticalAnimatedAxisPaint.setStyle(Paint.Style.FILL_AND_STROKE);

        verticalLabelsPaint.setColor(resources.getColor(R.color.lightThemeLabelText));
        verticalLabelsPaint.setTextSize(axisTextSize);
        verticalLabelsPaint.setStrokeWidth(axisStrokeWidth);
        verticalLabelsPaint.setTextAlign(Paint.Align.LEFT);
        verticalLabelsPaint.setAntiAlias(true);
        verticalLabelsPaint.setStyle(Paint.Style.FILL);

        verticalAnimatedLabelsPaint.setTextSize(axisTextSize);
        verticalAnimatedLabelsPaint.setStrokeWidth(axisStrokeWidth);
        verticalAnimatedLabelsPaint.setTextAlign(Paint.Align.LEFT);
        verticalAnimatedLabelsPaint.setAntiAlias(true);
        verticalAnimatedLabelsPaint.setStyle(Paint.Style.FILL);

        verticalAxisValueAnimator = ValueAnimator.ofFloat(1.0f, 0.0f);
        verticalAxisValueAnimator.setDuration(ANIMATION_DURATION);
        verticalAxisValueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                axisAnimationFraction = (float) animation.getAnimatedValue();
                callback.onRedrawRequired();
            }
        });
    }

    void setCanvasSize(float width, float height) {
        this.width = width;
        this.height = height;
    }

    void onHeightChanged(float height) {
        yDelta = (height - bottomAxisMargin - topAxisMargin) / AXIS_LEVELS_COUNT;
    }

    void drawVerticalAxis(Canvas canvas) {
        boolean isAnimationHappening = axisAnimationFraction != 0.0f && axisAnimationFraction != 1.0f;

        if (isAnimationHappening) {
            drawVerticalAxisAnimated(canvas, axisAnimationFraction, axisAnimationDirectionAppearFromBottom, true);
            drawVerticalAxisAnimated(canvas, axisAnimationFraction, !axisAnimationDirectionAppearFromBottom, false);
        } else {
            float verticalYAxisCoord;

            for (int i = 0; i < AXIS_LEVELS_COUNT; i++) {
                verticalYAxisCoord = height - bottomAxisMargin - i * yDelta;
                verticalAxesLinesCoords[4 * i] = 0;
                verticalAxesLinesCoords[4 * i + 1] = verticalYAxisCoord;
                verticalAxesLinesCoords[4 * i + 2] = width;
                verticalAxesLinesCoords[4 * i + 3] = verticalYAxisCoord;
            }

            canvas.drawLines(verticalAxesLinesCoords, verticalAxisPaint);
        }
    }

    private void drawVerticalAxisAnimated(Canvas canvas, float fraction, boolean appearFromBottom, boolean appearing) {

        int alpha = appearing ? Math.min((int) (0xff * ((1 - fraction) * (1 - fraction))), 0xff) : Math.min((int) (0xff * (fraction * fraction)), 0xff);

        int color = Color.argb(alpha,
                Color.red(verticalAxisPaint.getColor()),
                Color.green(verticalAxisPaint.getColor()),
                Color.blue(verticalAxisPaint.getColor())
        );

        verticalAnimatedAxisPaint.setColor(color);

        float animationFraction = appearing ? (1 - fraction) : fraction;

        // do not animate last line
        float verticalYAxisCoord = height - bottomAxisMargin;

        firstVerticalLineAnimationCoords[0] = 0;
        firstVerticalLineAnimationCoords[1] = verticalYAxisCoord;
        firstVerticalLineAnimationCoords[2] = width;
        firstVerticalLineAnimationCoords[3] = verticalYAxisCoord;

        for (int i = 1; i < AXIS_LEVELS_COUNT; i++) {
            if (appearFromBottom) {
                verticalYAxisCoord = height - bottomAxisMargin - (i - 1) * yDelta - yDelta * animationFraction;
            } else {
                verticalYAxisCoord = height - bottomAxisMargin - AXIS_LEVELS_COUNT * yDelta + (i - 1) * yDelta + yDelta * animationFraction;
            }
            verticalAxesLinesCoords[4 * i] = 0;
            verticalAxesLinesCoords[4 * i + 1] = verticalYAxisCoord;
            verticalAxesLinesCoords[4 * i + 2] = width;
            verticalAxesLinesCoords[4 * i + 3] = verticalYAxisCoord;
        }

        canvas.drawLines(verticalAxesLinesCoords, verticalAnimatedAxisPaint);
        canvas.drawLines(firstVerticalLineAnimationCoords, verticalAxisPaint);
    }

    void drawVerticalLabels(Canvas canvas) {
        boolean animationIsHappening = axisAnimationFraction != 0.0f && axisAnimationFraction != 1.0f;

        if (animationIsHappening) {
            drawVerticalLabelsAnimated(canvas, axisAnimationFraction, axisAnimationDirectionAppearFromBottom, true);
            drawVerticalLabelsAnimated(canvas, axisAnimationFraction, !axisAnimationDirectionAppearFromBottom, false);

        } else {
            for (int i = 0; i < AXIS_LEVELS_COUNT; i++) {
                float verticalYAxisCoord = height - bottomAxisMargin - i * yDelta;
                canvas.drawText(oldVerticalLevelValuesAsStrings[i], 0, verticalYAxisCoord - AXIS_TEXT_MARGIN_PX, verticalLabelsPaint);
            }
        }
    }

    private void drawVerticalLabelsAnimated(Canvas canvas, float fraction, boolean appearFromBottom, boolean appearing) {
        int alpha = appearing ? Math.min((int) (0xff * ((1 - fraction) * (1 - fraction))), 0xff) : Math.min((int) (0xff * (fraction * fraction)), 0xff);

        int color = Color.argb(alpha,
                Color.red(verticalLabelsPaint.getColor()),
                Color.green(verticalLabelsPaint.getColor()),
                Color.blue(verticalLabelsPaint.getColor())
        );

        verticalAnimatedLabelsPaint.setColor(color);

        float animationFraction = appearing ? (1 - fraction) : fraction;

        String[] labelsToUse = appearing ? verticalLevelValuesAsStrings : oldVerticalLevelValuesAsStrings;

        float verticalYAxisCoordAnimation;

        for (int i = 1; i < AXIS_LEVELS_COUNT; i++) {
            if (appearFromBottom) {
                verticalYAxisCoordAnimation = height - bottomAxisMargin - (i - 1) * yDelta - yDelta * animationFraction;
                canvas.drawText(labelsToUse[i], 0, verticalYAxisCoordAnimation - AXIS_TEXT_MARGIN_PX, verticalAnimatedLabelsPaint);
            } else {
                verticalYAxisCoordAnimation = height - bottomAxisMargin - (AXIS_LEVELS_COUNT) * yDelta + (AXIS_LEVELS_COUNT - i - 1) * yDelta + yDelta * animationFraction;
                canvas.drawText(labelsToUse[i], 0, verticalYAxisCoordAnimation - AXIS_TEXT_MARGIN_PX, verticalAnimatedLabelsPaint);
            }
        }

        verticalYAxisCoordAnimation = height - bottomAxisMargin;
        canvas.drawText(labelsToUse[0], 0, verticalYAxisCoordAnimation - AXIS_TEXT_MARGIN_PX, verticalLabelsPaint);
    }

    void onLinesVisibilityUpdated(boolean areLinesVisible, int newMaxVisibleValue) {
        int verticalLevelDelta = (newMaxVisibleValue / AXIS_LEVELS_COUNT);

        if (!areLinesVisible || verticalLevelDelta == 0)
            verticalLevelDelta = 1; // in case user is confused

        if (oldVerticalLevelValuesAsStrings[0] == null) {
            for (int i = 0; i < AXIS_LEVELS_COUNT; i++) {
                oldVerticalLevelValuesAsStrings[i] = Integer.toString(verticalLevelDelta * i);
            }
        }
        //calculationg background levels
        for (int i = 0; i < AXIS_LEVELS_COUNT; i++) {
            verticalLevelValuesAsStrings[i] = Integer.toString(verticalLevelDelta * i);
        }
    }

    void onNightModeChanged(boolean nightModeOn) {
        int verticalAxisColor = resources.getColor(nightModeOn ? R.color.darkThemeAxis : R.color.lightThemeAxis);
        int labelsColor = resources.getColor(nightModeOn ? R.color.darkThemeLabelText : R.color.lightThemeLabelText);

        verticalAxisPaint.setColor(verticalAxisColor);
        verticalLabelsPaint.setColor(labelsColor);
    }

    Paint getVerticalAxisPaint() {
        return verticalAxisPaint;
    }

    void onMaxVisibleValueAnimationEnd() {
        oldVerticalLevelValuesAsStrings = verticalLevelValuesAsStrings.clone();
    }

    void animateVerticalAxis(boolean maxVisibleValueDecreased) {
        if (verticalAxisValueAnimator != null) {
            verticalAxisValueAnimator.pause();
        }

        axisAnimationDirectionAppearFromBottom = maxVisibleValueDecreased;
        verticalAxisValueAnimator.start();
    }

}
