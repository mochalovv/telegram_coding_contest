package ru.vmochalov.vkchart.chart.view.secondary.delegates;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;

import ru.vmochalov.vkchart.R;

/**
 * Created by Vladimir Mochalov on 29.01.2020.
 */
public class FrameDrawDelegate {

    private static final float FRAME_VERTICAL_BORDER_WIDTH = 10;
    private static final float FRAME_HORIZONTAL_BORDER_WIDTH = 4;

    private Resources resources;

    private Paint backgroundPaint = new Paint();
    private Paint framePaint = new Paint();
    private Paint shadowPaint = new Paint();

    private float frameStart;
    private float frameWidth;

    public FrameDrawDelegate(Resources resources, float frameStart, float frameWidth) {
        this.resources = resources;

        this.frameStart = frameStart;
        this.frameWidth = frameWidth;

        backgroundPaint.setColor(resources.getColor(R.color.lightSecondaryBackground));
        backgroundPaint.setStyle(Paint.Style.FILL_AND_STROKE);

        framePaint.setColor(resources.getColor(R.color.lightSecondaryFrame));
        framePaint.setStyle(Paint.Style.FILL_AND_STROKE);

        shadowPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        shadowPaint.setColor(resources.getColor(R.color.lightSecondaryShadow));
        shadowPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.OVERLAY));
    }

    public void drawFrame(Canvas canvas) {
        int width = canvas.getWidth();
        int height = canvas.getHeight();

        // draw active background
        canvas.drawRect(0, 0, width, height, backgroundPaint);

        // draw frame
        canvas.drawRect(
                frameStart,
                0,
                frameStart + frameWidth - 1,
                FRAME_VERTICAL_BORDER_WIDTH,
                framePaint
        );
        canvas.drawRect(
                frameStart,
                height - FRAME_VERTICAL_BORDER_WIDTH,
                frameStart + frameWidth - 1,
                height,
                framePaint
        );
        canvas.drawRect(
                frameStart,
                0,
                frameStart + FRAME_HORIZONTAL_BORDER_WIDTH,
                height,
                framePaint
        );
        canvas.drawRect(
                frameStart + frameWidth - 1 - FRAME_HORIZONTAL_BORDER_WIDTH,
                0,
                frameStart + frameWidth - 1,
                height,
                framePaint
        );
    }

    public void drawShadow(Canvas canvas) {
        int width = canvas.getWidth();
        int height = canvas.getHeight();

        canvas.drawRect(0, 0, (int) frameStart - 1, height, shadowPaint);
        canvas.drawRect((int) (frameStart + frameWidth), 0, width, height, shadowPaint);
    }

    public void onNightModeChanged(boolean nightModeOn) {
        int backgroundColor = resources.getColor(
                nightModeOn ? R.color.darkSecondaryBackground : R.color.lightSecondaryBackground
        );
        int frameColor = resources.getColor(
                nightModeOn ? R.color.darkSecondaryFrame : R.color.lightSecondaryFrame
        );
        int shadowColor = resources.getColor(
                nightModeOn ? R.color.darkSecondaryShadow : R.color.lightSecondaryShadow
        );

        backgroundPaint.setColor(backgroundColor);
        framePaint.setColor(frameColor);
        shadowPaint.setColor(shadowColor);
    }

    public void onFrameUpdated(float frameStart, float frameWidth) {
        this.frameStart = frameStart;
        this.frameWidth = frameWidth;
    }

    public float getFrameStart() {
        return frameStart;
    }

    public float getFrameEnd() {
        return frameStart + frameWidth;
    }
}
