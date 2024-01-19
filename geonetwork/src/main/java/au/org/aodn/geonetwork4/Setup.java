package au.org.aodn.geonetwork4;

import au.org.aodn.geonetwork_api.openapi.invoker.ApiClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * This class is used for post provision setup after the system starts
 */
@Configuration
public class Setup {

    @Bean
    public ApiClient getApiClient(
            @Value("${GEONETWORK_ADMIN_USERNAME:admin}") String username,
            @Value("${GEONETWORK_ADMIN_PASSWORD:admin}") String password) {

        ApiClient api = new ApiClient();

        api.setVerifyingSsl(false);
        api.setUsername(username);
        api.setPassword(password);

        return api;
    }

}
