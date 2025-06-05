package au.org.aodn.geonetwork4;

import au.org.aodn.geonetwork_api.openapi.api.helper.SiteHelper;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
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
            Setup setup) {

        return () -> {
            String[] profiles = environment.getActiveProfiles();

            String host;
            String port;
            String protocol;

            if(Arrays.stream(profiles).anyMatch(p -> p.equalsIgnoreCase("dev"))) {
                // For health check, in dev profile where you run instance locally, you want to check
                // local instance. The local run profile contains edge as well so most of the
                // GN4 setup follows the value for edge env.
                host = "localhost";
                port = "8080";
                protocol = "http";
            }
            else {
                host = setup.getSiteSetting(SiteHelper.HOST);
                port = setup.getSiteSetting(SiteHelper.PORT);
                protocol = setup.getSiteSetting(SiteHelper.PROTOCOL);
            }

            ResponseEntity<List<Map<String, String>>> response = template.exchange(
                    String.format(CRITICAL_HEALTHCHECK, protocol, host, port),
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {});

            if(response.getStatusCode().is2xxSuccessful()) {
                if (response.getBody() != null) {
                    // We need to make sure all critical component is health in order to report healthy
                    Optional<Map<String, String>> target = response.getBody()
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
            return Health.down()
                    .withDetail("info", "GeoNetwork4 critical health check status not OK or missing body content")
                    .build();
        };
    }
}
