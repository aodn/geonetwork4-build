package au.org.aodn.geonetwork4.controller;

import au.org.aodn.geonetwork4.Setup;
import au.org.aodn.geonetwork4.model.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import jeeves.services.ReadWriteController;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

    protected Logger logger = LogManager.getLogger(Api.class);

    @Autowired
    protected Setup setup;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    @Qualifier("remoteSources")
    protected Map<String, GitRemoteConfig> remoteConfigMap;

    protected RemoteConfig getRemoteConfig(String type) {
        return remoteConfigMap.get(type);
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
                        setup.insertSettings(remote.readJson(items));
                        break;
                    }
                    case logos: {
                        setup.insertLogos(remote.readJson(items));
                        break;
                    }
                    case users: {
                        setup.insertUsers(remote.readJson(items));
                        break;
                    }
                    case categories: {
                        setup.insertCategories(remote.readJson(items));
                        break;
                    }
                    case vocabularies: {
                        setup.insertVocabularies(remote.readJson(items));
                        break;
                    }
                    case groups: {
                        setup.insertGroups(remote.readJson(items));
                        break;
                    }
                    case harvesters: {
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
