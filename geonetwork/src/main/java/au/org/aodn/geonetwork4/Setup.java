package au.org.aodn.geonetwork4;

import au.org.aodn.geonetwork_api.openapi.api.*;
import au.org.aodn.geonetwork_api.openapi.api.helper.*;
import au.org.aodn.geonetwork_api.openapi.model.HarvestersApiLegacyResponse;

import au.org.aodn.geonetwork_api.openapi.model.SystemInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.io.InputStream;

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
    protected LogosHelper logosHelper;
    protected TagsHelper tagsHelper;
    protected UsersHelper usersHelper;
    protected VocabulariesHelper vocabulariesHelper;
    protected SiteHelper siteHelper;
    protected HarvestersApi harvestersApi;
    protected HarvestersApiLegacy harvestersApiLegacy;
    protected GroupsHelper groupsHelper;

    public Setup(MeApi meApi,
                 LogosApiExt logosApi,
                 GroupsApi groupsApi,
                 TagsApi tagsApi,
                 RegistriesApi registriesApi,
                 SiteApi siteApi,
                 UsersApi usersApi,
                 HarvestersApiLegacy harvestersApiLegacy,
                 HarvestersApi harvestersApi) {

        this.meApi = meApi;
        this.logosHelper = new LogosHelper(logosApi);
        this.groupsHelper = new GroupsHelper(groupsApi);
        this.tagsHelper = new TagsHelper(tagsApi);
        this.vocabulariesHelper = new VocabulariesHelper(registriesApi);
        this.siteHelper = new SiteHelper(siteApi);
        this.usersHelper = new UsersHelper(usersApi);
        this.harvestersApiLegacy = harvestersApiLegacy;
        this.harvestersApi = harvestersApi;
    }

    public void getMe() {
        logger.info("Login user is {}", meApi.getMeWithHttpInfo().getBody());
    }

    public ResponseEntity<Map<String, Object>> getSystemInfo() {
        return ResponseEntity.ok(Map.of(
                "systemInfo", siteHelper.getApi().getSystemInfoWithHttpInfo().getBody(),
                "siteInfo", siteHelper.getApi().getInformationWithHttpInfo().getBody()
        ));
    }

    public void deleteAllHarvesters() {
        harvestersApiLegacy.deleteAllHarvesters();
    }

    public ResponseEntity<List<HarvestersApiLegacyResponse>> insertHarvester(String... filenames) {
        List<String> config = Utils.readJson(filenames);
        return ResponseEntity.of(Optional.of(harvestersApiLegacy.createHarvesters(config)));
    }
    /**
     * TODO: The return type is a bit messy
     * @param filenames
     */
    public ResponseEntity<List<Status>> insertLogos(String... filenames) {
        List<String> config = Utils.readJson(filenames);
        return ResponseEntity.of(Optional.of(logosHelper.createLogos(config)));
    }

    public ResponseEntity<List<Status>> insertCategories(String... filenames) {
        List<String> config = Utils.readJson(filenames);
        return ResponseEntity.of(Optional.of(tagsHelper.createTags(config)));
    }

    public ResponseEntity<List<Status>> insertVocabularies(String... filenames) {
        List<String> config = Utils.readJson(filenames);
        return ResponseEntity.of(Optional.of(vocabulariesHelper.createVocabularies(config)));
    }

    public ResponseEntity<List<Status>> insertSettings(String... filenames) {
        List<String> config = Utils.readJson(filenames);
        return ResponseEntity.of(Optional.of(siteHelper.createSettings(config)));
    }

    public ResponseEntity<List<Status>> insertGroups(String... filenames) {
        List<String> config = Utils.readJson(filenames);
        return ResponseEntity.of(Optional.of(groupsHelper.createGroups(config)));
    }

    public ResponseEntity<List<Status>> insertUsers(String... filenames) {
        List<String> config = Utils.readJson(filenames);
        return ResponseEntity.of(Optional.of(usersHelper.createOrUpdateUsers(config)));
    }
}
