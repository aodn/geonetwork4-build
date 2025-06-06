package au.org.aodn.geonetwork4.model;

import au.org.aodn.geonetwork4.enumeration.Environment;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
    protected String activeProfile;

    protected ObjectMapper objectMapper = new ObjectMapper();

    public static final String ACTIVE_PROFILE_PARAM = "{active_profile}";
    public static final String GIT_BRANCH = "{git_branch}";

    public GitRemoteConfig(RestTemplate template, String activeProfile, String githubBranch) {
        this.restTemplate = template;
        this.githubBranch = githubBranch;
        // Dev env share the same gn4 config as edge for easy testing
        this.activeProfile = activeProfile == null ?
                null :
                activeProfile.equalsIgnoreCase("dev") ? "edge" : activeProfile;
    }
    /**
     * We hardcode the path to github main geonetwork4-build so we always get the approved configuration after PR.
     */
    protected String getUrl(RemoteConfigValue value) {
        String file = activeProfile != null ?
                value.getJsonFileName().replace(ACTIVE_PROFILE_PARAM, activeProfile) :
                value.getJsonFileName();

        return String.format(
                "https://raw.githubusercontent.com/aodn/geonetwork4-build/%s/geonetwork-config/%s/%s",
                githubBranch,
                value.type,
                file);
    }

    @Override
    public List<String> readJson(List<RemoteConfigValue> filenames) {
        return filenames.stream()
                // Handle config belong to any env or specific env
                .filter(p -> p.getEnvironment() == Environment.any || p.getEnvironment() == Environment.valueOf(activeProfile))
                .map(n -> {
                    String url = this.getUrl(n);
                    logger.info("Read config from -> {}", url);

                    ResponseEntity<String> content = restTemplate.getForEntity(url, String.class);

                    if (content.getStatusCode().is2xxSuccessful()) {
                        return content.getBody() != null ?
                                content.getBody().replace(GIT_BRANCH, githubBranch) :
                                null;
                    }
                    else {
                        logger.info("Config file not found {}", n.getJsonFileName());
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
