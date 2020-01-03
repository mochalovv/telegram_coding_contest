package ru.vmochalov.vkchart;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import java.io.IOException;

import ru.vmochalov.vkchart.utils.JsonParsingUtil;
import ru.vmochalov.vkchart.utils.RawResourcesUtil;
import ru.vmochalov.vkchart.view.ChartView;

public class ChartActivity extends Activity {

    private boolean nightModeOn;

    private ScrollView scrollView;
    private LinearLayout chartContainer;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_chart);

        initViews();

        onNightModeChanged(nightModeOn);
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.night_mode, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.nightMode) {
            nightModeOn = !nightModeOn;
            onNightModeChanged(nightModeOn);

            return true;
        }
        return false;
    }

    private void initViews() {
        scrollView = findViewById(R.id.scrollView);
        chartContainer = findViewById(R.id.chartContainer);

        initChartViews();
    }

    private void initChartViews() {
        try {
            String chartsInput = RawResourcesUtil.getRawResourceAsString(getResources(), R.raw.charts_input);

            for (String json : JsonParsingUtil.getRawJsonObjectSources(chartsInput)) {
                ChartView chartView = new ChartView(this);
                chartView.setChartData(json);
                chartView.setMovementDirectionListener(new ChartView.MovementDirectionListener() {
                    @Override
                    public void onMovementDirectionChanged(boolean isHorizontalMovement) {
                        scrollView.requestDisallowInterceptTouchEvent(isHorizontalMovement);
                    }
                });
                chartContainer.addView(chartView);
            }
        } catch (IOException ex) {
            Log.e(this.getClass().getName(), "Error while reading charts input: " + ex.getMessage());
        }
    }

    private void onNightModeChanged(boolean nightModeOn) {
        for (int i = 0; i < chartContainer.getChildCount(); i++) {
            View child = chartContainer.getChildAt(i);
            if (child instanceof ChartView) {
                ((ChartView) child).setNightMode(nightModeOn);
            }
        }

        getWindow().setStatusBarColor(getResources().getColor(nightModeOn ? R.color.darkThemeStatusbar : R.color.lightThemeStatusbar));
        getActionBar().setBackgroundDrawable(getResources().getDrawable(nightModeOn ? R.color.darkThemeToolbar : R.color.lightThemeToolbar));
    }

}
