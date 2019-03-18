package ru.vmochalov.vkchart;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toolbar;

import java.io.IOException;
import java.io.InputStream;

import ru.vmochalov.vkchart.view.ChartView;
import timber.log.Timber;

/**
 * Created by Vladimir Mochalov on 10.03.2019.
 */
public class ChartActivity extends Activity {

    private ChartView chartView;
    private Toolbar toolbar;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_chart);

        initViews();

//        setActionBar(toolbar);
    }

    private boolean nightModeOn;

    private void initViews() {
        chartView = findViewById(R.id.chartView);
        toolbar = findViewById(R.id.toolbar);

        chartView.setChartData(readInputData());


        toolbar.inflateMenu(R.menu.night_mode);

        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (item.getItemId() == R.id.nightMode) {
                    nightModeOn = !nightModeOn;
                    chartView.setNightMode(nightModeOn);

                    toolbar.setBackgroundColor(nightModeOn ? Color.rgb(33, 45, 59) : Color.rgb(81, 125, 162));
//                    toolbar.setTitleTextColor(nightModeOn ? Color.WHITE : Color.BLACK);

                    return true;
                }
                return false;
            }
        });
//        chartView.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                chartView.setNightMode(true);
//            }
//        }, 1000);
    }

    private void onNightMode() {}

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
