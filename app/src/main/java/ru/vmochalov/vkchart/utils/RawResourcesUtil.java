package ru.vmochalov.vkchart.utils;

import android.content.res.Resources;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Created by Vladimir Mochalov on 03.01.2020.
 */
public class RawResourcesUtil {

    public static String getRawResourceAsString(Resources resources, int resourceId) throws IOException {
        StringBuilder result = new StringBuilder();

        BufferedReader reader = new BufferedReader(new InputStreamReader(resources.openRawResource(resourceId)));

        String nextLine;

        try {
            while ((nextLine = reader.readLine()) != null) {
                result.append(nextLine).append(System.getProperty("line.separator"));
            }
        } finally {
            reader.close();
        }

        return result.toString();
    }
}