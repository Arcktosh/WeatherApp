package com.example.weather;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Controller {
    @FXML
    public AnchorPane AnchorPaneWindow;
    @FXML
    public Pane CurrentPane, Forecast;
    @FXML
    public ListView<HistoryItem> historyListView;
    // Core Fields
    @FXML
    private TextField searchField;
    @FXML
    private ToggleButton fahrenheitToggle, mphToggle;
    @FXML
    public Label WindLabel, HumidityLabel, VisibilityLabel, CurrentWeatherLabel, FeelsLikeLabel;
    @FXML
    public ImageView sunriseImage, sunsetImage;
    @FXML
    private ImageView currentWeatherImage, currentWindSpeedDirection;
    @FXML
    private Label currentLocationLabel, currentTime, currentTemperature, currentWeatherMain, currentFeelsLike, currentTempMin, currentTempMax, currentWindSpeed, currentHumidity, currentVisibility, sunriseTime, sunsetTime;
    //Forecast
    @FXML
    public Label ForecastLabel, forecastDay1, forecastMin1, forecastMax1, forecastDay2, forecastMin2, forecastMax2, forecastDay3, forecastMin3, forecastMax3, forecastDay4, forecastMin4, forecastMax4, forecastDay5, forecastMin5, forecastMax5;
    @FXML
    public ImageView forecastImage1, forecastImage2, forecastImage3, forecastImage4, forecastImage5;

    private final String API_KEY = "73085df9f155c4c2b7a2aa67da0879b9";
    private final String API_URL = "https://api.openweathermap.org/data/2.5";

    @FXML
    private void handleToggleAction() {
        if (searchField.getText().isEmpty()) {
            if (fahrenheitToggle.isSelected()) {
                // Update Toggle to Fahrenheit
                fahrenheitToggle.setText("°F");
            } else {
                // Update Toggle to Celsius
                fahrenheitToggle.setText("°C");
            }
            if (mphToggle.isSelected()) {
                // Update Toggle to mph
                mphToggle.setText("mph");
            } else {
                // Update Toggle to km/h
                mphToggle.setText("km/h");
            }
        } else {
            updateUIWithConversion();
        }
    }

    @FXML
    private void onSearch() {
        String city = searchField.getText();
        // Validate input
        if (city.isEmpty()) {
            showErrorDialog("Invalid Input", "Please enter a city name.");
            return;
        }
        // Fetch weather data
        fetchWeatherData(city);
    }

    public void initialize() {
        historyListView.setCellFactory(lv -> new ListCell<>() {
            private ImageView weatherIcon = new ImageView();
            private Button removeButton = new Button("Remove");
            private HBox content = new HBox();

            {
                Label location = new Label();
                Label timeLabel = new Label();
                Label temperatureLabel = new Label();

                location.setMinWidth(100);
                timeLabel.setMinWidth(150);
                temperatureLabel.setMinWidth(50);

                content.setSpacing(10);
                content.getChildren().addAll(weatherIcon, location, timeLabel, temperatureLabel, removeButton);

                removeButton.setOnAction(event -> getListView().getItems().remove(getItem()));
            }

            @Override
            protected void updateItem(HistoryItem item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    ((Label) content.getChildren().get(1)).setText(item.getLocation());
                    ((Label) content.getChildren().get(2)).setText(item.getSearchTime());
                    ((Label) content.getChildren().get(3)).setText(item.getTemperature());
                    setGraphic(content);
                }
            }
        });
    }

    public void fetchWeatherData(String city) {
        System.out.println("Fetching Weather Data");
        Task<Void> fetchWeatherTask = new Task<>() {
            @Override
            protected Void call() {
                String units = fahrenheitToggle.isSelected() ? "&units=imperial" : "&units=metric";

                // Get Current Weather Data
                String requestUrl = API_URL + "/weather?q=" + city + "&appid=" + API_KEY + units;
                try {
                    HttpClient client = HttpClient.newHttpClient();
                    HttpRequest request = HttpRequest.newBuilder().uri(URI.create(requestUrl)).build();
                    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                    JSONObject jsonResponse = new JSONObject(response.body());
                    if (jsonResponse.getString("cod").equals("200")) {
                        javafx.application.Platform.runLater(() -> {
                            try {
                                setUIElements(jsonResponse);
                            } catch (JSONException e) {
                                System.out.println("Error1:" + e);
                                showErrorDialog("Something Went Wrong", "Error parsing the weather data.");
                            }
                        });
                    }

                    // Get Forecast Data
                    double lat = jsonResponse.getJSONObject("coord").getDouble("lat");
                    double lon = jsonResponse.getJSONObject("coord").getDouble("lon");
                    String forecastUrl = API_URL + "/forecast?lat=" + lat + "&lon=" + lon + "&appid=" + API_KEY + units;
                    request = HttpRequest.newBuilder().uri(URI.create(forecastUrl)).build();
                    response = client.send(request, HttpResponse.BodyHandlers.ofString());

                    JSONObject jsonResponseForecast = new JSONObject(response.body());
                    if (jsonResponse.getString("cod").equals("200")) {
                        javafx.application.Platform.runLater(() -> {
                            try {
                                setForecastElements(jsonResponseForecast);
                            } catch (JSONException e) {
                                System.out.println("Error2:" + e);
                            }
                        });
                    }
                } catch (IOException | InterruptedException | JSONException e) {
                    System.out.println("Error3:" + e.getMessage());
                    showErrorDialog("Unknown City", "City couldn't be found, Please try again.");
                }
                return null;
            }
        };
        new Thread(fetchWeatherTask).start();
    }

    private void setUIElements(JSONObject jsonResponse) throws JSONException {
        System.out.println("Setting Weather String Data");
        int timeZone = jsonResponse.getInt("timezone");
        String temp = convertTemperatureValue(jsonResponse.getJSONObject("main").getDouble("temp") + "", true);
        String weatherDescription = jsonResponse.getJSONArray("weather").getJSONObject(0).getString("main");
        String feelsLike = convertTemperatureValue(jsonResponse.getJSONObject("main").getDouble("feels_like") + "", true);
        String tempMin = convertTemperatureValue(jsonResponse.getJSONObject("main").getDouble("temp_min") + "", true);
        String tempMax = convertTemperatureValue(jsonResponse.getJSONObject("main").getDouble("temp_max") + "", true);
        String windSpeed = convertWindValue(jsonResponse.getJSONObject("wind").getDouble("speed"), true);
        double windDirection = jsonResponse.getJSONObject("wind").getDouble("deg");
        String humidity = jsonResponse.getJSONObject("main").getInt("humidity") + "%";
        String time = convertUnixTimeToReadableFormat(jsonResponse.getLong("dt"), timeZone);
        String sunrise = convertUnixTimeToReadableFormat(jsonResponse.getJSONObject("sys").getLong("sunrise"), timeZone);
        String sunset = convertUnixTimeToReadableFormat(jsonResponse.getJSONObject("sys").getLong("sunset"), timeZone);
        String visibility = jsonResponse.getInt("visibility") / 1000.0 + (mphToggle.isSelected() ? " mi" : " km");
        String city = jsonResponse.getString("name");
        getBackgroundColorBasedOnTime(time);
        currentLocationLabel.setText(city);
        currentTime.setText(time);
        currentTemperature.setText(temp);
        currentWeatherMain.setText(weatherDescription);
        currentFeelsLike.setText(feelsLike);
        currentTempMin.setText("Min: " + tempMin);
        currentTempMax.setText("Max: " + tempMax);
        currentWindSpeed.setText(windSpeed);
        currentHumidity.setText(humidity);
        currentVisibility.setText(visibility);
        sunsetTime.setText(sunset);
        sunriseTime.setText(sunrise);
        currentWindSpeedDirection.setRotate(windDirection);
        try {
            currentWeatherImage.setImage(loadWeatherImage(weatherDescription.toLowerCase()));
        } catch (Exception e) {
            showErrorDialog("Image Loading Error", "Failed to load weather image.");
        }
        addHistoryLocation(city, temp); // Add to History
        historyListView.setVisible(true);
        System.out.println("Data Set");
        CurrentPane.setVisible(true);
    }

    private void setForecastElements(JSONObject jsonResponseForecast) throws JSONException {
        System.out.println("Setting Weather Forecast String Data");
        List<WeatherDataAggregator.DailyWeatherInfo> forecastData = WeatherDataAggregator.aggregateWeatherData(jsonResponseForecast);
        Label[] dayLabels = {forecastDay1, forecastDay2, forecastDay3, forecastDay4, forecastDay5};
        Label[] minTempLabels = {forecastMin1, forecastMin2, forecastMin3, forecastMin4, forecastMin5};
        Label[] maxTempLabels = {forecastMax1, forecastMax2, forecastMax3, forecastMax4, forecastMax5};
        ImageView[] forecastImages = {forecastImage1, forecastImage2, forecastImage3, forecastImage4, forecastImage5};

        for (int i = 0; i < forecastData.size() && i < 5; i++) {
            WeatherDataAggregator.DailyWeatherInfo info = forecastData.get(i);

            dayLabels[i].setText(info.date);
            minTempLabels[i].setText(convertTemperatureValue(Double.toString(info.minTemp), true));
            maxTempLabels[i].setText(convertTemperatureValue(Double.toString(info.maxTemp), true));
            try {
                forecastImages[i].setImage(loadWeatherImage(info.weather.toLowerCase()));
            } catch (Exception e) {
                forecastImages[i].setImage(loadWeatherImage("clear"));
            }
        }
        System.out.println("Forecast Data Set");
        Forecast.setVisible(true);
    }

    private void addHistoryLocation(String location, String temperature) {
        String currentTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        HistoryItem newItem = new HistoryItem(location, currentTime, temperature);
        historyListView.getItems().add(0, newItem); // Add to the top of the list
    }

    // Update the color of the background based on the time of day
    public void getBackgroundColorBasedOnTime(String time) {
        // Parse the hour from the input string
        int hour = Integer.parseInt(time.split(":")[0]);

        if (hour < 6) {
            AnchorPaneWindow.setBackground(new Background(new BackgroundFill(Color.DARKBLUE, new CornerRadii(0), Insets.EMPTY))); // Night
        } else if (hour < 12) {
            AnchorPaneWindow.setBackground(new Background(new BackgroundFill(Color.LIGHTBLUE, new CornerRadii(0), Insets.EMPTY))); // Morning
        } else if (hour < 18) {
            AnchorPaneWindow.setBackground(new Background(new BackgroundFill(Color.ORANGE, new CornerRadii(0), Insets.EMPTY))); // Afternoon
        } else {
            AnchorPaneWindow.setBackground(new Background(new BackgroundFill(Color.PURPLE, new CornerRadii(0), Insets.EMPTY))); // Evening
        }
    }


    private Image loadWeatherImage(String path) {
        try {
            return new Image(getClass().getResourceAsStream("images/weather/" + path + ".png"));
        } catch (Exception e) {
            showErrorDialog("Resource Loading Error", "Could not load image: " + path);
            Image image = new Image(Objects.requireNonNull(getClass().getResourceAsStream("images/weather/clear.png")));
            return image;
        }
    }

    private void updateUIWithConversion() {
        if (fahrenheitToggle.isSelected()) {
            // Update Toggle to Fahrenheit
            fahrenheitToggle.setText("°F");
        } else {
            // Update Toggle to Celsius
            fahrenheitToggle.setText("°C");
        }
        if (mphToggle.isSelected()) {
            // Update Toggle to mph
            mphToggle.setText("mph");
        } else {
            // Update Toggle to km/h
            mphToggle.setText("km/h");
        }
        // Convert the Min and Max temperature if necessary
        currentTempMin.setText("Min: " + convertTemperatureValue(currentTempMin.getText().replace("Min: ", ""), false));
        currentTempMax.setText("Max: " + convertTemperatureValue(currentTempMax.getText().replace("Max: ", ""), false));

        // Current Winds
        currentWindSpeed.setText(convertWindValue(extractDouble(currentWindSpeed.getText()), false));

        // Current visibility
        currentVisibility.setText(convertVisibility(currentVisibility.getText()));

        // Temperatures
        Label[] temperatureLabels = {currentTemperature, currentFeelsLike, forecastDay1, forecastDay2, forecastDay3, forecastDay4, forecastDay5, forecastMin1, forecastMin2, forecastMin3, forecastMin4, forecastMin5, forecastMax1, forecastMax2, forecastMax3, forecastMax4, forecastMax5};

        // Loop through the array of temperatureLabels and convert each
        for (Label label : temperatureLabels) {
            String newText = convertTemperatureValue(label.getText(), false);
            label.setText(newText);
        }
    }

    // Conversion methods
    private Double convertToFahrenheit(Double celsius) {
        return (celsius * 9 / 5) + 32; // To Fahrenheit: (°C × 9/5) + 32 = °F
    }

    private Double convertToCelsius(Double fahrenheit) {
        return (fahrenheit - 32) * 5 / 9; // To Celsius: (°F − 32) × 5/9 = °C
    }

    private Double convertToMph(Double kmh) {
        return (kmh / 1.609); // KmpH to Miles per Hour: km/h ÷ 1.609 = mph
    }

    private Double convertToKmh(Double mph) {
        return (mph * 1.609); // mph to Kilometers per Hour: mph × 1.609 = km/h
    }

    private String convertTemperatureValue(String value, Boolean set) {
        // Extract the numeric value from the input string
        double numericValue = extractDouble(value);
        String newTemp = value; // Default to original value

        // Determine the target unit based on the toggle state
        String targetUnit = fahrenheitToggle.isSelected() ? "°F" : "°C";

        // Check if conversion is needed based on the presence of the target unit in the original value
        if (set) {
            // Decorate the numeric value with the target unit without conversion
            newTemp = String.format("%.1f", numericValue) + targetUnit;
        } else if ((value.contains("°F") && fahrenheitToggle.isSelected()) || (value.contains("°C") && !fahrenheitToggle.isSelected())) {
            // If the value already contains the target unit, no conversion is needed
            return value;
        } else {
            // Conversion is needed
            if (fahrenheitToggle.isSelected() && value.contains("°C")) {
                // Convert to Fahrenheit
                numericValue = convertToFahrenheit(numericValue);
            } else if (!fahrenheitToggle.isSelected() && value.contains("°F")) {
                // Convert to Celsius
                numericValue = convertToCelsius(numericValue);
            }
            newTemp = String.format("%.1f", numericValue) + targetUnit;
        }
        return newTemp;
    }

    private String convertWindValue(Double value, Boolean set) {
        System.out.println("convertWindValue: " + value + ", mphToggle: " + mphToggle.isSelected());
        String newWindSpeed;
        if (mphToggle.isSelected()) {
            // Convert to mph
            newWindSpeed = set ? value.intValue() + " mph" : convertToMph(value).intValue() + " mph";
        } else {
            // convert to km/h
            newWindSpeed = set ? value.intValue() + " km/h" : convertToKmh(value).intValue() + " km/h";
        }
        return newWindSpeed;
    }

    private String convertVisibility(String visibilityString) {
        // Check if the string ends with "km" or "mi"
        boolean isKm = visibilityString.endsWith("km");
        boolean isMiles = visibilityString.endsWith("mi");
        // Extract the numeric part of the visibility string
        double value = extractDouble(visibilityString);
        // Perform conversion based on the current state of mphToggle and the unit in the string
        if (isKm && mphToggle.isSelected()) {
            // Convert from kilometers to miles
            double miles = value * 0.621371;
            return String.format("%.1f", miles) + " mi";
        } else if (isMiles && !mphToggle.isSelected()) {
            // Convert from miles to kilometers
            double kilometers = value / 0.621371;
            return String.format("%.1f", kilometers) + " km";
        } else {
            // No conversion needed, return the original string
            return visibilityString;
        }
    }

    public static String convertUnixTimeToReadableFormat(long unixTimestamp, int timezoneOffsetInSeconds) {
        // Convert the Unix timestamp to an Instant
        Instant instant = Instant.ofEpochSecond(unixTimestamp);

        // Calculate the ZoneOffset using the timezone offset in seconds
        ZoneOffset zoneOffset = ZoneOffset.ofTotalSeconds(timezoneOffsetInSeconds);

        // Convert Instant to ZonedDateTime using the ZoneOffset
        ZonedDateTime zonedDateTime = instant.atZone(ZoneId.ofOffset("UTC", zoneOffset));

        // Format the ZonedDateTime to a readable string (e.g., HH:mm)
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        return zonedDateTime.format(formatter);
    }

    public double extractDouble(String text) {
        Matcher matcher = Pattern.compile("[-+]?\\d*\\.?\\d+").matcher(text);
        if (matcher.find()) {
            return Double.parseDouble(matcher.group());
        }
        return 0; // Default to 0 if no number is found
    }

    // Error Handling
    private void showErrorDialog(String header, String content) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(header);
            alert.setContentText(content);
            alert.showAndWait();
        });
    }
}