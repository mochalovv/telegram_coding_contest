package ru.vmochalov.vkchart.chart.view;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;

import ru.vmochalov.vkchart.R;
import ru.vmochalov.vkchart.chart.data.Chart;
import ru.vmochalov.vkchart.chart.view.common.GestureDirectionListener;
import ru.vmochalov.vkchart.chart.view.common.OnChartClickedListener;
import ru.vmochalov.vkchart.chart.view.common.OnRangeChangedListener;
import ru.vmochalov.vkchart.chart.view.primary.PrimaryChartView;
import ru.vmochalov.vkchart.chart.view.secondary.SecondaryChartView;

/**
 * Created by Vladimir Mochalov on 17.03.2019.
 */
public class ChartView extends LinearLayout {

    private PrimaryChartView primaryChartView;
    private SecondaryChartView secondaryChartView;
    private SelectedPointInfoView selectedPointInfoView;

    private GestureDirectionListener gestureDirectionListener;

    private OnRangeChangedListener onRangeChangedListener = new OnRangeChangedListener() {
        @Override
        public void onVisibleRangeChanged(double periodStart, double periodEnd) {
            primaryChartView.onVisibleRangeChanged(periodStart, periodEnd);
        }

        @Override
        public void onDragDirectionChanged(boolean horizontal) {
            if (gestureDirectionListener != null) {
                gestureDirectionListener.onGestureDirectionChanged(horizontal);
            }
        }
    };

    private OnChartClickedListener onChartClickedListener = new OnChartClickedListener() {

        @Override
        public void onTouch(float x, int pointIndex, boolean areLinesVisible) {
            selectedPointInfoView.onSelectedPointChanged(pointIndex);

            if (areLinesVisible) {
                selectedPointInfoView.onTouch(x, primaryChartView.getWidth());
                selectedPointInfoView.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public void onButtonUp() {
            selectedPointInfoView.setVisibility(View.GONE);
        }

        @Override
        public void onGestureDirectionChanged(boolean isHorizontal) {
            if (gestureDirectionListener != null) {
                gestureDirectionListener.onGestureDirectionChanged(isHorizontal);
            }

            if (!isHorizontal) {
                selectedPointInfoView.setVisibility(View.GONE);
            }
        }
    };

    public ChartView(Context context) {
        super(context);

        initView();
    }

    public ChartView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);

        initView();
    }

    private void initView() {
        LayoutInflater.from(getContext()).inflate(R.layout.layout_chart_view, this);

        setOrientation(VERTICAL);

        int padding = getResources().getDimensionPixelSize(R.dimen.chartPadding);
        setPadding(padding, padding, padding, padding);

        onViewInflated();
    }

    private void onViewInflated() {
        primaryChartView = findViewById(R.id.chart);
        secondaryChartView = findViewById(R.id.chartNavigation);
        selectedPointInfoView = findViewById(R.id.selectedPointInfoView);

        secondaryChartView.setOnRangeChangedListener(onRangeChangedListener);
    }

    public void setChart(Chart chart) {
        primaryChartView.setChart(chart);
        secondaryChartView.setChart(chart);
        selectedPointInfoView.setChart(chart);

        primaryChartView.setOnChartClickedListener(onChartClickedListener);

        for (int i = 0; i < chart.getLineIds().size(); i++) {
            addView(createCheckBoxForLine(chart, i));
        }
    }

    private CheckBox createCheckBoxForLine(Chart chart, final int lineIndex) {
        String lineId = chart.getLineIds().get(lineIndex);
        String lineLabel = chart.getLabels().get(lineIndex);
        int color = chart.getColors().get(lineIndex);

        CheckBox checkBox = new CheckBox(getContext());
        checkBox.setText(lineLabel);
        checkBox.setTag(lineId);
        checkBox.setButtonTintList(ColorStateList.valueOf(color));
        checkBox.setChecked(true);

        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                String lineId = buttonView.getTag().toString();

                primaryChartView.setLineVisibility(lineId, isChecked);
                secondaryChartView.setLineVisibility(lineId, isChecked);
                selectedPointInfoView.onLineVisiblityChanged(lineId, isChecked);
            }
        });

        return checkBox;
    }

    public void onNightModeChanged(boolean nightModeOn) {
        primaryChartView.onNightModeChanged(nightModeOn);
        secondaryChartView.onNightModeChanged(nightModeOn);
        selectedPointInfoView.onNightModeChanged(nightModeOn);

        setBackgroundColor(
                getResources().getColor(
                        nightModeOn ? R.color.darkThemeChartBackground : R.color.lightThemeChartBackground
                )
        );

        for (int i = 0; i < getChildCount(); i++) {
            View view = getChildAt(i);
            if (view instanceof CheckBox) {
                ((CheckBox) view).setTextColor(nightModeOn ? Color.WHITE : Color.BLACK);
            }
        }
    }

    public void setGestureDirectionListener(GestureDirectionListener listener) {
        this.gestureDirectionListener = listener;
    }

}
