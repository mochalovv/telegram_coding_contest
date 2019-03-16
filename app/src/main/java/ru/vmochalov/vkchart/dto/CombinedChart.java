package ru.vmochalov.vkchart.dto;

import org.json.JSONException;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Created by Vladimir Mochalov on 10.03.2019.
 */
public class CombinedChart {

    private static DateFormat dateFormat = new SimpleDateFormat("MMM d");

    private String xId;
    private List<String> lineIds;

    private List<Date> abscissa;
    private List<String> abscissaAsString;
    private List<List<Integer>> ordinates;
    private List<String> labels;
    private List<Integer> colors;

    public static CombinedChart parse(String json) throws JSONException {
        return new ChartParser().fromJson(json);
    }

    CombinedChart(
            String xId,
            List<String> lineIds,
            List<Date> abscissa,
            List<List<Integer>> ordinates,
            List<String> labels,
            List<Integer> colors
    ) {
        this.xId = xId;
        this.lineIds = lineIds;
        this.abscissa = abscissa;
        this.ordinates = ordinates;
        this.labels = labels;
        this.colors = colors;

        this.abscissaAsString = new ArrayList<>();
        for (Date date : abscissa) {
            abscissaAsString.add(dateFormat.format(date));
        }
    }

    public List<Date> getAbscissa() {
        return abscissa;
    }

    public List<String> getAbscissaAsString() {
        return abscissaAsString;
    }

    public List<List<Integer>> getOrdinates() {
        return ordinates;
    }

    public List<String> getLabels() {
        return labels;
    }

    public List<Integer> getColors() {
        return colors;
    }

    public List<String> getLineIds() {
        return lineIds;
    }

    public String getXId() {
        return xId;
    }

    public int getMaxValue() {
        int max = Integer.MIN_VALUE;

        for (List<Integer> list : ordinates) {
            max = Math.max(max, Collections.max(list));
        }

        return max;

    }
}
