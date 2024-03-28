package com.example.weather;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class WeatherDataAggregator {
    public static List<DailyWeatherInfo> aggregateWeatherData(JSONObject jsonObject) throws JSONException {
        JSONArray list = jsonObject.getJSONArray("list");

        List<DailyWeatherInfo> dailyWeatherInfos = new ArrayList<>();
        DailyWeatherInfo currentDayInfo = null;

        for (int i = 0; i < 40; i++) { // Loop through the first 40 entries (5 days)
            JSONObject entry = list.getJSONObject(i);
            String dtTxt = entry.getString("dt_txt");
            String date = dtTxt.split(" ")[0]; // Extracting the date part

            double tempMin = entry.getJSONObject("main").getDouble("temp_min");
            double tempMax = entry.getJSONObject("main").getDouble("temp_max");
            String weatherMain = entry.getJSONArray("weather").getJSONObject(0).getString("main");

            // If currentDayInfo is null or the date has changed, create a new DailyWeatherInfo
            if (currentDayInfo == null || !currentDayInfo.date.equals(date)) {
                if (currentDayInfo != null) {
                    dailyWeatherInfos.add(currentDayInfo); // Save the previous day's info
                }
                currentDayInfo = new DailyWeatherInfo(date, weatherMain, tempMin, tempMax);
            } else {
                // Update the existing DailyWeatherInfo with new min/max temperatures
                currentDayInfo.updateTemperature(tempMin, tempMax);
            }
        }

        // Don't forget to add the last day's info
        if (currentDayInfo != null) {
            dailyWeatherInfos.add(currentDayInfo);
        }

        return dailyWeatherInfos;
    }
    public static class DailyWeatherInfo {
        String date;
        String weather;
        double minTemp;
        double maxTemp;

        public DailyWeatherInfo(String date, String weather, double minTemp, double maxTemp) {
            this.date = date;
            this.weather = weather;
            this.minTemp = minTemp;
            this.maxTemp = maxTemp;
        }

        public void updateTemperature(double tempMin, double tempMax) {
            if (tempMin < this.minTemp) {
                this.minTemp = tempMin;
            }
            if (tempMax > this.maxTemp) {
                this.maxTemp = tempMax;
            }
        }
    }
}
