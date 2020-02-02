package ru.vmochalov.vkchart.chart.view.common;

/**
 * Created by Vladimir Mochalov on 02.02.2020.
 */
public interface OnChartClickedListener {
    void onTouch(float x, int pointIndex, boolean areLinesVisible);

    void onButtonUp();

    void onGestureDirectionChanged(boolean isHorizontal);
}
