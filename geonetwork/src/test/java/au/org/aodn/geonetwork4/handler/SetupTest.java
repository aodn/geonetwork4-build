package au.org.aodn.geonetwork4.handler;

import au.org.aodn.geonetwork4.Setup;
import au.org.aodn.geonetwork_api.openapi.api.GroupsApi;
import au.org.aodn.geonetwork_api.openapi.api.HarvestersApiLegacy;
import au.org.aodn.geonetwork_api.openapi.model.Group;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;
import org.json.JSONObject;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ResourceUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

public class SetupTest {

    protected ObjectMapper mapper = new ObjectMapper();
    /**
     * Refer to the method for details
     */
    @Test
    public void verifyGetAllHarvesters() throws IOException {
        String json = FileUtils.readFileToString(
                ResourceUtils.getFile("classpath:harvesterlist.xml"),
                StandardCharsets.UTF_8);

        HarvestersApiLegacy harvestersApiLegacy = Mockito.mock(HarvestersApiLegacy.class);
        when(harvestersApiLegacy.getHarvestersWithHttpInfo())
                .thenReturn(ResponseEntity.ok(json));

        String groupsString = FileUtils.readFileToString(
                ResourceUtils.getFile("classpath:groups.json"),
                StandardCharsets.UTF_8);

        List<Group> groups = mapper.readValue(groupsString, new TypeReference<>() {});

        GroupsApi groupsApi = Mockito.mock(GroupsApi.class);
        when(groupsApi.getGroupsWithHttpInfo(eq(Boolean.TRUE), isNull()))
                .thenReturn(ResponseEntity.ok(groups));

        Setup setup = new Setup(
                null,
                null,
                null,
                groupsApi,
                null,
                null,
                null,
                null,
                harvestersApiLegacy,
                null
        );

        String resultJson = FileUtils.readFileToString(
                ResourceUtils.getFile("classpath:harvesterlist.json"),
                StandardCharsets.UTF_8);

        ResponseEntity<String> result = setup.getAllHarvesters();

        if(result.getStatusCode().is2xxSuccessful() && result.getBody() != null) {
            String tt = result.getBody();

            JSONObject target = new org.json.JSONObject(tt);
            JSONObject expected = new org.json.JSONObject(resultJson);

            assertTrue("Both json equals", expected.similar(target));
        }
    }
}
