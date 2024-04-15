package au.org.aodn.geonetwork4.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class GitRemoteConfig implements RemoteConfig {

    protected static Logger logger = LogManager.getLogger(GitRemoteConfig.class);

    protected RestTemplate restTemplate;
    protected String githubBranch;

    protected ObjectMapper objectMapper = new ObjectMapper();

    public GitRemoteConfig(RestTemplate template, String githubBranch) {
        this.restTemplate = template;
        this.githubBranch = githubBranch;
    }
    /**
     * We hardcode the path to github main geonetwork4-build so we always get the approved configuration after PR.
     */
    protected String getUrl(RemoteConfigValue value) {
        return String.format(
                "https://raw.githubusercontent.com/aodn/geonetwork4-build/%s/geonetwork-config/%s/%s",
                githubBranch,
                value.type,
                value.jsonFileName);
    }

    @Override
    public List<String> readJson(List<RemoteConfigValue> filenames) {
        return filenames.stream()
                .map(n -> {
                    String url = this.getUrl(n);
                    logger.debug("Read config from -> {}", url);

                    ResponseEntity<String> content = restTemplate.getForEntity(url, String.class);

                    if(content.getStatusCode().is2xxSuccessful()) {
                        return content.getBody();
                    }
                    else {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public List<RemoteConfigValue> getDefaultConfig() {
        String url = String.format("https://raw.githubusercontent.com/aodn/geonetwork4-build/%s/geonetwork-config/config.json", githubBranch);
        logger.info("Get default config from -> {}", url);

        ResponseEntity<String> content = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                String.class);

        try {
            return objectMapper.readValue(
                    content.getBody(),
                    TypeFactory.defaultInstance().constructCollectionType(List.class, RemoteConfigValue.class));
        }
        catch (Exception e) {
            logger.error("Fail to read default config from {}", url);
            return List.of();
        }
    }

    @Override
    public String toString() {
        return "Github remote configurator";
    }
}
