package ru.vmochalov.vkchart.view.detailed;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;

import ru.vmochalov.vkchart.R;

/**
 * Created by Vladimir Mochalov on 18.01.2020.
 */
class BackgroundDrawDelegate {

    private Resources resources;

    private float width;
    private float height;

    //todo: make it private when refactoring is finished
    Paint backgroundPaint = new Paint();

    BackgroundDrawDelegate(Resources resources) {
        this.resources = resources;

        backgroundPaint.setColor(resources.getColor(R.color.lightThemeChartBackground));
        backgroundPaint.setStyle(Paint.Style.FILL_AND_STROKE);
    }

    void setCanvasSize(float width, float height) {
        this.width = width;
        this.height = height;
    }

    void drawBackground(Canvas canvas) {
        canvas.drawRect(0, 0, width, height, backgroundPaint);
    }

    void onNightModeChanged(boolean nightModeOn) {
        int backgroundColor = resources.getColor(nightModeOn ? R.color.darkThemeChartBackground : R.color.lightThemeChartBackground);

        backgroundPaint.setColor(backgroundColor);

    }
}