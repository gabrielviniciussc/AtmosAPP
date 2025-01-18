import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

public class AtmosAppApi {
    // Fetch weather data for given location
    public static JSONObject getWeatherData(String locationName){
        // Get location coordinates using the geolocation API
        JSONArray locationData = getLocationData(locationName);

        // Verificar se a resposta da geolocalização é válida
        if (locationData == null || locationData.isEmpty()) {
            System.out.println("Error: Could not find location data for " + locationName);
            return null;
        }

        // Extract latitude and longitude data
        JSONObject location = (JSONObject) locationData.get(0);
        double latitude = (double) location.get("latitude");
        double longitude = (double) location.get("longitude");

        // Build API request URL with location coordinates
        String urlString = "https://api.open-meteo.com/v1/forecast?" +
                "latitude=" + latitude + "&longitude=" + longitude +
                "&hourly=temperature_2m,relativehumidity_2m,weathercode,windspeed_10m&timezone=America%2FLos_Angeles";

        try{
            // Call API and get response
            HttpURLConnection conn = fetchApiResponse(urlString);

            // Check for response status (200 = success)
            if (conn.getResponseCode() != 200) {
                System.out.println("Error: Could not connect to API");
                return null;
            }

            // Store resulting JSON data
            StringBuilder resultJson = new StringBuilder();
            Scanner scanner = new Scanner(conn.getInputStream());
            while (scanner.hasNext()) {
                // Read and store into the string builder
                resultJson.append(scanner.nextLine());
            }

            // Close scanner and connection
            scanner.close();
            conn.disconnect();

            // Parse through our data
            JSONParser parser = new JSONParser();
            JSONObject resultJsonObj = (JSONObject) parser.parse(String.valueOf(resultJson));

            // Retrieve hourly data
            JSONObject hourly = (JSONObject) resultJsonObj.get("hourly");

            // We want to get the current hour's data
            // Get index of current hour
            JSONArray time = (JSONArray) hourly.get("time");
            int index = findIndexOfCurrentTime(time);

            // Get temperature
            JSONArray temperatureData = (JSONArray) hourly.get("temperature_2m");
            double temperature = (double) temperatureData.get(index);

            // Get weather code
            JSONArray weathercode = (JSONArray) hourly.get("weathercode");
            String weatherCondition = convertWeatherCode((long) weathercode.get(index));

            // Get humidity
            JSONArray relativeHumidity = (JSONArray) hourly.get("relativehumidity_2m");
            long humidity = (long) relativeHumidity.get(index);

            // Get windspeed
            JSONArray windspeedData = (JSONArray) hourly.get("windspeed_10m");
            double windspeed = (double) windspeedData.get(index);

            // Build weather JSON data object for frontend access
            JSONObject weatherData = new JSONObject();
            weatherData.put("temperature", temperature);
            weatherData.put("weather_condition", weatherCondition);
            weatherData.put("humidity", humidity);
            weatherData.put("windspeed", windspeed);

            return weatherData;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    // Retrieves geographic coordinates for given location name
    public static JSONArray getLocationData(String locationName) {
        // Replace any whitespace in location name to + to adhere to API's request format
        locationName = locationName.replaceAll(" ", "+");

        // Build API URL with location parameter
        String urlString = "https://geocoding-api.open-meteo.com/v1/search?name=" +
                locationName + "&count=10&language=en&format=json";

        try {
            // Call API and get a response
            HttpURLConnection conn = fetchApiResponse(urlString);

            // Check response status (200 = success)
            if (conn.getResponseCode() != 200) {
                System.out.println("Error: Could not connect to API");
                return null;
            } else {
                // Store API results
                StringBuilder resultJson = new StringBuilder();
                Scanner scanner = new Scanner(conn.getInputStream());

                while (scanner.hasNext()) {
                    resultJson.append(scanner.nextLine());
                }

                scanner.close();
                conn.disconnect();

                // Parse the JSON string into a JSON object
                JSONParser parser = new JSONParser();
                JSONObject resultsJsonObj = (JSONObject) parser.parse(String.valueOf(resultJson));

                // Get the list of location data returned from the API
                JSONArray locationData = (JSONArray) resultsJsonObj.get("results");

                // Check if location data is present
                if (locationData == null || locationData.isEmpty()) {
                    System.out.println("No location data found for: " + locationName);
                }

                return locationData;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    private static HttpURLConnection fetchApiResponse(String urlString) {
        try {
            // Attempt to create connection
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            // Set request method to GET
            conn.setRequestMethod("GET");

            // Connect to API
            conn.connect();
            return conn;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    private static int findIndexOfCurrentTime(JSONArray timeList) {
        String currentTime = getCurrentTime();

        // Iterate through the time list and find current time
        for (int i = 0; i < timeList.size(); i++) {
            String time = (String) timeList.get(i);
            if (time.equalsIgnoreCase(currentTime)) {
                // Return the index of current time
                return i;
            }
        }

        return 0; // Default index (should never be reached if current time is properly found)
    }

    private static String getCurrentTime() {
        // Get current date and time
        LocalDateTime currentDateTime = LocalDateTime.now();

        // Format date to be "2023-09-02T00:00" (this format is required by the API)
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH':00'");

        // Format and return the current time
        return currentDateTime.format(formatter);
    }

    // Convert the weather code to a human-readable format
    private static String convertWeatherCode(long weathercode) {
        String weatherCondition = "";
        if (weathercode == 0L) {
            // Clear
            weatherCondition = "Clear";
        } else if (weathercode > 0L && weathercode <= 3L) {
            // Cloudy
            weatherCondition = "Cloudy";
        } else if ((weathercode >= 51L && weathercode <= 67L)
                || (weathercode >= 80L && weathercode <= 99L)) {
            // Rain
            weatherCondition = "Rain";
        } else if (weathercode >= 71L && weathercode <= 77L) {
            // Snow
            weatherCondition = "Snow";
        }

        return weatherCondition;
    }
}
