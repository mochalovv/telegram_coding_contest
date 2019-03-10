package ru.vmochalov.vkchart;

import android.app.Activity;
import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Toast;

import ru.vmochalov.vkchart.dto.Chart;
import ru.vmochalov.vkchart.view.ChartView;

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

        chartView.setChart(Chart.getSampleChart());

    }
}
