package ru.vmochalov.vkchart.chart.view.primary.delegates;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;

import ru.vmochalov.vkchart.R;

/**
 * Created by Vladimir Mochalov on 18.01.2020.
 */
public class BackgroundDrawDelegate {

    private Resources resources;

    private float width;
    private float height;

    private Paint backgroundPaint = new Paint();

    public BackgroundDrawDelegate(Resources resources) {
        this.resources = resources;

        backgroundPaint.setColor(resources.getColor(R.color.lightThemeChartBackground));
        backgroundPaint.setStyle(Paint.Style.FILL_AND_STROKE);
    }

    public Paint getBackgroundPaint() {
        return backgroundPaint;
    }

    public void setCanvasSize(float width, float height) {
        this.width = width;
        this.height = height;
    }

    public void drawBackground(Canvas canvas) {
        canvas.drawRect(0, 0, width, height, backgroundPaint);
    }

    public void onNightModeChanged(boolean nightModeOn) {
        int backgroundColor = resources.getColor(nightModeOn ? R.color.darkThemeChartBackground : R.color.lightThemeChartBackground);

        backgroundPaint.setColor(backgroundColor);

    }
}