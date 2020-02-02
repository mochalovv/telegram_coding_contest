package ru.vmochalov.vkchart.chart.view.common;

/**
 * Created by Vladimir Mochalov on 18.01.2020.
 */
public interface OnRangeChangedListener {

    // 0.0 <= x <= 1.0
    void onVisibleRangeChanged(double periodStart, double periodEnd);

    void onDragDirectionChanged(boolean horizontal);
}
