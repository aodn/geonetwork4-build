package au.org.aodn.geonetwork4.controller;

import au.org.aodn.geonetwork4.Setup;
import au.org.aodn.geonetwork4.model.*;
import au.org.aodn.geonetwork_api.openapi.api.helper.SiteHelper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import jeeves.services.ReadWriteController;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.fao.geonet.domain.Group;
import org.fao.geonet.domain.Metadata;
import org.fao.geonet.kernel.harvest.HarvestManagerImpl;
import org.fao.geonet.kernel.harvest.harvester.AbstractHarvester;
import org.fao.geonet.kernel.harvest.harvester.AbstractParams;
import org.fao.geonet.kernel.harvest.harvester.HarvestResult;
import org.fao.geonet.kernel.harvest.harvester.csw.CswHarvester;
import org.fao.geonet.kernel.harvest.harvester.geonet.GeonetHarvester;
import org.fao.geonet.kernel.harvest.harvester.geonet20.Geonet20Harvester;
import org.fao.geonet.kernel.harvest.harvester.oaipmh.OaiPmhHarvester;
import org.fao.geonet.kernel.harvest.harvester.ogcwxs.OgcWxSHarvester;
import org.fao.geonet.kernel.harvest.harvester.webdav.WebDavHarvester;
import org.fao.geonet.repository.GroupRepository;
import org.fao.geonet.repository.MetadataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.*;

import java.util.stream.Collectors;

/**
 * Any api call here request a header X-XSRF-TOKEN to be presented, this is due to geonetwork4 enabled crsf(). The token
 * can be obtained by making a call with username/password, then the return header contains a XSRF-TOKEN which contains
 * the token value.
 */
@Controller("aodn")
@ReadWriteController
@RequestMapping(value = {"/{portal}/api/aodn"})
public class Api {

    public static final String SUGGEST_LOGOS = "suggest_logos";

    protected Logger logger = LogManager.getLogger(Api.class);

    protected Setup setup;
    protected MetadataRepository repository;
    protected HarvestManagerImpl harvestManager;
    protected GroupRepository groupRepository;
    protected ObjectMapper objectMapper;

    @Autowired
    @Qualifier("remoteSources")
    protected Map<String, GitRemoteConfig> remoteConfigMap;

    public Api(Setup setup,
               MetadataRepository metadataRepository,
               HarvestManagerImpl harvestManager,
               GroupRepository groupRepository,
               ObjectMapper objectMapper) {
        this.harvestManager = harvestManager;
        this.repository = metadataRepository;
        this.setup = setup;
        this.groupRepository = groupRepository;
        this.objectMapper = objectMapper;
    }

