package com.example.weather;

public class HistoryItem {
    private String location;
    private String searchTime;
    private String temperature;


    public HistoryItem(String location, String searchTime, String temperature) {
        this.location = location;
        this.searchTime = searchTime;
        this.temperature = temperature;
    }

    // Getters and setters
    public String getLocation() {
        return location;
    }

    public String getSearchTime() {
        return searchTime;
    }

    public String getTemperature() {
        return temperature;
    }

}
