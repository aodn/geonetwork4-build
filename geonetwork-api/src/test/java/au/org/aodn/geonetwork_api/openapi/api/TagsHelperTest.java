package au.org.aodn.geonetwork_api.openapi.api;

import au.org.aodn.geonetwork_api.openapi.api.helper.TagsHelper;
import au.org.aodn.geonetwork_api.openapi.model.MetadataCategory;
import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.springframework.util.ResourceUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.junit.Assert.*;

public class TagsHelperTest {

    @Test
    public void verifyGetHarvestersCategories() throws IOException {
        TagsHelper helper = new TagsHelper(null);
        Parser parser = new Parser();

        String json = FileUtils.readFileToString(
                ResourceUtils.getFile("classpath:catalogue_noaa.json"),
                StandardCharsets.UTF_8);

        Parser.Parsed parsed = parser.parseHarvestersConfig(json);
        Optional<JSONArray> i = helper.getHarvestersCategories(parsed.getJsonObject());

        assertFalse("Categories not exist", i.isPresent());

        json = FileUtils.readFileToString(
                ResourceUtils.getFile("classpath:portal_catalogue_aims.json"),
                StandardCharsets.UTF_8);

        parsed = parser.parseHarvestersConfig(json);
        i = helper.getHarvestersCategories(parsed.getJsonObject());

        assertTrue("Categories exist", i.isPresent());
        assertEquals("Categories ID is correct", i.get().getJSONObject(0).getJSONObject("category").getString("-id"), "portal:AIMS");
    }
    /**
     * This test is important because we need to add attribute into the JSON and it needs be -fieldname due to
     * use of underscore java lib for JSON parsing
     */
    @Test
    public void verifyUpdateHarvestersCategories() throws IOException {
        TagsHelper helper = new TagsHelper(null);

        String json = FileUtils.readFileToString(
                ResourceUtils.getFile("classpath:portal_catalogue_aims.json"),
                StandardCharsets.UTF_8);

        MetadataCategory metadataCategory = new MetadataCategory();
        metadataCategory.setId(170);

        JSONObject result = helper.updateHarvestersCategories(new JSONObject(json), metadataCategory);
        Optional<JSONArray> target = helper.getHarvestersCategories(result);

        assertTrue("Category exist", target.isPresent());
        assertEquals(
                "Id updated",
                170,
                target.get().getJSONObject(0).getJSONObject("category").getInt("-id"));
    }
}
