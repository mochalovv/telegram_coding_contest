package ru.vmochalov.vkchart.utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONTokener;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Vladimir Mochalov on 03.01.2020.
 */
public class JsonParsingUtil {

    public static List<String> getRawJsonObjectSources(String rawJsonArraySource) {
        JSONTokener tokener = new JSONTokener(rawJsonArraySource);

        List<String> result = new ArrayList<>();
        Object next;

        try {
            while (tokener.more()) {
                next = tokener.nextValue();

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
}
