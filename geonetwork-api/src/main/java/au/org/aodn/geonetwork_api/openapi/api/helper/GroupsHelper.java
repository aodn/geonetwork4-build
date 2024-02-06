package au.org.aodn.geonetwork_api.openapi.api.helper;

import au.org.aodn.geonetwork_api.openapi.api.GroupsApi;
import au.org.aodn.geonetwork_api.openapi.api.Status;
import au.org.aodn.geonetwork_api.openapi.model.Group;
import au.org.aodn.geonetwork_api.openapi.model.MetadataCategory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Enhance the GroupApi function by grouping similar group functions together, it calls the GroupApi behind
 */

@Component
public class GroupsHelper {

    protected static final String OWNER_GROUP = "ownerGroup";
    protected static final String HARVESTER_DATA = "harvester_data";
    protected static final String NODE = "node";
    protected GroupsApi api;
    protected Logger logger = LogManager.getLogger(TagsHelper.class);

    @Autowired
    ObjectMapper objectMapper;

    public GroupsHelper(GroupsApi api) {
        this.api = api;
    }

    public Optional<JSONObject> getHarvestersOwnerGroup(JSONObject jsonObject) {
        return jsonObject.getJSONObject(HARVESTER_DATA).getJSONObject(NODE).isNull(OWNER_GROUP) ?
                Optional.empty() :
                Optional.of(jsonObject.getJSONObject(HARVESTER_DATA).getJSONObject(NODE).getJSONObject(OWNER_GROUP));
    }

    public JSONObject updateHarvestersOwnerGroup(JSONObject jsonObject, Group group) {
        JSONObject j = new JSONObject(jsonObject.toString());

        if(!getHarvestersOwnerGroup(j).isPresent()) {
            Map<String, ?> g = Map.of("id", group.getId());
            j.getJSONObject(HARVESTER_DATA)
                    .getJSONObject(NODE)
                    .put(OWNER_GROUP, g);
        }
        else {
            j.getJSONObject(HARVESTER_DATA)
                    .getJSONObject(NODE)
                    .getJSONObject(OWNER_GROUP)
                    .put("id", group.getId());
        }
        return j;
    }

    public Optional<Group> findGroup(String name) {
        ResponseEntity<List<Group>> groups = api.getGroupsWithHttpInfo(Boolean.TRUE, null);
        if(groups.getStatusCode().is2xxSuccessful()) {
            // Find the group name that matches
            return Objects.requireNonNull(groups.getBody())
                    .stream()
                    .filter(f -> {
                        assert f.getName() != null;
                        return f.getName().equals(name);
                    })
                    .findFirst();
        }

        return Optional.empty();
    }

    public void deleteGroups() {
        // you can delete group with associate metadata records, for testing purpose, change to use blank local ElasticSearch instance
        // without deleting existing groups, new group with same name will not be created
        ResponseEntity<List<Group>> groups = api.getGroupsWithHttpInfo(Boolean.TRUE, null);
        if(groups.getStatusCode().is2xxSuccessful()) {
            Objects.requireNonNull(groups.getBody())
                    .forEach(f -> api.deleteGroupWithHttpInfo(f.getId(), true));
        }
    }

    public List<Status> createGroups(List<String> json) {

        // delete existing groups
        this.deleteGroups();

        return json.stream()
                .map(m -> {
                    JSONObject jsonObject = new JSONObject(m);
                    Group group = new Group();

                    String name = jsonObject.getString("name");
                    String description = jsonObject.optString("description", null);
                    String logo = jsonObject.optString("logo", null);

                    String website = jsonObject.optString("website", null);
                    Boolean enableAllowedCategories = jsonObject.optBoolean("enableAllowedCategories", false);
                    String email = jsonObject.optString("email", null);
                    Integer referrer = jsonObject.optString("referrer").isEmpty() ? null : jsonObject.getInt("referrer");

                    Optional<MetadataCategory> defaultCategory = jsonObject.optJSONObject("defaultCategory").toMap().isEmpty() ?
                            Optional.empty() :
                            Optional.of(jsonObject.getJSONObject("defaultCategory").toMap())
                                    .map(m1 -> {
                                        MetadataCategory metadataCategory = new MetadataCategory();
                                        metadataCategory.setId((Integer) m1.get("id"));
                                        metadataCategory.setName((String) m1.get("name"));
                                        metadataCategory.setLabel((HashMap) m1.get("label"));
                                        return metadataCategory;
                                    });

                    Optional<List<MetadataCategory>> allowedCategories = jsonObject.optJSONObject("allowedCategories") == null ?
                            Optional.empty() :
                            Optional.of(jsonObject.getJSONArray("allowedCategories").toList())
                                    .map(m1 -> m1.stream()
                                            .map(m2 -> {
                                                MetadataCategory metadataCategory = new MetadataCategory();
                                                metadataCategory.setId((Integer) ((HashMap) m2).get("id"));
                                                metadataCategory.setName((String) ((HashMap) m2).get("name"));
                                                metadataCategory.setLabel((HashMap) ((HashMap) m2).get("label"));
                                                return metadataCategory;
                                            })
                                            .collect(Collectors.toList()));

                    Map<String, String> labels = jsonObject
                            .getJSONObject("label")
                            .toMap()
                            .entrySet()
                            .stream()
                            .collect(Collectors.toMap(Map.Entry::getKey, e -> String.valueOf(e.getValue())));

                    logger.info("Processing group {}", name);

                    // need to assign unique ID, given the requirement is no 2 groups having the same name, thus, use name.hashCode()
                    group.setId(name.hashCode());

                    group.setName(name);
                    group.setDescription(description);
                    group.setLogo(logo);
                    group.setWebsite(website);
                    group.setEnableAllowedCategories(enableAllowedCategories);
                    group.setEmail(email);
                    group.setReferrer(referrer);
                    group.setLabel(labels);
                    defaultCategory.ifPresent(group::setDefaultCategory);
                    allowedCategories.ifPresent(group::setAllowedCategories);

                    Status status = new Status();
                    status.setFileContent(m);

                    ResponseEntity<Integer> response = null;
                    try {
                        response = this.api.addGroupWithHttpInfo(group);
                    } catch (HttpClientErrorException.BadRequest badRequest) {
                        logger.error("Error adding group {}: {}", name, badRequest.getMessage(), badRequest);
                        status.setStatus(badRequest.getStatusCode());
                        status.setMessage(String.format("Insert group failed - %s already exists?", name));
                    } finally {
                        if (response != null) {
                            status.setStatus(response.getStatusCode());
                            if (response.getBody() != null) {
                                status.setMessage(response.getBody().toString());
                            }
                        }
                        logger.info("Processed group {}", name);
                    }
                    return status;
                })
                .collect(Collectors.toList());
    }
}
