package au.org.aodn.geonetwork_api.openapi.api;

import au.org.aodn.geonetwork_api.openapi.api.helper.GroupsHelper;
import au.org.aodn.geonetwork_api.openapi.model.Group;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ResourceUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

public class GroupHelperTest {

    protected Logger logger = LogManager.getLogger(GroupHelperTest.class);

    @Test
    public void verifyGetHarvestersOwnerGroup() throws IOException {
        GroupsHelper helper = new GroupsHelper(null);
        Parser parser = new Parser();

        String json = FileUtils.readFileToString(
                ResourceUtils.getFile("classpath:catalogue_noaa_owngroup.json"),
                StandardCharsets.UTF_8);

        Parser.Parsed parsed = parser.parseHarvestersConfig(json);
        Optional<JSONObject> i = helper.getHarvestersOwnerGroup(parsed.getJsonObject());

        assertTrue("Own group exist", i.isPresent());
        assertEquals("Own group id is", 123, i.get().getInt("id"));
    }

    @Test
    public void verifyUpdateHarvestersOwnerGroup() throws IOException {
        GroupsHelper helper = new GroupsHelper(null);
        Parser parser = new Parser();

        String json = FileUtils.readFileToString(
                ResourceUtils.getFile("classpath:catalogue_noaa.json"),
                StandardCharsets.UTF_8);

        Group group = new Group();
        group.setName("ABC");
        group.setId(1234);

        Parser.Parsed parsed = parser.parseHarvestersConfig(json);
        Optional<JSONObject> i = helper.getHarvestersOwnerGroup(parsed.getJsonObject());

        assertFalse("No group found", i.isPresent());

        JSONObject n = helper.updateHarvestersOwnerGroup(parsed.getJsonObject(), group);
        parsed = parser.parseHarvestersConfig(n.toString());

        i = helper.getHarvestersOwnerGroup(parsed.getJsonObject());

        logger.info("{}", i.toString());

        assertTrue("Own group updated and exist", i.isPresent());
        assertEquals("Own group updated id is", 1234, i.get().getInt("id"));
    }
    /**
     * We do not want to delete any build in group as it cause issues.
     */
    @Test
    public void verfiyDeleteGroupKeepBuildInGroup() {
        // Check is equalIgnoreCase, so capital letter or not does not matter
        Group all = new Group().name("All");
        Group intranet = new Group().name("intranet");
        Group guest = new Group().name("guest");
        Group sample = new Group().name("SamplE");

        // Should delete this only
        Group toBeDeleted = new Group().name("To be deleted").id(1);

        List<Group> groupList = List.of(all, intranet, guest, sample, toBeDeleted);

        GroupsApi api = mock(GroupsApi.class);
        Mockito.doReturn(ResponseEntity.ok(groupList))
                .when(api)
                .getGroupsWithHttpInfo(eq(Boolean.TRUE), isNull());

        GroupsHelper helper = new GroupsHelper(api);
        helper.deleteAllGroups();

        verify(api, times(1)).deleteGroupWithHttpInfo(anyInt(), eq(true));
    }
}
