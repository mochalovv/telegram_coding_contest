package ru.vmochalov.vkchart.chart.view.common;

/**
 * Created by Vladimir Mochalov on 18.01.2020.
 */
public interface PeriodChangedListener {

    // 0.0 <= x <= 1.0
    void onPeriodChangedMoved(double periodStart, double periodEnd);

    void onPeriodModifyFinished();

    void onDragDirectionChanged(boolean horizontal);
}
