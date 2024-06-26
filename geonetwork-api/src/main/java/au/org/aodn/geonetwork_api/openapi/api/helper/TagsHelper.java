package au.org.aodn.geonetwork_api.openapi.api.helper;

import au.org.aodn.geonetwork_api.openapi.api.Status;
import au.org.aodn.geonetwork_api.openapi.api.TagsApi;
import au.org.aodn.geonetwork_api.openapi.model.MetadataCategory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Enhance the TagsApi function by grouping similar tags functions together, it calls the TagsApi behind
 */
public class TagsHelper {

    public static final String ID_ATTRIBUTE = "-id";
    public static final String NAME = "name";
    public static final String NAME_ATTRIBUTE = "-name";
    public static final String CATEGORIES = "categories";
    public static final String CATEGORY = "category";
    protected static final String HARVESTER_DATA = "harvester_data";
    protected static final String NODE = "node";

    protected Logger logger = LogManager.getLogger(TagsHelper.class);

    protected TagsApi api;

    public TagsHelper(TagsApi api) {
        this.api = api;
    }

    public Optional<MetadataCategory> findTag(String tag) {
        ResponseEntity<List<MetadataCategory>> response = this.api.getTagsWithHttpInfo();

        if(response.getStatusCode().is2xxSuccessful() && tag != null) {
            return Objects.requireNonNull(response.getBody())
                    .stream()
                    .filter(f -> tag.equalsIgnoreCase(f.getName()))
                    .findFirst();
        }
        else {
            return Optional.empty();
        }
    }

    public Optional<MetadataCategory> findTag(Integer id) {
        MetadataCategory r = this.api.getTag(id);
        return r != null ? Optional.of(r) : Optional.empty();
    }

    public void deleteAllTags() {
        try {
            ResponseEntity<List<MetadataCategory>> response = this.api.getTagsWithHttpInfo();
            if(response.getStatusCode().is2xxSuccessful()) {
                List<MetadataCategory> tags = response.getBody();

                if(tags != null) {
                    for (MetadataCategory m : tags) {
                        this.api.deleteTagWithHttpInfo(m.getId());
                    }
                }
            }
        }
        catch (Exception e) {
            logger.error("Fail to query all tags");
        }
    }

    public List<Status> createTags(List<String> json) {
        return json.stream()
                .map(m -> {
                    JSONObject jsonObject = new JSONObject(m);
                    MetadataCategory metadataCategory = new MetadataCategory();

                    // Convert map from <String, Object> to <String, String>
                    Map<String, String> labels = jsonObject
                            .getJSONObject("label")
                            .toMap()
                            .entrySet()
                            .stream()
                            .collect(Collectors.toMap(Map.Entry::getKey, e -> String.valueOf(e.getValue())));

                    String name = jsonObject.getString(NAME);

                    logger.info("Processing category {}", name);
                    metadataCategory.setName(name);
                    metadataCategory.setLabel(labels);

                    Status status = new Status();
                    status.setFileContent(m);

                    ResponseEntity<Integer> response;
                    try {
                        response = this.api.putTagWithHttpInfo(metadataCategory);
                        status.setStatus(response.getStatusCode());

                        if(response.getBody() != null) {
                            status.setMessage(response.getBody().toString());
                        }
                        logger.info("Processed category {}", name);
                        return status;
                    }
                    catch(HttpClientErrorException.BadRequest badRequest) {
                        status.setStatus(badRequest.getStatusCode());
                        status.setMessage(String.format("Insert category failed - %s already exist?", name));
                        return status;
                    }
                })
                .collect(Collectors.toList());
    }

    public Optional<JSONObject> getHarvestersCategories(JSONObject jsonObject) {
        return jsonObject.getJSONObject(HARVESTER_DATA).getJSONObject(NODE).isNull(CATEGORIES) ?
                Optional.empty() :
                Optional.of(jsonObject.getJSONObject(HARVESTER_DATA).getJSONObject(NODE).getJSONObject(CATEGORIES));
    }
    /**
     * Add the category id to the config if found
     * @param jsonObject - Incoming config of harvester
     * @param category - The category to be added
     * @return The jsonObject with category filled if possible
     */
    public JSONObject updateHarvestersCategories(JSONObject jsonObject, MetadataCategory category) {
        // Get object clone.
        JSONObject j = new JSONObject(jsonObject.toString());

        getHarvestersCategories(j).ifPresent(obj ->
                // Attribute value must be string
                obj.getJSONObject(CATEGORY).put(ID_ATTRIBUTE, String.valueOf(category.getId()))
        );

        return j;
    }
}
