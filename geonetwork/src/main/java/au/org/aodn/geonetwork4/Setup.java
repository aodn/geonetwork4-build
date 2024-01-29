package au.org.aodn.geonetwork4;

import au.org.aodn.geonetwork_api.openapi.api.*;
import au.org.aodn.geonetwork_api.openapi.model.HarvestersApiLegacyResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpServerErrorException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * This class is used for post provision setup after the system starts
 */
public class Setup {

    protected Logger logger = LogManager.getLogger(Setup.class);

    protected MeApi meApi;
    protected LogosApi logosApi;
    protected HarvestersApi harvestersApi;
    protected HarvestersApiLegacy harvestersApiLegacy;

    protected ObjectMapper mapper = new ObjectMapper();

    protected List<String> readJson(String... filenames) {
        return Arrays.stream(filenames)
                .map(n -> {
                    ClassLoader cl = Thread.currentThread().getContextClassLoader(); // or whatever classloader you want to search from

                    try(InputStream stream = cl.getResourceAsStream(n)){
                        return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
                    }
                    catch (IOException e) {
                        logger.error("Fail extract file content -> {}", n);
                        return null;
                    }
                })
                .filter(f -> f != null)
                .collect(Collectors.toList());
    }

    public Setup(MeApi meApi, LogosApi logosApi, HarvestersApiLegacy harvestersApiLegacy, HarvestersApi harvestersApi) {
        this.meApi = meApi;
        this.logosApi = logosApi;
        this.harvestersApiLegacy = harvestersApiLegacy;
        this.harvestersApi = harvestersApi;
    }

    public void getMe() {
        logger.info("Login user is {}", meApi.getMeWithHttpInfo().getBody());
    }

    public ResponseEntity<List<HarvestersApiLegacyResponse>> insertHarvester(String... filenames) {
        List<String> config = readJson(filenames);
        return ResponseEntity.of(Optional.of(harvestersApiLegacy.createHarvesters(config)));
    }
    /**
     * TODO: not working with multipart upload.
     * @param filenames
     */
    public void insertLogos(String... filenames) {

        // Extract the json content in the files
        List<Map<String, String>> json = readJson(filenames)
                .stream()
                .map(s -> {
                    try {
                        return mapper.readValue(s, new TypeReference<Map<String, String>>() {});
                    }
                    catch(Exception e) {
                        return null;
                    }
                })
                .filter(f -> f != null)
                .collect(Collectors.toList());

        List<File> fileList = json.stream()
                .map(j -> {
                    try (InputStream inputStream = new URL(j.get("link")).openStream()) {
                        File temp = new File(System.getProperty("java.io.tmpdir"), j.get("image"));
                        FileUtils.copyInputStreamToFile(inputStream, temp);
                        return temp;
                    }
                    catch(IOException e) {
                        return null;
                    }
                })
                .filter(f -> f != null)
                .collect(Collectors.toList());

        if(!fileList.isEmpty()) {
            logosApi.addLogoWithHttpInfo(fileList, true);
        }
    }
}
