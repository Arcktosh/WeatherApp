package com.example.weather;

public class HistoryItem {
    private String location;
    private String searchTime;
    private String temperature;
    private String iconPath;

    public HistoryItem(String location, String searchTime, String temperature, String iconPath) {
        this.location = location;
        this.searchTime = searchTime;
        this.temperature = temperature;
        this.iconPath = iconPath;
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

    public String getIconPath() {
        return iconPath;
    }
}
