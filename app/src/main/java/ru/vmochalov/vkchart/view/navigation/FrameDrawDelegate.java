package ru.vmochalov.vkchart.view.navigation;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;

/**
 * Created by Vladimir Mochalov on 29.01.2020.
 */
public class FrameDrawDelegate {

    private static final float FRAME_VERTICAL_BORDER_WIDTH = 10;
    private static final float FRAME_HORIZONTAL_BORDER_WIDTH = 4;

    private Paint activeBackgroundPaint = new Paint();
    private Paint framePaint = new Paint();
    private Paint duff = new Paint();

    private int activeBackgroundColor = Color.WHITE;
    private int activeBackgroundColorNightMode = Color.rgb(29, 39, 51);
    private int frameColor = Color.rgb(219, 231, 240);
    private int frameColorNightMode = Color.rgb(43, 66, 86);
    private int passiveBackgroundColor = Color.argb(0xa0, 245, 248, 249);
    private int passiveBackgroundColorNightMode = Color.argb(0xa0, 25, 33, 46);

    private float frameStart;
    private float frameWidth;

    FrameDrawDelegate(float frameStart, float frameWidth) {
        this.frameStart = frameStart;
        this.frameWidth = frameWidth;

        activeBackgroundPaint.setColor(activeBackgroundColor);
        activeBackgroundPaint.setStyle(Paint.Style.FILL_AND_STROKE);

        framePaint.setColor(frameColor);
        framePaint.setStyle(Paint.Style.FILL_AND_STROKE);

        duff.setStyle(Paint.Style.FILL_AND_STROKE);
        duff.setColor(passiveBackgroundColor);
        duff.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.OVERLAY));


    }

    void drawFrame(Canvas canvas) {
        int width = canvas.getWidth();
        int height = canvas.getHeight();

        // draw active background
        canvas.drawRect(0, 0, width, height, activeBackgroundPaint);

        // draw frame
        canvas.drawRect(frameStart, 0, frameStart + frameWidth - 1, FRAME_VERTICAL_BORDER_WIDTH, framePaint);
        canvas.drawRect(frameStart, height - FRAME_VERTICAL_BORDER_WIDTH, frameStart + frameWidth - 1, height, framePaint);
        canvas.drawRect(frameStart, 0, frameStart + FRAME_HORIZONTAL_BORDER_WIDTH, height, framePaint);
        canvas.drawRect(frameStart + frameWidth - 1 - FRAME_HORIZONTAL_BORDER_WIDTH, 0, frameStart + frameWidth - 1, height, framePaint);
    }

    void drawShadow(Canvas canvas) {
        int width = canvas.getWidth();
        int height = canvas.getHeight();

        canvas.drawRect(0, 0, (int) frameStart - 1, height, duff);
        canvas.drawRect((int) (frameStart + frameWidth), 0, width, height, duff);
    }

    void onNightModeChanged(boolean nightModeOn) {
        activeBackgroundPaint.setColor(nightModeOn ? activeBackgroundColorNightMode : activeBackgroundColor);
        framePaint.setColor(nightModeOn ? frameColorNightMode : frameColor);
        duff.setColor(nightModeOn ? passiveBackgroundColorNightMode : passiveBackgroundColor);
    }

    void onFrameUpdated(float frameStart, float frameWidth) {
        this.frameStart = frameStart;
        this.frameWidth = frameWidth;
    }

    float getFrameStart() {
        return frameStart;
    }

    float getFrameEnd() {
        return frameStart + frameWidth;
    }
}
