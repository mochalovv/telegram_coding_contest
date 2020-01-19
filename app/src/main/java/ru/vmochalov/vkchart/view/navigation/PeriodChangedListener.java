package ru.vmochalov.vkchart.view.navigation;

/**
 * Created by Vladimir Mochalov on 18.01.2020.
 */
public interface PeriodChangedListener {
    //  0.0 <= x <= 1.0
    void onPeriodLengthChanged(double periodStart, double periodEnd, boolean startIsStable); // only startIsStable - end is dragged

    // 0.0 <= x <= 1.0
    void onPeriodMoved(double periodStart, double periodEnd);

    void onPeriodModifyFinished();

    void onDragDirectionChanged(boolean horizontal);
}
