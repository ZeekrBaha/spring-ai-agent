package com.baha.agent.tools;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

/**
 * Weather tool exposed to the agent. Geocodes a city via Open-Meteo (no API
 * key) then fetches current conditions. Sink with ONE declared network
 * boundary (outbound HTTP); effect is visible in the returned string.
 */
@Component
public class WeatherTool {

    private static final String GEOCODE = "https://geocoding-api.open-meteo.com/v1/search";
    private static final String FORECAST = "https://api.open-meteo.com/v1/forecast";

    private final RestClient http;

    @Autowired
    public WeatherTool(RestClient.Builder builder) {
        this(builder.build());
    }

    WeatherTool(RestClient http) {
        this.http = http;
    }

    @Tool(description = "Get current weather (temperature and conditions) for a city name.")
    public String weather(String city) {
        URI geoUri = UriComponentsBuilder.fromUriString(GEOCODE)
                .queryParam("name", city)
                .queryParam("count", 1)
                .queryParam("language", "en")
                .queryParam("format", "json")
                .build().toUri();

        JsonNode geo = http.get().uri(geoUri).retrieve().body(JsonNode.class);
        JsonNode results = geo == null ? null : geo.get("results");
        if (results == null || !results.isArray() || results.isEmpty()) {
            return "City not found: " + city;
        }
        JsonNode place = results.get(0);
        double lat = place.get("latitude").asDouble();
        double lon = place.get("longitude").asDouble();
        String name = place.path("name").asText(city);
        String country = place.path("country").asText("");

        URI fcUri = UriComponentsBuilder.fromUriString(FORECAST)
                .queryParam("latitude", lat)
                .queryParam("longitude", lon)
                .queryParam("current", "temperature_2m,weather_code")
                .build().toUri();

        JsonNode fc = http.get().uri(fcUri).retrieve().body(JsonNode.class);
        JsonNode current = fc == null ? null : fc.get("current");
        if (current == null) {
            return "Weather unavailable for " + name;
        }
        double temp = current.path("temperature_2m").asDouble();
        int code = current.path("weather_code").asInt();

        String where = country.isEmpty() ? name : name + ", " + country;
        return where + ": " + temp + "°C, " + describe(code);
    }

    private String describe(int code) {
        return switch (code) {
            case 0 -> "Clear sky";
            case 1 -> "Mainly clear";
            case 2 -> "Partly cloudy";
            case 3 -> "Overcast";
            case 45, 48 -> "Fog";
            case 51, 53, 55 -> "Drizzle";
            case 61, 63, 65 -> "Rain";
            case 66, 67 -> "Freezing rain";
            case 71, 73, 75 -> "Snow";
            case 77 -> "Snow grains";
            case 80, 81, 82 -> "Rain showers";
            case 85, 86 -> "Snow showers";
            case 95 -> "Thunderstorm";
            case 96, 99 -> "Thunderstorm with hail";
            default -> "Unknown conditions";
        };
    }
}
