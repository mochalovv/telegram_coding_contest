package ru.vmochalov.vkchart.view;

import android.content.Context;
import android.content.res.ColorStateList;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;

import org.json.JSONException;

import java.util.List;

import ru.vmochalov.vkchart.R;
import ru.vmochalov.vkchart.dto.CombinedChart;
import timber.log.Timber;

/**
 * Created by Vladimir Mochalov on 17.03.2019.
 */
public class ChartView extends LinearLayout {

    private InnerChartView innerChartView;
    private ChartNavigationView chartNavigationView;

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
        innerChartView = findViewById(R.id.chart);
        chartNavigationView = findViewById(R.id.chartNavigation);

        chartNavigationView.setPeriodChangedListener(
                new ChartNavigationView.PeriodChangedListener() {
                    @Override
                    public void onPeriodLengthChanged(double periodStart, double periodEnd) {
                        innerChartView.onVisibleRangeScaleChanged(periodStart, periodEnd);
                    }

                    @Override
                    public void onPeriodMoved(double periodStart, double periodEnd) {
                        innerChartView.onVisibleRangeMoved(periodStart, periodEnd);
                    }

                    @Override
                    public void onPeriodModifyFinished() {
                        Timber.d("onPeriodModifyFinished()");
                    }
                }
        );
    }

    public void setChartData(String jsonSource) {
        CombinedChart chart = parseChartFromJson(jsonSource);

        if (chart != null) {
            innerChartView.setChart(chart);
            chartNavigationView.setCombinedChart(chart);

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

                        innerChartView.setLineVisibility(lineId, isChecked);
                        chartNavigationView.setLineVisibility(lineId, isChecked);
                    }
                });

                addView(checkBox);
            }
        }
    }

    private CombinedChart parseChartFromJson(String jsonSource) {
        try {
            return CombinedChart.parse(jsonSource);
        } catch (
                JSONException ex) {
            Timber.e("Can not parse json: " + ex.getMessage());
        }

        return null;
    }
}
