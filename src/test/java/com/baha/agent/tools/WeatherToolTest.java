package com.baha.agent.tools;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class WeatherToolTest {

    private WeatherTool toolWith(MockRestServiceServer[] serverOut) {
        RestClient.Builder builder = RestClient.builder();
        serverOut[0] = MockRestServiceServer.bindTo(builder).build();
        return new WeatherTool(builder.build());
    }

    @Test
    void returnsCurrentWeatherForKnownCity() {
        MockRestServiceServer[] s = new MockRestServiceServer[1];
        WeatherTool tool = toolWith(s);

        s[0].expect(requestTo(containsString("geocoding-api.open-meteo.com")))
                .andRespond(withSuccess(
                        "{\"results\":[{\"name\":\"Paris\",\"latitude\":48.85,\"longitude\":2.35,\"country\":\"France\"}]}",
                        MediaType.APPLICATION_JSON));
        s[0].expect(requestTo(containsString("api.open-meteo.com/v1/forecast")))
                .andRespond(withSuccess(
                        "{\"current\":{\"temperature_2m\":14.2,\"weather_code\":3}}",
                        MediaType.APPLICATION_JSON));

        String result = tool.weather("Paris");

        assertThat(result).contains("Paris");
        assertThat(result).contains("14.2");
        assertThat(result).containsIgnoringCase("overcast");
        s[0].verify();
    }

    @Test
    void unknownCityReturnsClearError() {
        MockRestServiceServer[] s = new MockRestServiceServer[1];
        WeatherTool tool = toolWith(s);

        s[0].expect(requestTo(containsString("geocoding-api.open-meteo.com")))
                .andRespond(withSuccess("{\"results\":[]}", MediaType.APPLICATION_JSON));

        assertThat(tool.weather("Nowheresville")).startsWith("City not found");
        s[0].verify();
    }
}
