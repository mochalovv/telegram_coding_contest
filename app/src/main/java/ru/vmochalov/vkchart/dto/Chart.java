package ru.vmochalov.vkchart.dto;

import java.util.*;

/**
 * Created by Vladimir Mochalov on 10.03.2019.
 */
public class Chart {

    private String name;
    private TreeMap<Date, Integer> points; // timestamp, value

    public static Chart getSampleChart() {
        TreeMap<Date, Integer> points = new TreeMap<>();

        Calendar calendar = Calendar.getInstance();

        Random rand = new Random();

        for (int i = 0; i < 10; i++) {
            calendar.add(Calendar.DAY_OF_YEAR, -1);
            Date nextDate = calendar.getTime();

            int value = 50 + rand.nextInt(25);

            points.put(nextDate, value);
        }

        return new Chart("Sample Chart", points);
    }

    public Chart(String name, TreeMap<Date, Integer> points) {
        this.name = name;
        this.points = points;
    }

    public String getName() {
        return name;
    }

    public Map<Date, Integer> getPoints() {
        return points;
    }


}
