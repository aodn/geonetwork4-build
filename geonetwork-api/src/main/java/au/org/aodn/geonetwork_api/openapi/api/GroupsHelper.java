package au.org.aodn.geonetwork_api.openapi.api;

import au.org.aodn.geonetwork_api.openapi.model.Group;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class GroupsHelper {

    protected static final String OWNER_GROUP = "ownerGroup";
    protected static final String HARVESTER_DATA = "harvester_data";
    protected static final String NODE = "node";
    protected GroupsApi api;

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
}
