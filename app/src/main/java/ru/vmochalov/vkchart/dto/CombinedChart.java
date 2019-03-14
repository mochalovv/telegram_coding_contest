package ru.vmochalov.vkchart.dto;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Created by Vladimir Mochalov on 10.03.2019.
 */
public class CombinedChart {

    String xId = null;
    List<String> lineIds = null; //new ArrayList<>();

    List<Date> abscissa = new ArrayList<>();
    List<List<Integer>> ordinates = null; //new HashMap<>();
    List<String> labels = null; //new ArrayList<>();
    List<Integer> colors = null; //new ArrayList<>();

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
    }

    public List<Date> getAbscissa() {
        return abscissa;
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
