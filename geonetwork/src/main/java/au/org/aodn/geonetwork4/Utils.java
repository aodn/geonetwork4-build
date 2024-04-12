package au.org.aodn.geonetwork4;

import au.org.aodn.geonetwork4.model.GitConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class Utils {

    protected static Logger logger = LogManager.getLogger(Utils.class);

    protected RestTemplate restTemplate;

    @Value("${aodn.geonetwork4.esIndexer.githubBranch:main}")
    protected String githubBranch;

    public Utils(RestTemplate template) {
        restTemplate = template;
    }

    protected List<String> readJson(String... filenames) {
        return Arrays.stream(filenames)
                .map(n -> {
                    ClassLoader cl = Thread.currentThread().getContextClassLoader(); // or whatever classloader you want to search from

                    try(InputStream stream = cl.getResourceAsStream(n)) {
                        if (stream != null) {
                            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
                        }
                    }
                    catch (IOException | NullPointerException e) {
                        logger.error("Fail extract file content -> {}", n);
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    protected List<String> readJson(List<GitConfig> filenames) {
        return filenames.stream()
                .map(n -> {
                    ResponseEntity<String> content = restTemplate.getForEntity(n.getUrl(githubBranch), String.class);

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
}
