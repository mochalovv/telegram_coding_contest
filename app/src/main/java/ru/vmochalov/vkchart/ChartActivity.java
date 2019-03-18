package ru.vmochalov.vkchart;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import java.io.IOException;
import java.io.InputStream;

import ru.vmochalov.vkchart.view.ChartView;
import timber.log.Timber;

/**
 * Created by Vladimir Mochalov on 10.03.2019.
 */
public class ChartActivity extends Activity {

    private ChartView chartView;
    private boolean nightModeOn;

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
        chartView = findViewById(R.id.chartView);
        chartView.setChartData(readInputData());
    }

    private void onNightModeChanged(boolean nightModeOn) {
        chartView.setNightMode(nightModeOn);

        getWindow().setStatusBarColor(getResources().getColor(nightModeOn ? R.color.darkThemeStatusbar : R.color.lightThemeStatusbar));
        getActionBar().setBackgroundDrawable(getResources().getDrawable(nightModeOn ? R.color.darkThemeToolbar : R.color.lightThemeToolbar));
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
