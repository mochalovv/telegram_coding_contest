package ru.vmochalov.vkchart.view;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ru.vmochalov.vkchart.R;
import ru.vmochalov.vkchart.chart.Chart;
import ru.vmochalov.vkchart.view.detailed.DetailedChartView;
import ru.vmochalov.vkchart.view.navigation.PeriodChangedListener;
import ru.vmochalov.vkchart.view.navigation.ChartNavigationView;

import static android.util.TypedValue.COMPLEX_UNIT_SP;

/**
 * Created by Vladimir Mochalov on 17.03.2019.
 */
public class ChartView extends LinearLayout {

    private DetailedChartView detailedChartView;
    private ChartNavigationView chartNavigationView;
    private LinearLayout chartContainer;

    private View infoView;
    private TextView dateView;
    private LinearLayout valuesView;
    private LinearLayout namesView;

    private List<TextView> valueViews = new ArrayList<>();
    private List<TextView> nameViews = new ArrayList<>();

    public interface MovementDirectionListener {
        void onMovementDirectionChanged(boolean isHorizontalMovement);
    }

    private MovementDirectionListener listener;

    public void setMovementDirectionListener(MovementDirectionListener listener) {
        this.listener = listener;
    }

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
        infoView = findViewById(R.id.infoView);
        dateView = findViewById(R.id.dateView);
        valuesView = findViewById(R.id.values);
        namesView = findViewById(R.id.chartNames);

        chartNavigationView.setPeriodChangedListener(
                new PeriodChangedListener() {
                    @Override
                    public void onPeriodChangedMoved(double periodStart, double periodEnd) {
                        detailedChartView.onVisibleRangeChanged(periodStart, periodEnd);
                    }

                    @Override
                    public void onPeriodModifyFinished() {
                    }

                    @Override
                    public void onDragDirectionChanged(boolean horizontal) {
                        if (listener != null) {
                            listener.onMovementDirectionChanged(horizontal);
                        }
                    }
                }
        );
    }

    private Chart chart;
    private boolean[] visibility;

    public void setChartData(String jsonSource) {
        chart = parseChartFromJson(jsonSource);

        if (chart != null) {
            detailedChartView.setChart(chart);
            chartNavigationView.setChart(chart);

            final List<String> lineIds = chart.getLineIds();
            List<String> lineLabels = chart.getLabels();
            List<Integer> colors = chart.getColors();
            visibility = new boolean[lineIds.size()];
            Arrays.fill(visibility, true);

            for (int i = 0; i < lineIds.size(); i++) {
                String tag = lineIds.get(i);

                CheckBox checkBox = new CheckBox(getContext());
                checkBox.setText(lineLabels.get(i));
                checkBox.setTag(tag);
                checkBox.setButtonTintList(
                        ColorStateList.valueOf(colors.get(i))
                );
                checkBox.setChecked(true);
                final int index = i;
                checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        String lineId = buttonView.getTag().toString();

                        detailedChartView.setLineVisibility(lineId, isChecked);
                        chartNavigationView.setLineVisibility(lineId, isChecked);

                        visibility[index] = isChecked;

                        infoView.findViewWithTag(lineId).setVisibility(isChecked ? View.VISIBLE : View.GONE);
                        namesView.findViewWithTag(lineId).setVisibility(isChecked ? View.VISIBLE : View.GONE);
                    }
                });

                chartContainer.addView(checkBox);

                TextView valueTextView = new TextView(getContext());
                valueTextView.setTag(tag);
                valueTextView.setTextSize(COMPLEX_UNIT_SP, 14);
                valueTextView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
                valueTextView.setTextColor(colors.get(i));

                valuesView.addView(valueTextView);

                valueViews.add(valueTextView);

                LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) valueTextView.getLayoutParams();
                params.weight = 1;
                params.rightMargin = 16;

                TextView nameTextView = new TextView(getContext());
                nameTextView.setTag(tag);
                nameTextView.setTextColor(colors.get(i));
                nameTextView.setText(tag);
                nameTextView.setTextSize(COMPLEX_UNIT_SP, 12);
                namesView.addView(nameTextView);

                nameViews.add(nameTextView);

                params = (LinearLayout.LayoutParams) nameTextView.getLayoutParams();
                params.weight = 1;

            }

            detailedChartView.setOnChartClickedListener(new DetailedChartView.OnChartClickedListener() {

                @Override
                public void onTouch(float x, int pointIndex, List<Integer> values) {
                    updateInfo(pointIndex);
                    if (!values.isEmpty()) {
                        updateInfoViewX(x);
                        infoView.setVisibility(View.VISIBLE);
                    }
                }

                @Override
                public void onButtonUp() {
                    infoView.setVisibility(View.GONE);
                }

                @Override
                public void onMovementDirectionChanged(boolean isHorizontal) {
                    if (listener != null) {
                        listener.onMovementDirectionChanged(isHorizontal);
                    }

                    if (!isHorizontal) {
                        infoView.setVisibility(View.GONE);
                    }
                }

                private void updateInfo(int pointIndex) {
                    dateView.setText((chart.getAbscissaAsLongString().get(pointIndex)));

                    for (int i = 0; i < visibility.length; i++) {

                        if (visibility[i]) {
                            valueViews.get(i).setText(chart.getOrdinates().get(i).get(pointIndex).toString());
                        }
                    }
                }

                float newX;
                int infoWidthGap = infoView.getWidth() / 2 + 30;

                private void updateInfoViewX(float x) {

                    newX = x + infoWidthGap;

                    if ((newX + infoView.getWidth()) > detailedChartView.getWidth()) {
                        newX = detailedChartView.getWidth() - infoView.getWidth();
                    }

                    infoView.setX(newX);

                }
            });
        }
    }

    public void setNightMode(boolean nightModeOn) {
        detailedChartView.onNightModeChanged(nightModeOn);
        chartNavigationView.onNightModeChanged(nightModeOn);

        int backgroundColor = getResources().getColor(nightModeOn ? R.color.darkThemeChartBackground : R.color.lightThemeChartBackground);
        setBackgroundColor(backgroundColor);

        for (int i = 0; i < chartContainer.getChildCount(); i++) {
            View view = chartContainer.getChildAt(i);
            if (view instanceof CheckBox) {
                ((CheckBox) view).setTextColor(nightModeOn ? Color.WHITE : Color.BLACK);
            }
        }

        infoView.setBackgroundColor(backgroundColor);
        dateView.setTextColor(getResources().getColor(nightModeOn ? android.R.color.white : android.R.color.black));
    }

    private Chart parseChartFromJson(String jsonSource) {
        try {
            return Chart.fromJson(jsonSource);
        } catch (JSONException ex) {
            //do nothing
        }

        return null;
    }
}
