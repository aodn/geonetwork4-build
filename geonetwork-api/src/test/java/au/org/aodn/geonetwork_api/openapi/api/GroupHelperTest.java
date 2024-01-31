package au.org.aodn.geonetwork_api.openapi.api;

import au.org.aodn.geonetwork_api.openapi.api.helper.GroupsHelper;
import au.org.aodn.geonetwork_api.openapi.model.Group;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;
import org.junit.Test;
import org.springframework.util.ResourceUtils;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Optional;

import static org.junit.Assert.*;

public class GroupHelperTest {

    protected Logger logger = LogManager.getLogger(GroupHelperTest.class);

    @Test
    public void verifyGetHarvestersOwnerGroup() throws IOException {
        GroupsHelper helper = new GroupsHelper(null);
        Parser parser = new Parser();

        String json = FileUtils.readFileToString(
                ResourceUtils.getFile("classpath:catalogue_noaa_owngroup.json"),
                Charset.forName("UTF-8"));

        Parser.Parsed parsed = parser.parseHarvestersConfig(json);
        Optional<JSONObject> i = helper.getHarvestersOwnerGroup(parsed.getJsonObject());

        assertTrue("Own group exist", i.isPresent());
        assertEquals("Own group id is", i.get().getInt("id"),123);
    }

    @Test
    public void verifyUpdateHarvestersOwnerGroup() throws IOException {
        GroupsHelper helper = new GroupsHelper(null);
        Parser parser = new Parser();

        String json = FileUtils.readFileToString(
                ResourceUtils.getFile("classpath:catalogue_noaa.json"),
                Charset.forName("UTF-8"));

        Group group = new Group();
        group.setName("ABC");
        group.setId(1234);

        Parser.Parsed parsed = parser.parseHarvestersConfig(json);
        Optional<JSONObject> i = helper.getHarvestersOwnerGroup(parsed.getJsonObject());

        assertTrue("No group found", !i.isPresent());

        JSONObject n = helper.updateHarvestersOwnerGroup(parsed.getJsonObject(), group);
        parsed = parser.parseHarvestersConfig(n.toString());

        i = helper.getHarvestersOwnerGroup(parsed.getJsonObject());

        logger.info("{}", i.toString());

        assertTrue("Own group updated and exist", i.isPresent());
        assertEquals("Own group updated id is", i.get().getInt("id"),1234);
    }
}
