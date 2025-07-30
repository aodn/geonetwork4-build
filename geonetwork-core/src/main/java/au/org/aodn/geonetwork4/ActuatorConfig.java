package au.org.aodn.geonetwork4;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Configuration
public class ActuatorConfig {
    // API provided by geonetwork on whether critical dependent resource available.
    protected static final String CRITICAL_HEALTHCHECK = "%s://%s:%s/geonetwork/criticalhealthcheck";

    @Bean
    public HealthIndicator geonetworkHealthIndicator(
            org.springframework.core.env.Environment environment,
            RestTemplate template,
            Setup setup,
            ObjectMapper objectMapper) {

        return () -> {
            String host = "localhost";
            String port = "8080";
            String protocol = "http";

            try {
                ResponseEntity<List<Map<String, String>>> response = template.exchange(
                        String.format(CRITICAL_HEALTHCHECK, protocol, host, port),
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<>() {
                        });

                if (response.getStatusCode().is2xxSuccessful()) {
                    if (response.getBody() != null) {
                        return createHealthResponse(response.getBody());
                    }
                }
            }
            catch(HttpServerErrorException.InternalServerError e) {
                try {
                    List<Map<String, String>> value = objectMapper.readValue(e.getResponseBodyAsString(), new TypeReference<>() {});
                    return createHealthResponse(value);
                }
                catch (JsonProcessingException ex) {
                    // Do nothing
                }
            }
            return Health.down()
                    .withDetail("info", "GeoNetwork4 critical health check status not OK or malform body content")
                    .build();
        };
    }

    protected Health createHealthResponse(List<Map<String, String>> value) {
        // We need to make sure all critical component is health in order to report healthy
        Optional<Map<String, String>> target = value
                                .stream()
                .filter(p -> !(p.containsKey("status") && p.get("status").equalsIgnoreCase("OK")))
                .findFirst();

        return target.<HealthIndicator>map(
                        v -> Health.outOfService()
                                .withDetails(Map.of(
                                        "info", "GeoNetwork4 missing critical service",
                                        "service", v.get("name"),
                                        "msg", v.get("msg")
                                ))::build
                )
                .orElseGet(() -> Health.up()
                        .withDetail("info", "GeoNetwork4")
                        ::build
                ).health();
    }
}
