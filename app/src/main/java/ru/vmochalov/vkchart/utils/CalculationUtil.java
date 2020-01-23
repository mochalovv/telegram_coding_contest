package ru.vmochalov.vkchart.utils;

import java.util.Collections;
import java.util.List;

/**
 * Created by Vladimir Mochalov on 18.01.2020.
 */
public class CalculationUtil {

    public static boolean isHorizontalMovement(float initialX, float initialY, float updatedX, float updatedY) {
        if (initialX == updatedX) return false;

        double tg = (updatedY - initialY) / (updatedX - initialX);

        return Math.abs(tg) < 1;
    }

    public static int getMaxValue(List<List<Integer>> lists) {
        int max = Integer.MIN_VALUE;

        for (List<Integer> list : lists) {
            max = Math.max(max, Collections.max(list));
        }

        return max;
    }

    public static int getPowOfTwo(int pow) {
        int result = 1;

        for (int i = 0; i < pow; i++) {
            result = result << 1;
        }

        return result;
    }

}
