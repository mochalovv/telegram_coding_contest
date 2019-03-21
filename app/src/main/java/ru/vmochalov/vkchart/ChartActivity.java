package ru.vmochalov.vkchart;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONTokener;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import ru.vmochalov.vkchart.view.ChartView;
import timber.log.Timber;

/**
 * Created by Vladimir Mochalov on 10.03.2019.
 */
public class ChartActivity extends Activity {

    private boolean nightModeOn;
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
        chartContainer = findViewById(R.id.chartContainer);

        for (String json : getDividedJsons(readInputData())) {
            ChartView chartView = new ChartView(this);
            chartView.setChartData(json);
            chartContainer.addView(chartView);
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

    private List<String> getDividedJsons(String input) {
        JSONTokener tokener = new JSONTokener(input);

        List<String> result = new ArrayList<>();
        try {
            while (tokener.more()) {
                Object next = tokener.nextValue();

                if (next instanceof JSONArray) {
                    JSONArray arr = ((JSONArray) next);

                    for (int i = 0; i < arr.length(); i++) {
                        result.add(arr.get(i).toString());
                    }
                }
            }
        } catch (JSONException ex) {
            //do nothing
        }

        return result;
    }


    private String readInputData() {
        InputStream is = null;
        String json = null;
        try {
            is = getAssets().open("chart_data.json");
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
