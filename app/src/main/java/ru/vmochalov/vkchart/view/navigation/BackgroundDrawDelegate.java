package ru.vmochalov.vkchart.view.navigation;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

/**
 * Created by Vladimir Mochalov on 29.01.2020.
 */
public class BackgroundDrawDelegate {

//    private int height;
//    private int width;

    private Paint activeBackgroundPaint = new Paint();
    private Paint framePaint = new Paint();

    private int activeBackgroundColor = Color.WHITE;
    private int activeBackgroundColorNightMode = Color.rgb(29, 39, 51);
    private int frameColor = Color.rgb(219, 231, 240);
    private int frameColorNightMode = Color.rgb(43, 66, 86);


    BackgroundDrawDelegate() {

        activeBackgroundPaint.setColor(activeBackgroundColor);
        activeBackgroundPaint.setStyle(Paint.Style.FILL_AND_STROKE);

        framePaint.setColor(frameColor);
        framePaint.setStyle(Paint.Style.FILL_AND_STROKE);

    }

    //todo: continue moving code from navigation view to its background delegate
    void drawBackground(Canvas canvas) {
       int width = canvas.getWidth();
       int height = canvas.getHeight();

        // draw active background
        canvas.drawRect(0, 0, width, height, activeBackgroundPaint);

        // draw frame
        canvas.drawRect(frameStart, 0, frameStart + frameWidth - 1, frameVerticalBorderWidth, framePaint);
        canvas.drawRect(frameStart, height - frameVerticalBorderWidth, frameStart + frameWidth - 1, height, framePaint);
        canvas.drawRect(frameStart, 0, frameStart + frameHorizontalBorderWidth, height, framePaint);
        canvas.drawRect(frameStart + frameWidth - 1 - frameHorizontalBorderWidth, 0, frameStart + frameWidth - 1, height, framePaint);

//        drawShadow(canvas);
    }

}
