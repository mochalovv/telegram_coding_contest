package ru.vmochalov.vkchart;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import org.json.JSONException;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import ru.vmochalov.vkchart.dto.CombinedChart;
import ru.vmochalov.vkchart.view.ChartNavigationView;
import ru.vmochalov.vkchart.view.ChartView;
import timber.log.Timber;

/**
 * Created by Vladimir Mochalov on 10.03.2019.
 */
public class ChartActivity extends Activity {

    private ChartView chartView;
    private ChartNavigationView chartNavigationView;
    private ViewGroup chartContainer;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_chart);

        chartView = findViewById(R.id.chart);
        chartNavigationView = findViewById(R.id.chartNavigation);
        chartContainer = findViewById(R.id.chartContainer);

        initViews();

    }

    private void initViews() {
        initChart();

        chartNavigationView.setPeriodChangedListener(
                new ChartNavigationView.PeriodChangedListener() {
                    @Override
                    public void onPeriodLengthChanged(double periodStart, double periodEnd) {
                        chartView.onVisibleRangeScaleChanged(periodStart, periodEnd);
                    }

                    @Override
                    public void onPeriodMoved(double periodStart, double periodEnd) {
                        chartView.onVisibleRangeMoved(periodStart, periodEnd);
                    }

                    @Override
                    public void onPeriodModifyFinished() {
                        Timber.d("onPeriodModifyFinished()");
                    }
                }
        );
    }


    private CombinedChart readChartFromAssets() {
        try {
            return CombinedChart.parse(readInputData());
        } catch (JSONException ex) {
            Timber.e("Can not parse json: " + ex.getMessage());
        }

        return null;
    }

    private void initChart() {

        CombinedChart chart = readChartFromAssets();

        if (chart != null) {
            chartView.setChart(chart);
            chartNavigationView.setCombinedChart(chart);

            List<String> lineIds = chart.getLineIds();
            List<String> lineLabels = chart.getLabels();
            List<Integer> colors = chart.getColors();

            for (int i = 0; i < lineIds.size(); i++) {
                CheckBox checkBox = new CheckBox(this);
                checkBox.setText(lineLabels.get(i));
                checkBox.setTag(lineIds.get(i));
                checkBox.setButtonTintList(
                        ColorStateList.valueOf(colors.get(i))
                );
                checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        Timber.d("Checkbox for line name: " + buttonView.getText() + ", id: " + buttonView.getTag() + " is checked: " + isChecked);
                    }
                });

                chartContainer.addView(checkBox);
            }

        }
    }

    private String readInputData() {
        InputStream is = null;
        String json = null;
        try {
            is = getAssets().open("first_chart_data.json");
            int bufferSize = is.available();
            byte[] buffer = new byte[bufferSize];
            is.read(buffer);
            is.close();
            json = new String(buffer);

//            Timber.d("JSON: " + json);
        } catch (IOException ex) {
            Timber.e("Can not open asset: " + ex.getMessage());
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ex) {
                    Timber.e("Can not close inputstream: " + ex.getMessage());
                }
            }

        }
        return json;

    }
}
