package au.org.aodn.geonetwork4.model;

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

    public GitRemoteConfig(RestTemplate template) {
        restTemplate = template;
    }
    /**
     * By default it use the main branch, however when you do your development, you can use a different branch
     * by setup the parameter
     */
    @Value("${aodn.geonetwork4.esIndexer.githubBranch:main}")
    protected String githubBranch;
    /**
     * We hardcode the path to github main geonetwork4-build so we always get the approved configuration after PR.
     */
    protected String getUrl(String branch, RemoteConfigValue value) {
        return String.format(
                "https://raw.githubusercontent.com/aodn/geonetwork4-build/%s/geonetwork-config/%s/%s",
                branch,
                value.type,
                value.jsonFileName);
    }

    @Override
    public List<String> readJson(List<RemoteConfigValue> filenames) {
        return filenames.stream()
                .map(n -> {
                    ResponseEntity<String> content = restTemplate.getForEntity(this.getUrl(githubBranch, n), String.class);

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
        return restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<RemoteConfigValue>>() {}).getBody();
    }
}
