package au.org.aodn.geonetwork4;

import au.org.aodn.geonetwork_api.openapi.api.*;
import au.org.aodn.geonetwork_api.openapi.api.helper.*;
import au.org.aodn.geonetwork_api.openapi.model.Group;
import au.org.aodn.geonetwork_api.openapi.model.HarvestersApiLegacyResponse;

import au.org.aodn.geonetwork_api.openapi.model.MeResponse;
import au.org.aodn.geonetwork_api.openapi.model.MetadataCategory;
import com.github.underscore.U;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.ResponseEntity;

import java.util.List;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * This class is used for post provision setup after the system starts
 */
public class Setup {

    public static final String SYSTEM_INFO = "systemInfo";
    public static final String SITE_INFO = "siteInfo";

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

    public Setup(ResourceLoader resourceLoader,
                 MeApi meApi,
                 LogosApiExt logosApi,
                 GroupsApi groupsApi,
                 TagsApi tagsApi,
                 RegistriesApi registriesApi,
                 SiteApi siteApi,
                 UsersApi usersApi,
                 HarvestersApiLegacy harvestersApiLegacy,
                 HarvestersApi harvestersApi) {

        this.meApi = meApi;
        this.logosHelper = new LogosHelper(logosApi, resourceLoader);
        this.groupsHelper = new GroupsHelper(groupsApi);
        this.tagsHelper = new TagsHelper(tagsApi);
        this.vocabulariesHelper = new VocabulariesHelper(registriesApi);
        this.siteHelper = new SiteHelper(siteApi);
        this.usersHelper = new UsersHelper(usersApi);
        this.harvestersApiLegacy = harvestersApiLegacy;
        this.harvestersApi = harvestersApi;
    }

    public ResponseEntity<MeResponse> getMe() {
        return ResponseEntity.ok(meApi.getMeWithHttpInfo().getBody());
    }

    public ResponseEntity<Map<String, Object>> getSystemInfo() {
        return ResponseEntity.ok(Map.of(
                SYSTEM_INFO, Objects.requireNonNull(siteHelper.getApi().getSystemInfoWithHttpInfo().getBody()),
                SITE_INFO, Objects.requireNonNull(siteHelper.getApi().getInformationWithHttpInfo().getBody())
        ));
    }

    public void deleteAllHarvesters() {
        harvestersApiLegacy.deleteAllHarvesters();
    }

    public void deleteAllCategories() { tagsHelper.deleteAllTags(); }
    /**
     * The getAllHarvesters is used to get the json format of harvester setting of geonetwork4. It will add
     * extra fields so works better across different instance of geonetwork4.
     * However, before we store it into repo, we need to break it down so that each harvester json contains 1 harvester
     * setting only.
     *
     * @return - The full set of harvesters
     */
    public ResponseEntity<String> getAllHarvesters() {
        ResponseEntity<String> harvesters = harvestersApiLegacy.getHarvestersWithHttpInfo();

        if(harvesters.getStatusCode().is2xxSuccessful() && harvesters.getBody() != null) {
            // We need to add a field to the ownerGroup because it do not comes by default,
            // the default only expose the group id where it is not reliable across different instance of geonetwork
            // so we expose the group name
            String json = U.xmlToJson(harvesters.getBody());
            JSONObject raw = new JSONObject(json);
            JSONObject nodes = raw.optJSONObject("nodes");
            JSONArray node = nodes.optJSONArray(GroupsHelper.NODE);

            for(int i = 0; i < node.length(); i++) {
                JSONObject n = node.optJSONObject(i);

                // Fill in owner group name if possible
                if(!n.isEmpty()
                        && !n.isNull(GroupsHelper.OWNER_GROUP)
                        && !n.optJSONObject(GroupsHelper.OWNER_GROUP).isEmpty()) {

                    Optional<Group> g = groupsHelper.findGroupById(
                            n.optJSONObject(GroupsHelper.OWNER_GROUP).getInt(GroupsHelper.ID)
                    );
                    g.ifPresent(group -> n.optJSONObject(GroupsHelper.OWNER_GROUP).put("name", group.getName()));
                }

                // Fill in category name if possible
                if(!n.isEmpty()
                        && !n.isNull(TagsHelper.CATEGORIES)
                        && !n.optJSONObject(TagsHelper.CATEGORIES).isEmpty()) {

                    // Single object case here
                    Optional<MetadataCategory> tag = tagsHelper.findTag(
                            n.optJSONObject(TagsHelper.CATEGORIES)
                                    .getJSONObject(TagsHelper.CATEGORY)
                                    .getInt(TagsHelper.ID_ATTRIBUTE)
                    );

                    tag.ifPresent(metadataCategory -> n.optJSONObject(TagsHelper.CATEGORIES)
                            .getJSONObject(TagsHelper.CATEGORY)
                            .put(TagsHelper.NAME_ATTRIBUTE, metadataCategory.getName()));
                }
            }

            return ResponseEntity
                    .ok(raw.toString(2));
        }
        else {
            return ResponseEntity.ok("Nothing found");
        }
    }

    public ResponseEntity<List<HarvestersApiLegacyResponse>> insertHarvester(List<String> config) {
        return ResponseEntity.of(Optional.of(harvestersApiLegacy.createHarvesters(config)));
    }

    public String getSiteSetting(String path) {
        return siteHelper.getAllSettingsDetails().get(path).getValue();
    }
    /**
     * TODO: The return type is a bit messy
     * @param config - The json config
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
