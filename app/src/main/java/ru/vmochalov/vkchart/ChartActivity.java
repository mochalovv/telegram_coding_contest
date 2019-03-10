package ru.vmochalov.vkchart;

import android.app.Activity;
import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Toast;

import org.json.JSONException;

import java.io.IOException;
import java.io.InputStream;

import ru.vmochalov.vkchart.dto.CombinedChart;
import ru.vmochalov.vkchart.view.ChartView;
import timber.log.Timber;

/**
 * Created by Vladimir Mochalov on 10.03.2019.
 */
public class ChartActivity extends Activity {

    private ChartView chartView;
    private CheckBox joinedCheckBox;
    private CheckBox leftCheckBox;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_chart);

        chartView = findViewById(R.id.chart);
        joinedCheckBox = findViewById(R.id.joinedCheckBox);
        leftCheckBox = findViewById(R.id.leftCheckBox);

        initViews();

    }

    private void initViews() {
        joinedCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Toast.makeText(ChartActivity.this, "Joined is checked: " + isChecked, Toast.LENGTH_SHORT).show();
            }
        });

        leftCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Toast.makeText(ChartActivity.this, "Left is checked: " + isChecked, Toast.LENGTH_SHORT).show();
            }
        });

        initChart();
    }

    private void initChart() {

        String json = readInputData();

        try {
            chartView.setChart(CombinedChart.parse(json));
        } catch (JSONException ex) {
            Timber.e("Can not parse json: " + ex.getMessage());
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
