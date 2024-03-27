package com.example.weather;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public class Controller {
    @FXML
    public ListView<HistoryItem> historyListView;
    // Core Fields
    @FXML
    private TextField searchField;
    @FXML
    private ToggleButton celsiusToggle, mphToggle;
    @FXML
    public Label WindLabel, HumidityLabel, VisibilityLabel, ForecastLabel, CurrentWeatherLabel, FeelsLikeLabel;
    @FXML
    public ImageView sunriseImage, sunsetImage;
    @FXML
    private ImageView currentWeatherImage, currentWindSpeedDirection;
    @FXML
    private Label currentLocationLabel, currentTime, currentTemperature, temperatureGetter, currentWeatherMain, currentFeelsLike, currentTempMin, currentTempMax, currentWindSpeed, windSpeedGetter, currentHumidity, currentVisibility, sunriseTime, sunsetTime;
    //Forecast Labels
    @FXML
    public Label forecastDay1, forecastMin1, forecastMax1, forecastDay2, forecastMin2, forecastMax2, forecastDay3, forecastMin3, forecastMax3, forecastDay4, forecastMin4, forecastMax4, forecastDay5, forecastMin5, forecastMax5;
    //Forecast Images
    @FXML
    public ImageView forecastMax1Image, forecastMin1Image, forecastImage1, forecastImage2, forecastImage3, forecastImage4, forecastImage5;

    @FXML
    private void handleToggleAction() {
        updateUIWithConversion();
    }

    @FXML
    private void onSearch() {
        String city = searchField.getText();
        // Validate input
        if (city.isEmpty()) {
            System.out.println("Location Empty");
            showErrorDialog("Invalid Input", "Please enter a city name.");
            return;
        }
        // Fetch weather data
        fetchWeatherData(city);
    }

    public void initialize() {
        historyListView.setCellFactory(lv -> new ListCell<HistoryItem>() {
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
                    weatherIcon.setImage(new Image(item.getIconPath()));
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
            protected Void call() throws Exception {
                String units = celsiusToggle.isSelected() ? "&units=metric" : "&units=imperial";
                try {
                    // Get Current Weather Data
                    String apiKey = "73085df9f155c4c2b7a2aa67da0879b9";
                    String requestUrl = "https://api.openweathermap.org/data/2.5/weather?q=" + city + "&appid=" + apiKey + units;

                    HttpClient client = HttpClient.newHttpClient();
                    HttpRequest request = HttpRequest.newBuilder().uri(URI.create(requestUrl)).build();
                    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                    JSONObject jsonResponse = new JSONObject(response.body());
                    if (jsonResponse.getString("cod") == "200") {
                        javafx.application.Platform.runLater(() -> {
                            try {

                                setUIElements(jsonResponse);
//                        addHistoryLocation(city, "20");
                            } catch (JSONException e) {
                                System.out.println("Error:" + e);
                                showErrorDialog("Something Went Wrong", "Error parsing the weather data.");
                            }
                        });
                    } else {
                        showErrorDialog("Something Went Wrong", "Error collecting the weather data.");
                    }

                    // Get Forecast Data
                    double lat = jsonResponse.getJSONObject("coord").getDouble("lat");
                    double lon = jsonResponse.getJSONObject("coord").getDouble("lon");
                    String forecastUrl = "https://api.openweathermap.org/data/2.5/forecast?lat=" + lat + "&lon=" + lon + "&appid=" + apiKey;
                    request = HttpRequest.newBuilder().uri(URI.create(forecastUrl)).build();
                    response = client.send(request, HttpResponse.BodyHandlers.ofString());

                    JSONObject jsonResponseForecast = new JSONObject(response.body());
                    if (jsonResponse.getString("cod") == "200") {
                        javafx.application.Platform.runLater(() -> {
                            try {
                                setForecastElements(jsonResponseForecast);
                            } catch (JSONException e) {
                                System.out.println("Error:" + e);
                                showErrorDialog("Something Went Wrong", "Error parsing the forecast data.");
                            }
                        });
                    } else {
                        showErrorDialog("Something Went Wrong", "Error collecting the weather's forecast data.");
                    }
                } catch (JSONException e) {
                    showErrorDialog("Something Went Wrong", "Error collecting the weather's forecast data.");
                }

                return null;
            }
        };

        new Thread(fetchWeatherTask).start();
    }

    private void setUIElements(JSONObject jsonResponse) throws JSONException {
        System.out.println("Setting Weather String Data");
        String unitSymbol = celsiusToggle.isSelected() ? "°C" : "°F";
        Integer timeZone = jsonResponse.getInt("timezone");
        String temp = jsonResponse.getJSONObject("main").getDouble("temp") + unitSymbol;
        String weatherDescription = jsonResponse.getJSONArray("weather").getJSONObject(0).getString("main");
        String feelsLike = jsonResponse.getJSONObject("main").getDouble("feels_like") + unitSymbol;
        String tempMin = jsonResponse.getJSONObject("main").getDouble("temp_min") + unitSymbol;
        String tempMax = jsonResponse.getJSONObject("main").getDouble("temp_max") + unitSymbol;
        Double windSpeed = jsonResponse.getJSONObject("wind").getDouble("speed");
        Double windDirection = jsonResponse.getJSONObject("wind").getDouble("deg");
        String humidity = jsonResponse.getJSONObject("main").getInt("humidity") + "%";
        String time = convertUnixTimeToReadableFormat(jsonResponse.getLong("dt"), timeZone);
        String sunrise = convertUnixTimeToReadableFormat(jsonResponse.getJSONObject("sys").getLong("sunrise"), timeZone);
        String sunset = convertUnixTimeToReadableFormat(jsonResponse.getJSONObject("sys").getLong("sunset"), timeZone);
        String visibility = jsonResponse.getInt("visibility") / 1000.0 + (mphToggle.isSelected() ? " mi" : " km");
        String city = jsonResponse.getString("name");

        currentLocationLabel.setText("in " + city);
        currentTime.setText(time);
        currentTemperature.setText(temp);
        currentWeatherMain.setText(weatherDescription);
        currentFeelsLike.setText(feelsLike);
        currentTempMin.setText("Min: " + tempMin);
        currentTempMax.setText("Max: " + tempMax);
        currentWindSpeed.setText(windSpeed.toString());
        currentHumidity.setText(humidity);
        currentVisibility.setText(visibility);
        temperatureGetter.setText(unitSymbol);
        sunsetTime.setText(sunset);
        sunriseTime.setText(sunrise);
        windSpeedGetter.setText(mphToggle.isSelected() ? " m/s" : " mi/h");
        currentWindSpeedDirection.setRotate(windDirection);
        try {
            System.out.println("Set Image");
            String imagePath = "/images/weather/" + weatherDescription + ".png";
            System.out.println(imagePath);
            Image image = new Image(getClass().getResourceAsStream(imagePath));
            System.out.println(image);
            currentWeatherImage.setImage(image);
            System.out.println("Data Set");
        } catch (Exception e) {
            showErrorDialog("Image Loading Error", "Failed to load weather image.");
        }
    }

    private void setForecastElements(JSONObject jsonResponse) throws JSONException {
        System.out.println("Setting Forecast String Data");
        String unitSymbol = celsiusToggle.isSelected() ? "°C" : "°F";
        int timeZone = jsonResponse.getInt("timezone");
        String temp = jsonResponse.getJSONObject("main").getDouble("temp") + unitSymbol;
        String weatherDescription = jsonResponse.getJSONArray("weather").getJSONObject(0).getString("main");
        String feelsLike = jsonResponse.getJSONObject("main").getDouble("feels_like") + unitSymbol;
        String tempMin = jsonResponse.getJSONObject("main").getDouble("temp_min") + unitSymbol;
        String tempMax = jsonResponse.getJSONObject("main").getDouble("temp_max") + unitSymbol;
        Double windSpeed = jsonResponse.getJSONObject("wind").getDouble("speed");
        Double windDirection = jsonResponse.getJSONObject("wind").getDouble("deg");
        String humidity = jsonResponse.getJSONObject("main").getInt("humidity") + "%";
        String sunrise = convertUnixTimeToReadableFormat(jsonResponse.getJSONObject("sys").getLong("sunrise"), timeZone);
        String sunset = convertUnixTimeToReadableFormat(jsonResponse.getJSONObject("sys").getLong("sunset"), timeZone);
        String visibility = jsonResponse.getInt("visibility") / 1000.0 + (mphToggle.isSelected() ? " mi" : " km");
        String city = jsonResponse.getString("name");

        currentLocationLabel.setText("in " + city);
        currentTemperature.setText(temp);
        currentWeatherMain.setText(weatherDescription);
        currentFeelsLike.setText(feelsLike);
        currentTempMin.setText("Min: " + tempMin);
        currentTempMax.setText("Max: " + tempMax);
        currentWindSpeed.setText(windSpeed.toString());
        currentHumidity.setText(humidity);
        currentVisibility.setText(visibility);
        temperatureGetter.setText(unitSymbol);
        sunsetTime.setText(sunset);
        sunriseTime.setText(sunrise);
        windSpeedGetter.setText(mphToggle.isSelected() ? " m/s" : " mi/h");
        currentWindSpeedDirection.setRotate(windDirection);
        try {
            System.out.println("Set Image");
            String imagePath = "/images/weather/" + weatherDescription + ".png";
            System.out.println(imagePath);
            Image image = new Image(getClass().getResourceAsStream(imagePath));
            System.out.println(image);
            currentWeatherImage.setImage(image);
            System.out.println("Data Set");
        } catch (Exception e) {
            showErrorDialog("Image Loading Error", "Failed to load weather image.");
        }
    }

    //TODO: maintain a list of previous searches that the user can revisit. This involves updating a ListView and storing this history between sessions using a simple text file.
    private void addHistoryLocation(String location, String temperature) {
        String iconPath = "@images/weather/barometer.png";
        String currentTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        HistoryItem newItem = new HistoryItem(location, currentTime, temperature, iconPath);
        historyListView.getItems().add(0, newItem); // Add to the top of the list
    }

    public void removeHistoryLocation(ActionEvent actionEvent) {
//TODO: Remove HistoryLocation from list
    }


    private Image loadImage(String path) {
        try {
            return new Image(Objects.requireNonNull(getClass().getResourceAsStream(path)));
        } catch (Exception e) {
            showErrorDialog("Resource Loading Error", "Could not load image: " + path);
            return null; // TODO: Return a default image
        }
    }

    private void updateUIWithConversion() {
        // Convert the temperature if necessary
        String newTemp;
        String newWindSpeed;

        if (celsiusToggle.isSelected()) {
            // Convert to Celsius if not already
            newTemp = convertToCelsius(currentTemperature.getText()) + "°C";
            // Update Toggle to Celsius
            celsiusToggle.setText("°C");
        } else {
            // Convert to Fahrenheit if not already
            newTemp = convertToFahrenheit(currentTemperature.getText()) + "°F";
            // Update Toggle to Fahrenheit
            celsiusToggle.setText("°F");
        }
        if (mphToggle.isSelected()) {
            // Convert to mph if not already
            newWindSpeed = convertToMph(currentWindSpeed.getText()) + " mph";
            // Update Toggle to mph
            mphToggle.setText("mph");
        } else {
            // Convert to km/h if not already
            newWindSpeed = convertToKmh(currentWindSpeed.getText()) + " km/h";
            // Update Toggle to km/h
            mphToggle.setText("km/h");
        }

        // Update UI with new values
        currentTemperature.setText(newTemp);
        currentWindSpeed.setText(newWindSpeed);
    }

    // Conversion methods
    private String convertToFahrenheit(String celsiusTemp) {
        // Extract numerical value from the string, convert and return as string
        double temp = Double.parseDouble(celsiusTemp.replaceAll("[^\\d.]", ""));
        double fahrenheit = (temp * 9 / 5) + 32; // To Fahrenheit: (°C × 9/5) + 32 = °F
        return String.format("%.1f", fahrenheit);
    }

    private String convertToCelsius(String fahrenheitTemp) {
        double temp = Double.parseDouble(fahrenheitTemp.replaceAll("[^\\d.]", ""));
        double celsius = (temp - 32) * 5 / 9; // To Celsius: (°F − 32) × 5/9 = °C
        return String.format("%.1f", celsius);
    }

    private String convertToMph(String kmhSpeed) {
        double kmh = Double.parseDouble(kmhSpeed.replaceAll("[^\\d.]", ""));
        double fahrenheit = kmh / 1.609; // To Miles per Hour: km/h ÷ 1.609 = mph
        return String.format("%.1f", fahrenheit);
    }

    private String convertToKmh(String mphSpeed) {
        double mph = Double.parseDouble(mphSpeed.replaceAll("[^\\d.]", ""));
        double kilometers = mph * 1.609; // To Kilometers per Hour: mph × 1.609 = km/h
        return String.format("%.1f", kilometers);
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