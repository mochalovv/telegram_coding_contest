package ru.vmochalov.vkchart.view;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;

import org.json.JSONException;

import java.util.List;

import ru.vmochalov.vkchart.R;
import ru.vmochalov.vkchart.dto.Chart;
import timber.log.Timber;

/**
 * Created by Vladimir Mochalov on 17.03.2019.
 */
public class ChartView extends LinearLayout {

    private DetailedChartView detailedChartView;
    private ChartNavigationView chartNavigationView;
    private LinearLayout chartContainer;

    public ChartView(Context context) {
        super(context);

        initView();
    }

    public ChartView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);

        initView();
    }

    public ChartView(Context context, AttributeSet attributeSet, int defStyleAttr) {
        super(context, attributeSet, defStyleAttr);

        initView();
    }

    public ChartView(Context context, AttributeSet attributeSet, int defStyleAttr, int defStyleRes) {
        super(context, attributeSet, defStyleAttr, defStyleRes);

        initView();
    }

    private void initView() {
        inflateView();
        initInnerViews();
    }

    private void inflateView() {
        LayoutInflater layoutInflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        layoutInflater.inflate(R.layout.layout_chart_view, this, true);
    }

    private void initInnerViews() {
        detailedChartView = findViewById(R.id.chart);
        chartNavigationView = findViewById(R.id.chartNavigation);
        chartContainer = findViewById(R.id.chartContainer);

        chartNavigationView.setPeriodChangedListener(
                new ChartNavigationView.PeriodChangedListener() {
                    @Override
                    public void onPeriodLengthChanged(double periodStart, double periodEnd, boolean startIsStable) {
                        detailedChartView.onVisibleRangeScaleChanged(periodStart, periodEnd, startIsStable);
                    }

                    @Override
                    public void onPeriodMoved(double periodStart, double periodEnd) {
                        detailedChartView.onVisibleRangeMoved(periodStart, periodEnd);
                    }

                    @Override
                    public void onPeriodModifyFinished() {
                        Timber.d("onPeriodModifyFinished()");
                    }
                }
        );
    }


    public void setChartData(String jsonSource) {
        Chart chart = parseChartFromJson(jsonSource);

        if (chart != null) {
            detailedChartView.setChart(chart);
            chartNavigationView.setChart(chart);

            List<String> lineIds = chart.getLineIds();
            List<String> lineLabels = chart.getLabels();
            List<Integer> colors = chart.getColors();

            for (int i = 0; i < lineIds.size(); i++) {
                CheckBox checkBox = new CheckBox(getContext());
                checkBox.setText(lineLabels.get(i));
                checkBox.setTag(lineIds.get(i));
                checkBox.setButtonTintList(
                        ColorStateList.valueOf(colors.get(i))
                );
                checkBox.setChecked(true);
                checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        String lineId = buttonView.getTag().toString();

                        detailedChartView.setLineVisibility(lineId, isChecked);
                        chartNavigationView.setLineVisibility(lineId, isChecked);
                    }
                });

                chartContainer.addView(checkBox);
            }
        }
    }

    public void setNightMode(boolean nightModeOn) {
        detailedChartView.setNightMode(nightModeOn);
        chartNavigationView.setNightMode(nightModeOn);

        setBackgroundColor(getResources().getColor(nightModeOn ? R.color.darkThemeChartBackground : R.color.lightThemeChartBackground));

        for (int i = 0; i < chartContainer.getChildCount(); i++) {
            View view = chartContainer.getChildAt(i);
            if (view instanceof CheckBox) {
                ((CheckBox) view).setTextColor(nightModeOn ? Color.WHITE : Color.BLACK);
            }
        }
    }

    private Chart parseChartFromJson(String jsonSource) {
        try {
            return Chart.parse(jsonSource);
        } catch (
                JSONException ex) {
            Timber.e("Can not parse json: " + ex.getMessage());
        }

        return null;
    }
}
