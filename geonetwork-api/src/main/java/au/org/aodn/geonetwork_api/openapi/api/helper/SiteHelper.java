package au.org.aodn.geonetwork_api.openapi.api.helper;

import au.org.aodn.geonetwork_api.openapi.api.SiteApi;
import au.org.aodn.geonetwork_api.openapi.api.Status;
import au.org.aodn.geonetwork_api.openapi.model.Setting;
import au.org.aodn.geonetwork_api.openapi.model.SystemInfo;
import org.json.JSONObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class SiteHelper {

    protected static final String SETTINGS = "settings";
    protected static final String DATA = "data";

    protected Logger logger = LogManager.getLogger(SiteHelper.class);
    protected SiteApi api;

    public SiteHelper(SiteApi api) {
        this.api = api;
    }

    public SiteApi getApi() { return api; }

    public List<Status> createSettings(List<String> json) {
        return json.stream()
                .map(m -> {
                    JSONObject jsonObject = new JSONObject(m);

                    Status status = new Status();
                    status.setFileContent(m);

                    if(jsonObject.optJSONObject(SETTINGS) != null
                            && jsonObject.optJSONObject(SETTINGS).optJSONObject(DATA) != null) {

                        Set<String> keys = new HashSet<>();

                        ResponseEntity<List<Setting>> storedSetting = this.api.getSettingsDetailsWithHttpInfo(null, null);

                        if(storedSetting.getStatusCode().is2xxSuccessful()) {
                            // Get the available keys in the geonetwork4 system.
                            Set<String> s = storedSetting.getBody()
                                    .stream()
                                    .map(k -> k.getName())
                                    .collect(Collectors.toSet());

                            keys.addAll(s);
                        }

                        // Convert map from <String, Object> to <String, String>
                        Map<String, String> settings = jsonObject.
                                getJSONObject(SETTINGS)
                                .getJSONObject(DATA)
                                .toMap()
                                .entrySet()
                                .stream()
                                .filter(f -> {
                                    if(keys.contains(f.getKey())) {
                                        logger.info("Key exist {}", f.getKey());
                                        return true;
                                    }
                                    else {
                                        logger.info("Key not exist {} - Skip", f.getKey());
                                        return false;
                                    }
                                })  // If the setting isn't in geonetwork4, you cannot set it.
                                .collect(Collectors.toMap(Map.Entry::getKey, e -> String.valueOf(e.getValue())));

                        logger.info("Processing setting {}", settings);
                        ResponseEntity<Void> response = null;
                        try {
                            response = this.api.saveSettingsWithHttpInfo(settings);
                            status.setMessage(settings.toString());
                            status.setStatus(response.getStatusCode());
                        }
                        catch(HttpClientErrorException.BadRequest badRequest) {
                            status.setStatus(badRequest.getStatusCode());
                            status.setMessage("Insert setting failed - %s already exist?");
                        }
                        catch(Exception e) {
                            logger.error(e.getMessage());
                        }
                        finally {
                            if(response != null) {
                                status.setStatus(response.getStatusCode());
                            }

                            logger.info("Processed setting");
                            return status;
                        }
                    }
                    else {
                        status.setStatus(HttpStatus.BAD_REQUEST);
                        status.setMessage("Setting format incorrect, expect settings and data key");
                        return status;
                    }
                })
                .collect(Collectors.toList());
    }
}
