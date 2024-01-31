package au.org.aodn.geonetwork_api.openapi.api;

import au.org.aodn.geonetwork_api.openapi.model.MetadataCategory;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Optional;

public class TagsHelper {

    protected static final String CATEGORIES = "categories";
    protected static final String HARVESTER_DATA = "harvester_data";
    protected static final String NODE = "node";

    protected TagsApi api;

    public TagsHelper(TagsApi api) {
        this.api = api;
    }

    public Optional<MetadataCategory> findTag(String tag) {
        ResponseEntity<List<MetadataCategory>> response = this.api.getTagsWithHttpInfo();

        if(response.getStatusCode().is2xxSuccessful() && tag != null) {
            return response.getBody()
                    .stream()
                    .filter(f -> tag.equalsIgnoreCase(f.getName()))
                    .findFirst();
        }
        else {
            return Optional.empty();
        }
    }

    public Optional<JSONArray> getHarvestersCategories(JSONObject jsonObject) {
        return jsonObject.getJSONObject(HARVESTER_DATA).getJSONObject(NODE).isNull(CATEGORIES) ?
                Optional.empty() :
                Optional.of(jsonObject.getJSONObject(HARVESTER_DATA).getJSONObject(NODE).getJSONArray(CATEGORIES));
    }

    public JSONObject updateHarvestersCategories(JSONObject jsonObject, MetadataCategory category) {
        JSONObject j = new JSONObject(jsonObject.toString());

        if(getHarvestersCategories(j).isPresent()) {
            j.getJSONObject(HARVESTER_DATA)
                    .getJSONObject(NODE)
                    .getJSONArray(CATEGORIES)
                    .getJSONObject(0)
                    .getJSONObject("category")
                    .put("@id", category.getId());
        }

        return j;
    }
}