    protected RemoteConfig getRemoteConfig(String type) {
        return remoteConfigMap.get(type);
    }
    /**
     * HACK!!
     * This function is used to expose something not found in the rest api, it used the jpa api to get the
     * metadata object itself and then expose the additional values, this object contains the sourceId
     * which is the uuid of the record from the source system being harvested, it different from the
     * UUID use in this geonetwork, because harvested record get assign a new UUID locally.
     * The logos are prefer logo that order by prefence, that means we prefer
     * 1. the logo from the metadata if downloaded
     * 2. the logo from the metadata store remotely in source system (where this metadata harvested from)
     * 3. the logo of the harvester
     * 4. the logo of the group that the harvester belongs
     *
     * @param uuid - UUID of the record use by this geonetwork
     * @return - A data structure contains the UUID of the record in the source system as well as suggested logo in order of possibility
     * {
     *     "sourceId": "dbee258b-8730-4072-96d4-2818a69a4afd",
     *     "schemaid": "iso19115-3.2018",
     *     "harvesterUri": "https://catalogue-imos.aodn.org.au/geonetwork",
     *     "suggest_logos": [
     *         "http://localhost:8080/geonetwork/images/logos/dbee258b-8730-4072-96d4-2818a69a4afd.png",  <-- likely the icon store locally
     *         "https://catalogue-imos.aodn.org.au/geonetwork/images/logos/dbee258b-8730-4072-96d4-2818a69a4afd.png" <-- the icon that store from metadata source server
     *         "https://localhost:8080/geonetwork/images/harvesting/... " <-- the icon from harvester
     *         "https://localhost:8080/geonetwork/images/harvesting/... " <-- the icon use by the group and this metadata belongs to this group
     *     ],
     *     "isHarvested": true,
     *     "harvesterType": "GeonetHarvester"
     * }
     */
    @GetMapping("/records/{uuid}/info")
    public ResponseEntity<Map<String, Object>> getRecordExtraInfo(@PathVariable("uuid") String uuid) {
        Map<String, Object> info = new HashMap<>();
        // The insert order is important as this is the order of suggested icon
        List<String> logos = new ArrayList<>();
        info.put(SUGGEST_LOGOS, logos);

        final String host = setup.getSiteSetting(SiteHelper.HOST);
        final String port = setup.getSiteSetting(SiteHelper.PORT);
        final String protocol = setup.getSiteSetting(SiteHelper.PROTOCOL);

        Metadata metadata = repository.findOneByUuid(uuid);
        if(metadata != null) {
            if(metadata.getSourceInfo() != null) {
                // Here we can get the source id, then we can create the first option for logo
                // which is extract logo from this host
                info.put("sourceId", metadata.getSourceInfo().getSourceId());
                // Default logo location of record
                logos.add(String.format("%s://%s:%s/geonetwork/images/logos/%s.png", protocol, host, port, info.get("sourceId")));
            }

            // We can also get the harvester uuid, from there we can get the harvester url
            if(metadata.getHarvestInfo() != null) {
                info.put("isHarvested", metadata.getHarvestInfo().isHarvested());

                Object harvester = harvestManager.getHarvester(metadata.getHarvestInfo().getUuid());
                if(harvester != null) {
                    // Set the harvester class
                    info.put("harvesterType", harvester.getClass());
                    if (harvester instanceof GeonetHarvester) {
                        GeonetHarvester h = (GeonetHarvester) harvester;
                        info.put("harvesterName", h.getParams().getName());
                        info.put("harvesterUri", StringUtils.removeEnd(h.getParams().host, "/"));
                        // The geonetwork store logo under this dir with the uuid name, we provide a list of suggestion
                        // on where to find the logo
                        logos.add(String.format("%s/images/logos/%s.png", info.get("harvesterUri"), info.get("sourceId")));
                    }
                    else if (harvester instanceof OaiPmhHarvester) {
                        // If non GN harvester  e.g. OAI then logo from harvester
                        info.put("harvesterUri", StringUtils.removeEnd(((OaiPmhHarvester) harvester).getParams().url, "/"));
                    }
                    else if (harvester instanceof CswHarvester) {
                        // Will have remote link to logo
                        info.put("harvesterUri", StringUtils.removeEnd(((CswHarvester) harvester).getParams().capabUrl, "/"));
                    }
                    else if (harvester instanceof OgcWxSHarvester) {
                        // Will have remote link to logo
                        info.put("harvesterUri", StringUtils.removeEnd(((OgcWxSHarvester) harvester).getParams().url, "/"));
                    }
                    else if (harvester instanceof Geonet20Harvester) {
                        // Will have remote link to logo
                        info.put("harvesterUri", StringUtils.removeEnd(((Geonet20Harvester) harvester).getParams().host, "/"));
                    }
                    else if (harvester instanceof WebDavHarvester) {
                        // Will have remote link to logo
                        info.put("harvesterUri", StringUtils.removeEnd(((WebDavHarvester) harvester).getParams().url, "/"));
                    }
                    else {
                        logger.error("Unknown instanceof type for harvester {}", harvester.getClass());
                    }
                    // Get icon
                    @SuppressWarnings("unchecked")
                    AbstractHarvester<HarvestResult, AbstractParams> h = (AbstractHarvester<HarvestResult, AbstractParams>) harvester;
                    if(h.getParams().getIcon() != null) {
                        logos.add(String.format("%s://%s:%s/geonetwork/images/harvesting/%s", protocol, host, port, h.getParams().getIcon()));
                    }
                    // Get icon from group
                    if(h.getParams().getOwnerIdGroup() != null) {
                        try {
                            Optional<Group> group = groupRepository.findById(Integer.parseInt(h.getParams().getOwnerIdGroup()));
                            group.ifPresent(g -> logos.add(String.format("%s://%s:%s/geonetwork/images/harvesting/%s", protocol, host, port, g.getLogo())));
                        }
                        catch(Exception nfe) {
                            // If the group is not a number then ignore it.
                        }
                    }

                }
            }

            if(metadata.getDataInfo() != null) {
                info.put("schemaid", metadata.getDataInfo().getSchemaId());
            }
        }
        return ResponseEntity.ok(info);
    }

