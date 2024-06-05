package au.org.aodn.geonetwork4;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ActuatorConfig {
    @Bean
    public HealthIndicator geonetworkHealthIndicator() {
        return Health.up()
                .withDetail("info", "GeoNetwork4")
                ::build;
    }
}
