package au.org.aodn.geonetwork4;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpEntity;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.*;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

public class ActuatorConfigTest {

    @Test
    public void verifyHeathReportOnCriticalError() throws JsonProcessingException {
        // Create a mock of RestTemplate
        RestTemplate restTemplate = Mockito.mock(RestTemplate.class);
        ObjectMapper mapper = new ObjectMapper();

        List<Map<String, String>> responseBody = List.of(
                Map.of("name", "CswGetCapabilitiesHealthCheck", "status", "OK"),
                Map.of("name", "DatabaseHealthCheck", "status", "OK"),
                Map.of("name", "IndexHealthCheck",
                        "status", "ERROR",
                        "msg", "Connection refused",
                        "exception", "java.net.ConnectException: Connection refused"
                )
        );

        Mockito.when(
                restTemplate.<List<Map<String,String>>>exchange(
                        eq("http://localhost:8080/geonetwork/criticalhealthcheck"),
                        eq(HttpMethod.GET),
                        isNull(),
                        any(ParameterizedTypeReference.class)
                )
        ).thenThrow(
                HttpServerErrorException.create(
                        null,
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "ERROR",
                        null,
                        mapper.writeValueAsBytes(responseBody), Charset.defaultCharset())
        );

        ActuatorConfig config = new ActuatorConfig();
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("test");

        HealthIndicator indicator = config.geonetworkHealthIndicator(env, restTemplate, null, new ObjectMapper());

        // Verify results, with the exception throw, you will get out of service
        Health health = indicator.health();
        assertEquals(health.getStatus(), Status.OUT_OF_SERVICE);
    }

    @Test
    public void verifyHeathReportOnNormal() throws JsonProcessingException {
        // Create a mock of RestTemplate
        RestTemplate restTemplate = Mockito.mock(RestTemplate.class);
        ObjectMapper mapper = new ObjectMapper();

        List<Map<String, String>> responseBody = List.of(
                Map.of("name", "CswGetCapabilitiesHealthCheck", "status", "OK"),
                Map.of("name", "DatabaseHealthCheck", "status", "OK"),
                Map.of("name", "IndexHealthCheck",
                        "status", "OK"
                )
        );

        Mockito.when(
                restTemplate.<List<Map<String,String>>>exchange(
                        eq("http://localhost:8080/geonetwork/criticalhealthcheck"),
                        eq(HttpMethod.GET),
                        isNull(),
                        any(ParameterizedTypeReference.class)
                )
        ).thenReturn(ResponseEntity.ok().body(responseBody));

        ActuatorConfig config = new ActuatorConfig();
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("test");

        HealthIndicator indicator = config.geonetworkHealthIndicator(env, restTemplate, null, new ObjectMapper());

        // Verify results, with the exception throw, you will get out of service
        Health health = indicator.health();
        assertEquals(health.getStatus(), Status.UP);
    }
}
