package au.org.aodn.geonetwork4;

import au.org.aodn.geonetwork_api.openapi.api.*;
import au.org.aodn.geonetwork_api.openapi.api.helper.*;
import au.org.aodn.geonetwork_api.openapi.model.HarvestersApiLegacyResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;
import org.json.XML;
import org.springframework.http.ResponseEntity;

import java.util.List;

import java.util.Map;
import java.util.Optional;

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

    public void deleteAllCategories() { tagsHelper.deleteAllTags(); }

    public ResponseEntity<String> getAllHarvesters() {
        ResponseEntity<String> harvesters = harvestersApiLegacy.getHarvestersWithHttpInfo();

        if(harvesters.getStatusCode().is2xxSuccessful() && harvesters.getBody() != null) {
            JSONObject jsonObject = XML.toJSONObject(harvesters.getBody());
            return ResponseEntity.ok(jsonObject.toString());
        }
        else {
            return ResponseEntity.ok("Nothing found");
        }
    }

    public ResponseEntity<List<HarvestersApiLegacyResponse>> insertHarvester(List<String> config) {
        return ResponseEntity.of(Optional.of(harvestersApiLegacy.createHarvesters(config)));
    }
    /**
     * TODO: The return type is a bit messy
     * @param filenames
     */
    public ResponseEntity<List<Status>> insertLogos(List<String> config) {
        return ResponseEntity.of(Optional.of(logosHelper.createLogos(config)));
    }

    public ResponseEntity<List<Status>> insertCategories(List<String> config) {
        return ResponseEntity.of(Optional.of(tagsHelper.createTags(config)));
    }

    public ResponseEntity<List<Status>> insertVocabularies(List<String> config) {
        return ResponseEntity.of(Optional.of(vocabulariesHelper.createVocabularies(config)));
    }

    public ResponseEntity<List<Status>> insertSettings(List<String> config) {
        return ResponseEntity.of(Optional.of(siteHelper.createSettings(config)));
    }

    public ResponseEntity<List<Status>> insertGroups(List<String> config) {
        return ResponseEntity.of(Optional.of(groupsHelper.createGroups(config)));
    }

    public ResponseEntity<List<Status>> insertUsers(List<String> config) {
        return ResponseEntity.of(Optional.of(usersHelper.createOrUpdateUsers(config)));
    }
}
