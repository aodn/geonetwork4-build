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

import java.util.List;
import java.util.Map;
import java.util.Optional;
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
            return groups.getBody()
                    .stream()
                    .filter(f -> f.getName().equals(name))
                    .findFirst();
        }

        return Optional.empty();
    }

    public List<Status> createGroups(List<String> json) {
        return json.stream()
                .map(m -> {
                    JSONObject jsonObject = new JSONObject(m);

                    // Convert map from <String, Object> to <String, String>
                    Map<String, String> labels = jsonObject
                            .getJSONObject("label")
                            .toMap()
                            .entrySet()
                            .stream()
                            .collect(Collectors.toMap(Map.Entry::getKey, e -> String.valueOf(e.getValue())));


                    String name = jsonObject.getString("name");
                    String description = jsonObject.optString("description");
                    String logo = jsonObject.getString("logo");

//
//                    try {
//                        // Deserialize the JSON string to a Group instance
//                        Group group = objectMapper.readValue(jsonObject.toString(), Group.class);
//
//                        // Now, you can use the 'group' object as needed
//                        System.out.println("Group Name: " + group.getName());
//                        System.out.println("Group Logo: " + group.getLogo());
//
//                        // ... and so on
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    }
//
//                    // TODO: replace with group related
//                    String name = jsonObject.getString("name");
//                    String description = jsonObject.getString("description");
//                    String logo = jsonObject.getString("logo");
//
//                    String website = jsonObject.getString("website");
//                    Boolean enableAllowedCategories = jsonObject.optBoolean("enableAllowedCategories", false);
//                    String email = jsonObject.getString("email");
//                    Integer referrer = jsonObject.getInt("referrer");
//
//                    Optional<MetadataCategory> defaultCategory =
//                            Optional.of(jsonObject.getJSONObject("defaultCategory").toMap())
//                                    .map(m1 -> {
//                                        MetadataCategory metadataCategory = new MetadataCategory();
//                                        metadataCategory.setId((Integer) m1.get("id"));
//                                        metadataCategory.setName((String) m1.get("name"));
//                                        metadataCategory.setLabel((Map<String, String>) m1.get("label"));
//                                        return metadataCategory;
//                                    });
//
//                    Optional<List<MetadataCategory>> allowedCategories = jsonObject.getJSONObject("allowedCategories") == null ?
//                            Optional.empty() :
//                            Optional.of(jsonObject.getJSONArray("allowedCategories").toList())
//                                    .map(m1 -> m1.stream()
//                                            .map(m2 -> {
//                                                MetadataCategory metadataCategory = new MetadataCategory();
//                                                metadataCategory.setId((Integer) ((Map<String, Object>) m2).get("id"));
//                                                metadataCategory.setName((String) ((Map<String, Object>) m2).get("name"));
//                                                metadataCategory.setLabel((Map<String, String>) ((Map<String, Object>) m2).get("label"));
//                                                return metadataCategory;
//                                            })
//                                            .collect(Collectors.toList()));
//
//
//                    logger.info("Processing group {}", name);
//
//                    group.setName(name);
//                    group.setLabel(labels);
//                    description.ifPresent(group::setDescription);
//                    logo.ifPresent(group::setLogo);
//                    website.ifPresent(group::setWebsite);
//                    defaultCategory.ifPresent(group::setDefaultCategory);
//                    allowedCategories.ifPresent(group::setAllowedCategories);
//                    group.setEnableAllowedCategories(enableAllowedCategories);
//                    email.ifPresent(group::setEmail);
//                    referrer.ifPresent(group::setReferrer);
//
//                    System.out.println("Processing group");
//                    System.out.println(group);
//                    Status status = new Status();
//                    status.setFileContent(m);
//
//                    ResponseEntity<Integer> response = null;
//                    try {
//                        System.out.println(group);
////                        response = new ResponseEntity<>(); //this.api.putTagWithHttpInfo(metadataCategory);
//                    } catch (HttpClientErrorException.BadRequest badRequest) {
//                        status.setStatus(badRequest.getStatusCode());
//                        status.setMessage(String.format("Insert group failed - %s already exist?", name));
//                    } finally {
//                        if (response != null) {
//                            status.setStatus(response.getStatusCode());
//                            if (response.getBody() != null) {
//                                status.setMessage(response.getBody().toString());
//                            }
//                        }
//                        logger.info("Processed group {}", name);
//                        return status;
//                    }
                    return new Status();
                })
                .collect(Collectors.toList());
    }
}
