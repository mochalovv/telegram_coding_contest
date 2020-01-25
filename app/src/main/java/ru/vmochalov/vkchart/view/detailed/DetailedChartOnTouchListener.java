package ru.vmochalov.vkchart.view.detailed;

import android.view.MotionEvent;
import android.view.View;

/**
 * Created by Vladimir Mochalov on 25.01.2020.
 */
public class DetailedChartOnTouchListener implements View.OnTouchListener {

    private float initialX;
    private float initialY;

    private boolean isHorizontalMovement;

    private HorizontalLabelsDrawDelegate horizontalLabelsDrawDelegate;
    private ChartDrawDelegate chartDrawDelegate;
    private DetailedChartView.OnChartClickedListener onChartClickedListener;

    DetailedChartOnTouchListener(
            HorizontalLabelsDrawDelegate horizontalLabelsDrawDelegate,
            ChartDrawDelegate chartDrawDelegate,
            DetailedChartView.OnChartClickedListener onChartClickedListener
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
                    chartDrawDelegate.getVisibleSelectedValues()
            );
        } else if (event.getAction() == MotionEvent.ACTION_UP) {
            chartDrawDelegate.setSelectedPointIndex(-1);

            isHorizontalMovement = false;

            onChartClickedListener.onButtonUp();
            onChartClickedListener.onMovementDirectionChanged(isHorizontalMovement);

        } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
            int lastSelectedPointIndex = horizontalLabelsDrawDelegate.getClosestPointIndex(event.getX());

            chartDrawDelegate.setSelectedPointIndex(lastSelectedPointIndex);

            boolean isHorizontal = isHorizontalMovement(event.getX(), event.getY());

            if (isHorizontal != isHorizontalMovement) {
                isHorizontalMovement = isHorizontal;

                onChartClickedListener.onMovementDirectionChanged(isHorizontalMovement);
            }

            onChartClickedListener.onTouch(
                    event.getX(),
                    lastSelectedPointIndex,
                    chartDrawDelegate.getVisibleSelectedValues()
            );

        } else {
            isHorizontalMovement = false;
            chartDrawDelegate.setSelectedPointIndex(-1);

            onChartClickedListener.onMovementDirectionChanged(isHorizontalMovement);
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