package ru.vmochalov.vkchart.dto;

import android.graphics.Color;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

class ChartParser {

    public Chart fromJson(String json) throws JSONException {

        String xId = null;
        List<String> lineIds = new ArrayList<>();
        List<Date> abscissa = new ArrayList<>();
        List<List<Integer>> ordinates = null;
        List<String> labels = null;
        List<Integer> colors = null;

        JSONTokener tokener = new JSONTokener(json);

        while (tokener.more()) {
            Object next = tokener.nextValue();

            if (next instanceof JSONObject) {
                JSONObject types = ((JSONObject) next).getJSONObject("types");
                Iterator<String> keys = types.keys();

                while (keys.hasNext()) {
                    String nextKey = keys.next();
                    String nextType = types.getString(nextKey);

                    if (nextType.equals("line")) {
                        lineIds.add(nextKey);
                    } else if (xId == null && nextType.equals("x")) {
                        xId = nextKey;
                    } else {
                        throw new IllegalStateException("Invalid types for input data: " + types.toString());
                    }
                }

                if (xId == null) {
                    throw new IllegalStateException("No x type found: " + types.toString());
                }

                labels = new ArrayList<>(lineIds.size());
                colors = new ArrayList<>(lineIds.size());
                ordinates = new ArrayList<>(lineIds.size());

                Collections.fill(ordinates, null);

                JSONObject names = ((JSONObject) next).getJSONObject("names");
                JSONObject colorValues = ((JSONObject) next).getJSONObject("colors");
                JSONArray columns = ((JSONObject) next).getJSONArray("columns");

                for (String id : lineIds) {
                    labels.add(names.getString(id));
                    colors.add(Color.parseColor(colorValues.getString(id)));
                }

                for (int i = 0; i < columns.length(); i++) {
                    JSONArray column = columns.getJSONArray(i);
                    String columnName = column.getString(0);

                    if (columnName.equals(xId)) {
                        for (int j = 1; j < column.length(); j++) {
                            abscissa.add(new Date(column.getLong(j)));
                        }
                    } else if (lineIds.contains(columnName)) {
                        List<Integer> line = new ArrayList<>();
                        for (int j = 1; j < column.length(); j++) {
                            line.add(column.getInt(j));
                        }
                        //todo: DO I need to check indexes of data???
                        ordinates.add(lineIds.indexOf(columnName), line);
                    }
                }
            }
        }
        return new Chart(xId, lineIds, abscissa, ordinates, labels, colors);
    }


}