    @GetMapping("/setup/harvesters")
    public ResponseEntity<String> getAllHarvesters() {
        return ResponseEntity.ok(setup.getAllHarvesters().getBody());
    }

    @DeleteMapping("/setup/harvesters")
    public ResponseEntity<?> deleteAllHarvesters() {
        setup.deleteAllHarvesters();
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/setup/categories")
    public ResponseEntity<?> deleteAllCategories() {
        setup.deleteAllCategories();
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/setup/group")
    public ResponseEntity<?> deleteAllGroup() {
        setup.deleteAllGroup();
        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "/setup", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> updateConfig(
            @RequestParam(value="source", defaultValue = "github") String source,
            @RequestBody(required = false) List<RemoteConfigValue> remoteConfigValue) throws JsonProcessingException {

        RemoteConfig remote = getRemoteConfig(source);

        if(remote != null) {

            if(remoteConfigValue == null) {
                // Use default config
                remoteConfigValue = remote.getDefaultConfig();
                logger.info("Loaded default config from '{}'", remote);

                ObjectWriter ow = objectMapper.writer().withDefaultPrettyPrinter();
                logger.info(ow.writeValueAsString(remoteConfigValue));
            }

            // Group the config based on type
            Map<ConfigTypes, List<RemoteConfigValue>> groups = remoteConfigValue.stream().collect(Collectors.groupingBy(RemoteConfigValue::getType));

            // We need to add in certain order
            List<ConfigTypes> types = new ArrayList<>();
            types.add(ConfigTypes.settings);
            types.add(ConfigTypes.logos);
            types.add(ConfigTypes.categories);
            types.add(ConfigTypes.vocabularies);
            types.add(ConfigTypes.groups);
            types.add(ConfigTypes.users);
            types.add(ConfigTypes.harvesters);

            for (ConfigTypes type : types) {
                List<RemoteConfigValue> items = groups.get(type);

                // Avoid null pointer if nothing to process
                if(items == null) continue;

                switch (type) {
                    case settings: {
                        logger.info("Processing settings");
                        setup.insertSettings(remote.readJson(items));
                        break;
                    }
                    case logos: {
                        logger.info("Processing logos");
                        setup.insertLogos(remote.readJson(items));
                        break;
                    }
                    case users: {
                        logger.info("Processing users");
                        setup.insertUsers(remote.readJson(items));
                        break;
                    }
                    case categories: {
                        logger.info("Processing categories");
                        setup.insertCategories(remote.readJson(items));
                        break;
                    }
                    case vocabularies: {
                        logger.info("Processing vocabularies");
                        setup.insertVocabularies(remote.readJson(items));
                        break;
                    }
                    case groups: {
                        logger.info("Processing groups");
                        setup.insertGroups(remote.readJson(items));
                        break;
                    }
                    case harvesters: {
                        logger.info("Processing harvesters");
                        setup.insertHarvester(remote.readJson(items));
                        break;
                    }
                }
            }
            return ResponseEntity.ok(null);
        }
        else {
            return ResponseEntity.badRequest().body("Unknown source type specified.");
        }
    }
}
