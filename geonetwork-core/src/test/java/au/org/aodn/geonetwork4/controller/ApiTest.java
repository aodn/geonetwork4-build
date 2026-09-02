package au.org.aodn.geonetwork4.controller;

import au.org.aodn.geonetwork4.PortalSyncSwitch;
import au.org.aodn.geonetwork4.Setup;
import au.org.aodn.geonetwork4.model.ConfigTypes;
import au.org.aodn.geonetwork4.model.GitRemoteConfig;
import au.org.aodn.geonetwork4.model.RemoteConfigValue;
import au.org.aodn.geonetwork_api.openapi.api.helper.SiteHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.fao.geonet.domain.Group;
import org.fao.geonet.domain.Metadata;
import org.fao.geonet.domain.MetadataHarvestInfo;
import org.fao.geonet.domain.MetadataSourceInfo;
import org.fao.geonet.kernel.harvest.HarvestManagerImpl;

import org.fao.geonet.kernel.harvest.harvester.geonet.v21_3.GeonetHarvester;
import org.fao.geonet.kernel.harvest.harvester.geonet.v21_3.GeonetParams;
import org.fao.geonet.kernel.harvest.harvester.oaipmh.OaiPmhHarvester;
import org.fao.geonet.kernel.harvest.harvester.oaipmh.OaiPmhParams;
import org.fao.geonet.repository.GroupRepository;
import org.fao.geonet.repository.MetadataRepository;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class ApiTest {

    @Test
    public void verifyRecordExtraInfoWorks() {
        String uuid = "2852a776-cbfc-4bc8-a126-f3c036814892";
        String sourceId = "dbee258b-8730-4072-96d4-2818a69a4afd";
        String harvesterUuid = "1234";
        String oaiHarvesterUuid = "oai1234";

        MetadataRepository metadataRepository = Mockito.mock(MetadataRepository.class);

        Metadata metadata = new Metadata();

        metadata.setSourceInfo(new MetadataSourceInfo());
        metadata.getSourceInfo().setSourceId(sourceId);

        metadata.setHarvestInfo(new MetadataHarvestInfo());
        metadata.getHarvestInfo().setHarvested(true);
        metadata.getHarvestInfo().setUuid(harvesterUuid);

        when(metadataRepository.findOneByUuid(eq(uuid)))
                .thenReturn(metadata);

        Setup setup = Mockito.mock(Setup.class);

        when(setup.getSiteSetting(eq(SiteHelper.HOST)))
                .thenReturn("localhost");
        when(setup.getSiteSetting(eq(SiteHelper.PORT)))
                .thenReturn("8080");
        when(setup.getSiteSetting(eq(SiteHelper.PROTOCOL)))
                .thenReturn("http");

        // Geonetwork harvester
        GeonetHarvester harvester = new GeonetHarvester();
        GeonetParams params = Mockito.mock(GeonetParams.class);
        params.host = "https://catalogue-imos.aodn.org.au/geonetwork";
        harvester.setParams(params);

        when(params.getOwnerIdGroup())
                .thenReturn("100");

        HarvestManagerImpl harvestManager = Mockito.mock(HarvestManagerImpl.class);
        when(harvestManager.getHarvester(eq(harvesterUuid)))
                .thenReturn(harvester);

        Group group = new Group();
        group.setLogo("IMOS_colour_logo.png");

        GroupRepository groupRepository = Mockito.mock(GroupRepository.class);
        when(groupRepository.findById(anyInt()))
                .thenReturn(Optional.of(group));

        Api api = new Api(setup, metadataRepository, harvestManager, groupRepository, new ObjectMapper(), Mockito.mock(PortalSyncSwitch.class));

        ResponseEntity<Map<String, Object>> v = api.getRecordExtraInfo(uuid);

        Assert.assertNotNull(v.getBody());
        Assert.assertEquals("GeonetHarvester logo suggestions", 3, ((List<?>)v.getBody().get(Api.SUGGEST_LOGOS)).size());
        Assert.assertEquals("GeonetHarvester logo link 1",
                "http://localhost:8080/geonetwork/images/harvesting/IMOS_colour_logo.png",
                ((List<?>)v.getBody().get(Api.SUGGEST_LOGOS)).get(0));
        Assert.assertEquals("GeonetHarvester logo link 2",
                "http://localhost:8080/geonetwork/images/logos/dbee258b-8730-4072-96d4-2818a69a4afd.png",
                ((List<?>)v.getBody().get(Api.SUGGEST_LOGOS)).get(1));
        Assert.assertEquals("GeonetHarvester logo link 3",
                "https://catalogue-imos.aodn.org.au/geonetwork/images/logos/dbee258b-8730-4072-96d4-2818a69a4afd.png",
                ((List<?>)v.getBody().get(Api.SUGGEST_LOGOS)).get(2));

        // If the GeoNetwork harvester's group has no usable logo, retain the source-logo fallbacks.
        group.setLogo(null);
        v = api.getRecordExtraInfo(uuid);

        Assert.assertNotNull(v.getBody());
        Assert.assertEquals("GeonetHarvester fallback logo suggestions", 2, ((List<?>)v.getBody().get(Api.SUGGEST_LOGOS)).size());
        Assert.assertEquals("GeonetHarvester fallback logo link 1",
                "http://localhost:8080/geonetwork/images/logos/dbee258b-8730-4072-96d4-2818a69a4afd.png",
                ((List<?>)v.getBody().get(Api.SUGGEST_LOGOS)).get(0));
        Assert.assertEquals("GeonetHarvester fallback logo link 2",
                "https://catalogue-imos.aodn.org.au/geonetwork/images/logos/dbee258b-8730-4072-96d4-2818a69a4afd.png",
                ((List<?>)v.getBody().get(Api.SUGGEST_LOGOS)).get(1));

        group.setLogo("IMOS_colour_logo.png");

        // If use other harvester then we will not have remote section
        String oaiHarvesterUrl = "oaiHarvesterUrl";
        OaiPmhHarvester oaiPmhHarvester = Mockito.mock(OaiPmhHarvester.class);
        OaiPmhParams pmhParams = Mockito.mock(OaiPmhParams.class);
        pmhParams.url = oaiHarvesterUrl;

        when(pmhParams.getIcon())
                .thenReturn("logo.gif");

        when(pmhParams.getOwnerIdGroup())
                .thenReturn("100");

        when(oaiPmhHarvester.getParams())
                .thenReturn(pmhParams);

        when(harvestManager.getHarvester(eq(oaiHarvesterUuid)))
                .thenReturn(oaiPmhHarvester);

        // Set the metadata to use Oai Harvester
        metadata.getHarvestInfo().setUuid(oaiHarvesterUuid);

        v = api.getRecordExtraInfo(uuid);

        // Only one link this time and suggestion is localhost
        Assert.assertNotNull(v.getBody());
        Assert.assertEquals("OaiPmhHarvester logo suggestions", 3, ((List<?>)v.getBody().get(Api.SUGGEST_LOGOS)).size());
        Assert.assertEquals("OaiPmhHarvester logo link 1",
                "http://localhost:8080/geonetwork/images/logos/dbee258b-8730-4072-96d4-2818a69a4afd.png",
                ((List<?>)v.getBody().get(Api.SUGGEST_LOGOS)).get(0));
        Assert.assertEquals("OaiPmhHarvester logo link 2",
                "http://localhost:8080/geonetwork/images/harvesting/logo.gif",
                ((List<?>)v.getBody().get(Api.SUGGEST_LOGOS)).get(1));
        Assert.assertEquals("OaiPmhHarvester logo link 3",
                "http://localhost:8080/geonetwork/images/harvesting/IMOS_colour_logo.png",
                ((List<?>)v.getBody().get(Api.SUGGEST_LOGOS)).get(2));
    }

    // One entry of config.json, e.g. { "type": "harvesters", "jsonFileName": "..." }
    protected RemoteConfigValue configEntry(ConfigTypes type) {
        RemoteConfigValue entry = new RemoteConfigValue();
        entry.setType(type);
        entry.setJsonFileName("any.json");
        return entry;
    }
    /**
     * POST /setup disables portal sync when the config includes harvesters, they re-harvest everything.
     */
    @Test
    public void verifySetupDisablesPortalSync() throws Exception {
        // Setup reads the config files from github, nothing needed for this test
        GitRemoteConfig github = Mockito.mock(GitRemoteConfig.class);
        when(github.withRef("main")).thenReturn(github);
        when(github.exists()).thenReturn(true);
        when(github.readJson(anyList())).thenReturn(List.of());

        PortalSyncSwitch portalSyncSwitch = Mockito.mock(PortalSyncSwitch.class);

        Api api = new Api(Mockito.mock(Setup.class), Mockito.mock(MetadataRepository.class),
                Mockito.mock(HarvestManagerImpl.class), Mockito.mock(GroupRepository.class),
                new ObjectMapper(), portalSyncSwitch);
        api.remoteConfigMap = Map.of("github", github);

        // With harvesters: disabled
        api.updateConfig("github", "main", List.of(configEntry(ConfigTypes.logos), configEntry(ConfigTypes.harvesters)));
        verify(portalSyncSwitch, times(1)).disable();

        // Without harvesters: not called again
        api.updateConfig("github", "main", List.of(configEntry(ConfigTypes.logos), configEntry(ConfigTypes.uiConfig)));
        verify(portalSyncSwitch, times(1)).disable();
    }

    // ---- POST /setup?ref=... reads the config from a tag, branch or commit ----

    protected Api apiWith(GitRemoteConfig github) {
        Api api = new Api(Mockito.mock(Setup.class), Mockito.mock(MetadataRepository.class),
                Mockito.mock(HarvestManagerImpl.class), Mockito.mock(GroupRepository.class),
                new ObjectMapper(), Mockito.mock(PortalSyncSwitch.class));
        api.remoteConfigMap = Map.of("github", github);
        return api;
    }
    /**
     * No ref: 400 telling the caller how, a default could silently read a drifted main
     */
    @Test
    public void verifyMissingRefFails() throws Exception {
        GitRemoteConfig startBranch = Mockito.mock(GitRemoteConfig.class);

        ResponseEntity<?> response = apiWith(startBranch)
                .updateConfig("github", null, List.of(configEntry(ConfigTypes.logos)));

        Assert.assertEquals("Bad request", 400, response.getStatusCode().value());
        verify(startBranch, never()).readJson(anyList());
    }
    /**
     * With a ref: the config comes from that ref, the start branch is not read
     */
    @Test
    public void verifyRefReadsFromThatRef() throws Exception {
        GitRemoteConfig startBranch = Mockito.mock(GitRemoteConfig.class);
        GitRemoteConfig tagged = Mockito.mock(GitRemoteConfig.class);
        when(startBranch.withRef("v0.0.36")).thenReturn(tagged);
        when(tagged.exists()).thenReturn(true);
        when(tagged.readJson(anyList())).thenReturn(List.of());

        ResponseEntity<?> response = apiWith(startBranch)
                .updateConfig("github", "v0.0.36", List.of(configEntry(ConfigTypes.logos)));

        Assert.assertTrue("Ok", response.getStatusCode().is2xxSuccessful());
        verify(tagged, times(1)).readJson(anyList());
        verify(startBranch, never()).readJson(anyList());
    }
    /**
     * A ref that does not exist fails with 400 instead of setting up nothing
     */
    @Test
    public void verifyUnknownRefFails() throws Exception {
        GitRemoteConfig startBranch = Mockito.mock(GitRemoteConfig.class);
        GitRemoteConfig tagged = Mockito.mock(GitRemoteConfig.class);
        when(startBranch.withRef("v9.9.9")).thenReturn(tagged);
        when(tagged.exists()).thenReturn(false);

        ResponseEntity<?> response = apiWith(startBranch)
                .updateConfig("github", "v9.9.9", List.of(configEntry(ConfigTypes.logos)));

        Assert.assertEquals("Bad request", 400, response.getStatusCode().value());
        Assert.assertEquals("No config found for ref 'v9.9.9'", response.getBody());
    }
    /**
     * What a ref may look like: tags, branches with slashes, commits are fine, anything that could
     * change the url path is not
     */
    @Test
    public void verifyRefFormats() {
        Api api = apiWith(Mockito.mock(GitRemoteConfig.class));

        for (String valid : List.of(
                "v0.0.36",                                     // release tag
                "main",                                        // branch
                "bugfix/8887-data-density-console-error",      // branch with slash
                "8f3a2c1d9b7e6f5a4c3b2a1d9e8f7a6b5c4d3e2f",    // commit
                "release_1.2")) {
            Assert.assertTrue("Valid: " + valid, api.isValidRef(valid));
        }

        for (String invalid : List.of(
                "../evil",              // escapes to another repository
                "a/../b",               // same, hidden in the middle
                "/main",                // empty first segment
                "main/",                // empty last segment
                "a//b",                 // empty middle segment
                "v0.0.36 evil",         // whitespace
                "")) {                  // empty
            Assert.assertFalse("Invalid: " + invalid, api.isValidRef(invalid));
        }
    }
}
