package au.org.aodn.geonetwork4;

import au.org.aodn.geonetwork4.model.GitConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Utils {

    protected static Logger logger = LogManager.getLogger(Utils.class);

    protected RestTemplate restTemplate;

    protected static List<String> readJson(String... filenames) {
        return Arrays.stream(filenames)
                .map(n -> {
                    ClassLoader cl = Thread.currentThread().getContextClassLoader(); // or whatever classloader you want to search from

                    try(InputStream stream = cl.getResourceAsStream(n)){
                        return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
                    }
                    catch (IOException | NullPointerException e) {
                        logger.error("Fail extract file content -> {}", n);
                        return null;
                    }
                })
                .filter(f -> f != null)
                .collect(Collectors.toList());
    }

    protected static List<String> readJson(List<GitConfig> filenames) {
        return filenames.stream()
                .map(n -> {

                    try(InputStream stream = cl.getResourceAsStream(n)){
                        return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
                    }
                    catch (IOException | NullPointerException e) {
                        logger.error("Fail extract file content -> {}", n);
                        return null;
                    }
                })
                .filter(f -> f != null)
                .collect(Collectors.toList());
    }
}
