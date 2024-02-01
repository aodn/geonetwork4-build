package au.org.aodn.geonetwork4;

import au.org.aodn.geonetwork_api.openapi.api.*;
import au.org.aodn.geonetwork_api.openapi.api.helper.LogosHelper;
import au.org.aodn.geonetwork_api.openapi.api.helper.TagsHelper;
import au.org.aodn.geonetwork_api.openapi.model.HarvestersApiLegacyResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.io.InputStream;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import java.util.Optional;
import java.util.stream.Collectors;

/**
 * This class is used for post provision setup after the system starts
 */
public class Setup {

    protected Logger logger = LogManager.getLogger(Setup.class);

    protected MeApi meApi;
    protected LogosHelper logosHelper;
    protected TagsHelper tagsHelper;
    protected HarvestersApi harvestersApi;
    protected HarvestersApiLegacy harvestersApiLegacy;

    protected List<String> readJson(String... filenames) {
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

    public Setup(MeApi meApi, LogosApiExt logosApi, TagsApi tagsApi, HarvestersApiLegacy harvestersApiLegacy, HarvestersApi harvestersApi) {
        this.meApi = meApi;
        this.logosHelper = new LogosHelper(logosApi);
        this.tagsHelper = new TagsHelper(tagsApi);
        this.harvestersApiLegacy = harvestersApiLegacy;
        this.harvestersApi = harvestersApi;
    }

    public void getMe() {
        logger.info("Login user is {}", meApi.getMeWithHttpInfo().getBody());
    }

    public void deleteAllHarvesters() {
        harvestersApiLegacy.deleteAllHarvesters();
    }

    public ResponseEntity<List<HarvestersApiLegacyResponse>> insertHarvester(String... filenames) {
        List<String> config = readJson(filenames);
        return ResponseEntity.of(Optional.of(harvestersApiLegacy.createHarvesters(config)));
    }
    /**
     * TODO: The return type is a bit messy
     * @param filenames
     */
    public ResponseEntity<List<String>> insertLogos(String... filenames) {
        List<String> config = readJson(filenames);
        return ResponseEntity.of(Optional.of(logosHelper.createLogos(config)));
    }

    public ResponseEntity<List<Status>> insertCatagories(String... filenames) {
        List<String> config = readJson(filenames);
        return ResponseEntity.of(Optional.of(tagsHelper.createTags(config)));
    }
}
