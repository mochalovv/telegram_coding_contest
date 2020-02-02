package ru.vmochalov.vkchart.chart.view.primary;

import android.view.MotionEvent;
import android.view.View;

import ru.vmochalov.vkchart.chart.view.common.OnChartClickedListener;
import ru.vmochalov.vkchart.chart.view.common.delegates.ChartDrawDelegate;
import ru.vmochalov.vkchart.chart.view.primary.delegates.HorizontalLabelsDrawDelegate;

/**
 * Created by Vladimir Mochalov on 25.01.2020.
 */
public class PrimaryChartOnTouchListener implements View.OnTouchListener {

    private float initialX;
    private float initialY;

    private boolean isHorizontalGesture;

    private HorizontalLabelsDrawDelegate horizontalLabelsDrawDelegate;
    private ChartDrawDelegate chartDrawDelegate;
    private OnChartClickedListener onChartClickedListener;

    PrimaryChartOnTouchListener(
            HorizontalLabelsDrawDelegate horizontalLabelsDrawDelegate,
            ChartDrawDelegate chartDrawDelegate,
            OnChartClickedListener onChartClickedListener
    ) {
        this.horizontalLabelsDrawDelegate = horizontalLabelsDrawDelegate;
        this.chartDrawDelegate = chartDrawDelegate;
        this.onChartClickedListener = onChartClickedListener;
    }

    @Override
    public boolean onTouch(View chartView, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            initialX = event.getX();
            initialY = event.getY();

            int selectedPointIndex = horizontalLabelsDrawDelegate.getClosestPointIndex(event.getX());

            chartDrawDelegate.setSelectedPointIndex(selectedPointIndex);

            onChartClickedListener.onTouch(
                    event.getX(),
                    selectedPointIndex,
                    chartDrawDelegate.areLinesVisible()
            );
        } else if (event.getAction() == MotionEvent.ACTION_UP) {
            chartDrawDelegate.setSelectedPointIndex(-1);

            isHorizontalGesture = false;

            onChartClickedListener.onButtonUp();
            onChartClickedListener.onGestureDirectionChanged(isHorizontalGesture);

        } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
            int lastSelectedPointIndex = horizontalLabelsDrawDelegate.getClosestPointIndex(event.getX());

            chartDrawDelegate.setSelectedPointIndex(lastSelectedPointIndex);

            boolean isHorizontal = isHorizontalMovement(event.getX(), event.getY());

            if (isHorizontal != isHorizontalGesture) {
                isHorizontalGesture = isHorizontal;

                onChartClickedListener.onGestureDirectionChanged(isHorizontalGesture);
            }

            onChartClickedListener.onTouch(
                    event.getX(),
                    lastSelectedPointIndex,
                    chartDrawDelegate.areLinesVisible()
            );

        } else {
            isHorizontalGesture = false;
            chartDrawDelegate.setSelectedPointIndex(-1);

            onChartClickedListener.onGestureDirectionChanged(isHorizontalGesture);
        }

        chartView.invalidate();

        return true;
    }

    private boolean isHorizontalMovement(float updatedX, float updatedY) {
        if (initialX == updatedX) return false;

        double tg = (updatedY - initialY) / (updatedX - initialX);

        return Math.abs(tg) < 1;
    }

}
