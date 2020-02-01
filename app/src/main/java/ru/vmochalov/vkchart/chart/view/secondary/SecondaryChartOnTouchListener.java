package ru.vmochalov.vkchart.chart.view.secondary;

import android.view.MotionEvent;
import android.view.View;

import ru.vmochalov.vkchart.chart.view.common.PeriodChangedListener;

import static ru.vmochalov.vkchart.utils.CalculationUtil.isHorizontalGesture;

/**
 * Created by Vladimir Mochalov on 18.01.2020.
 */
public class SecondaryChartOnTouchListener implements View.OnTouchListener {

    private static final float BORDER_TOUCH_AREA_PX = 30;
    private static final float MIN_FRAME_WIDTH_PX = 40;

    private enum TouchType {
        LEFT_BORDER_TOUCH, FRAME_TOUCH, RIGHT_BORDER_TOUCH, UNHANDLED_TOUCH
    }

    private TouchType touchType;

    private float chartWidth;

    private float initialTouchX;
    private float initialTouchY;
    private float previousX;

    private boolean isDragHorizontal;

    private float frameStart;
    private float frameWidth;

    private PeriodChangedListener periodChangedListener;
    private SecondaryChartView.FrameUpdatedListener frameUpdatedListener;

    public SecondaryChartOnTouchListener(
            float frameStart,
            float frameWidth,
            SecondaryChartView.FrameUpdatedListener frameUpdatedListener
    ) {
        this.frameStart = frameStart;
        this.frameWidth = frameWidth;
        this.frameUpdatedListener = frameUpdatedListener;
    }

    public void setPeriodChangedListener(PeriodChangedListener listener) {
        periodChangedListener = listener;
    }

    @Override
    public boolean onTouch(View chartNavigationView, MotionEvent event) {

        if (chartWidth == 0) {
            chartWidth = chartNavigationView.getWidth();
        }

        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            initialTouchX = event.getX();
            initialTouchY = event.getY();

            previousX = event.getX();
            touchType = getTouchType(event.getX());

        } else if (event.getAction() == MotionEvent.ACTION_UP) {
            isDragHorizontal = false;

            dispatchActionUpEvent();
        } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
            if (touchType != TouchType.UNHANDLED_TOUCH) {
                float x = getActualX(event);
                float deltaX = getActualDeltaX(x);
                previousX = x;

                updatedFrameParameters(deltaX);
                frameUpdatedListener.onFrameUpdated(frameStart, frameWidth);

                dispatchActionMoveEvent(x, event.getY(), deltaX);

                if (deltaX != 0) {
                    chartNavigationView.invalidate();
                }
            }
        } else {
            isDragHorizontal = false;

            if (periodChangedListener != null) {
                periodChangedListener.onDragDirectionChanged(isDragHorizontal);
            }

            return false;
        }

        return true;
    }

    private TouchType getTouchType(float x) {
        TouchType touchType;

        if (x >= frameStart - BORDER_TOUCH_AREA_PX && x <= frameStart + BORDER_TOUCH_AREA_PX) {
            touchType = TouchType.LEFT_BORDER_TOUCH;
        } else if (x >= frameStart + frameWidth - BORDER_TOUCH_AREA_PX && x <= frameStart + frameWidth + BORDER_TOUCH_AREA_PX) {
            touchType = TouchType.RIGHT_BORDER_TOUCH;
        } else if (x >= frameStart && x <= frameStart + frameWidth) {
            touchType = TouchType.FRAME_TOUCH;
        } else {
            touchType = TouchType.UNHANDLED_TOUCH;
        }

        return touchType;
    }

    private void dispatchActionUpEvent() {
        if ((touchType == TouchType.LEFT_BORDER_TOUCH ||
                touchType == TouchType.FRAME_TOUCH ||
                touchType == TouchType.RIGHT_BORDER_TOUCH) &&
                periodChangedListener != null
        ) {
            periodChangedListener.onPeriodModifyFinished();
            periodChangedListener.onDragDirectionChanged(isDragHorizontal);
        }

        touchType = null;
    }

    private void dispatchActionMoveEvent(float x, float y, float deltaX) {
        double frameStartInPercent = frameStart / chartWidth;
        double frameEndInPercent = (frameStart + frameWidth) / chartWidth;

        if (frameStartInPercent < 0) {
            frameStartInPercent = 0;
        }

        if (frameEndInPercent > 1) {
            frameEndInPercent = 1;
        }

        if (periodChangedListener != null & deltaX != 0 && touchType != TouchType.UNHANDLED_TOUCH) {
            periodChangedListener.onPeriodChangedMoved(frameStartInPercent, frameEndInPercent);
        }
        boolean isHorizontal = isHorizontalGesture(initialTouchX, initialTouchY, x, y);

        if (isHorizontal != isDragHorizontal) {
            isDragHorizontal = isHorizontal;
            periodChangedListener.onDragDirectionChanged(isDragHorizontal);
        }
    }

    private float getActualX(MotionEvent event) {
        float x;

        //do not handle points outside the view
        if (event.getX() < 0) {
            x = 0;
        } else if (event.getX() > chartWidth) {
            x = chartWidth;
        } else {
            x = event.getX();
        }

        return x;
    }

    private float getActualDeltaX(float actualX) {
        float dx = actualX - previousX;

        // do not allow frame move outside the view
        if ((touchType == TouchType.FRAME_TOUCH || touchType == TouchType.LEFT_BORDER_TOUCH) && frameStart + dx < 0) {
            dx = -frameStart;
        } else if ((touchType == TouchType.FRAME_TOUCH || touchType == TouchType.RIGHT_BORDER_TOUCH) && frameStart + frameWidth + dx > chartWidth) {
            dx = chartWidth - frameStart - frameWidth;
        }

        //do not allow frame be "left side right"
        if (touchType == TouchType.LEFT_BORDER_TOUCH) {
            if (frameStart + dx + MIN_FRAME_WIDTH_PX > frameStart + frameWidth) {
                dx = frameWidth - MIN_FRAME_WIDTH_PX;
            }
        } else if (touchType == TouchType.RIGHT_BORDER_TOUCH) {
            if (frameStart + frameWidth + dx < frameStart + MIN_FRAME_WIDTH_PX) {
                dx = MIN_FRAME_WIDTH_PX - frameWidth;
            }
        }

        return dx;
    }

    private void updatedFrameParameters(float deltaX) {
        // consume deltaX according to current event
        if (touchType == TouchType.FRAME_TOUCH) {
            frameStart += deltaX;
        } else if (touchType == TouchType.LEFT_BORDER_TOUCH) {
            frameStart += deltaX;
            frameWidth -= deltaX;
        } else if (touchType == TouchType.RIGHT_BORDER_TOUCH) {
            frameWidth += deltaX;
        }
    }

}
