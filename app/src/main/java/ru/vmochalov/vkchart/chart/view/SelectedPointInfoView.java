package ru.vmochalov.vkchart.chart.view;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import ru.vmochalov.vkchart.R;
import ru.vmochalov.vkchart.chart.data.Chart;

import static android.util.TypedValue.COMPLEX_UNIT_SP;

/**
 * Created by Vladimir Mochalov on 01.02.2020.
 */
public class SelectedPointInfoView extends LinearLayout {

    private static final int HORIZONTAL_MARGIN_PX = 10;

    private Chart chart;

    private TextView dateView;
    private LinearLayout valuesContainer;
    private LinearLayout labelsContainer;

    public SelectedPointInfoView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);

        LayoutInflater
                .from(getContext())
                .inflate(R.layout.layout_selected_point_info_view, this);

        setOrientation(VERTICAL);

        onViewInflated();
    }

    private void onViewInflated() {
        dateView = findViewById(R.id.dateView);
        valuesContainer = findViewById(R.id.valuesContainer);
        labelsContainer = findViewById(R.id.labelsContainer);
    }

    public void setChart(Chart chart) {
        this.chart = chart;

        onChartInited();
    }

    private void onChartInited() {
        valuesContainer.removeAllViews();
        labelsContainer.removeAllViews();

        for (int i = 0; i < chart.getLineIds().size(); i++) {
            valuesContainer.addView(createValueTextView(i));
            labelsContainer.addView(createLabelTextView(i));
        }

        for (int i = 0; i < valuesContainer.getChildCount(); i++) {
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) valuesContainer.getChildAt(i).getLayoutParams();
            params.weight = 1;
            params.rightMargin = 16;
        }

        for (int i = 0; i < labelsContainer.getChildCount(); i++) {
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) labelsContainer.getChildAt(i).getLayoutParams();
            params.weight = 1;

        }
    }

    private TextView createValueTextView(int lineIndex) {
        String lineId = chart.getLineIds().get(lineIndex);
        int lineColor = chart.getColors().get(lineIndex);

        TextView valueTextView = new TextView(getContext());
        valueTextView.setTag(lineId);
        valueTextView.setTextSize(COMPLEX_UNIT_SP, 14);
        valueTextView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        valueTextView.setTextColor(lineColor);

        return valueTextView;
    }

    private TextView createLabelTextView(int lineIndex) {
        String lineId = chart.getLineIds().get(lineIndex);
        int lineColor = chart.getColors().get(lineIndex);

        TextView nameTextView = new TextView(getContext());
        nameTextView.setTag(lineId);
        nameTextView.setTextColor(lineColor);
        nameTextView.setText(lineId);
        nameTextView.setTextSize(COMPLEX_UNIT_SP, 12);

        return nameTextView;
    }

    public void onSelectedPointChanged(int selectedPointIndex) {
        dateView.setText(chart.getAbscissaAsString().get(selectedPointIndex));

        for (int i = 0; i < chart.getLineIds().size(); i++) {
            ((TextView) valuesContainer.getChildAt(i)).setText(
                    chart.getOrdinates().get(i).get(selectedPointIndex).toString()
            );
        }
    }

    public void onTouch(float x, int primaryChartWidth) {
        int gap = getWidth() / 2 + HORIZONTAL_MARGIN_PX;

        float newX = x + gap;

        if ((newX + getWidth() + HORIZONTAL_MARGIN_PX) > primaryChartWidth) {
            newX = primaryChartWidth - getWidth() - HORIZONTAL_MARGIN_PX;
        }

        setX(newX);
    }

    public void onNightModeChanged(boolean nightModeOn) {
        setBackgroundColor(
                getResources().getColor(
                        nightModeOn ? R.color.darkThemeChartBackground : R.color.lightThemeChartBackground
                )
        );
        dateView.setTextColor(getResources().getColor(nightModeOn ? android.R.color.white : android.R.color.black));
    }

    public void onLineVisiblityChanged(String lineId, boolean visible) {
        valuesContainer.findViewWithTag(lineId).setVisibility(visible ? View.VISIBLE : View.GONE);
        labelsContainer.findViewWithTag(lineId).setVisibility(visible ? View.VISIBLE : View.GONE);
    }

}