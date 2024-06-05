package au.org.aodn.geonetwork4.controller;

import au.org.aodn.geonetwork4.Setup;
import au.org.aodn.geonetwork_api.openapi.api.helper.SiteHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.fao.geonet.domain.Metadata;
import org.fao.geonet.domain.MetadataHarvestInfo;
import org.fao.geonet.domain.MetadataSourceInfo;
import org.fao.geonet.kernel.harvest.HarvestManagerImpl;
import org.fao.geonet.kernel.harvest.harvester.geonet.GeonetHarvester;
import org.fao.geonet.kernel.harvest.harvester.geonet.GeonetParams;
import org.fao.geonet.kernel.harvest.harvester.oaipmh.OaiPmhHarvester;
import org.fao.geonet.kernel.harvest.harvester.oaipmh.OaiPmhParams;
import org.fao.geonet.repository.MetadataRepository;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

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

        HarvestManagerImpl harvestManager = Mockito.mock(HarvestManagerImpl.class);
        when(harvestManager.getHarvester(eq(harvesterUuid)))
                .thenReturn(harvester);

        Api api = new Api(setup, metadataRepository, harvestManager, new ObjectMapper());

        ResponseEntity<Map<String, Object>> v = api.getRecordExtraInfo(uuid);

        assertEquals("Logo have two suggestions", 2, ((List<String>)v.getBody().get(Api.SUGGEST_LOGOS)).size());
        assertEquals("Logo link 1",
                "http://localhost:8080/geonetwork/images/logos/dbee258b-8730-4072-96d4-2818a69a4afd.png",
                ((List<String>)v.getBody().get(Api.SUGGEST_LOGOS)).get(0));
        assertEquals("Logo link 2",
                "https://catalogue-imos.aodn.org.au/geonetwork/images/logos/dbee258b-8730-4072-96d4-2818a69a4afd.png",
                ((List<String>)v.getBody().get(Api.SUGGEST_LOGOS)).get(1));

        // If use other harvester then we will not have remote section
        String oaiHarvesterUrl = "oaiHarvesterUrl";
        OaiPmhHarvester oaiPmhHarvester = Mockito.mock(OaiPmhHarvester.class);
        OaiPmhParams pmhParams = Mockito.mock(OaiPmhParams.class);
        pmhParams.url = oaiHarvesterUrl;

        when(oaiPmhHarvester.getParams())
                .thenReturn(pmhParams);

        when(harvestManager.getHarvester(eq(oaiHarvesterUuid)))
                .thenReturn(oaiPmhHarvester);

        // Set the metadata to use Oai Harvester
        metadata.getHarvestInfo().setUuid(oaiHarvesterUuid);

        v = api.getRecordExtraInfo(uuid);

        // Only one link this time and suggestion is localhost
        assertEquals("Logo have two suggestions", 1, ((List<String>)v.getBody().get(Api.SUGGEST_LOGOS)).size());
        assertEquals("Logo link 1",
                "http://localhost:8080/geonetwork/images/logos/dbee258b-8730-4072-96d4-2818a69a4afd.png",
                ((List<String>)v.getBody().get(Api.SUGGEST_LOGOS)).get(0));

    }
}
