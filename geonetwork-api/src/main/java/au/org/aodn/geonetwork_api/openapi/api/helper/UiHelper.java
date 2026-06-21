package au.org.aodn.geonetwork_api.openapi.api.helper;

import au.org.aodn.geonetwork_api.openapi.api.Status;
import au.org.aodn.geonetwork_api.openapi.api.UiApi;
import au.org.aodn.geonetwork_api.openapi.model.UiSetting;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

import java.util.List;
import java.util.stream.Collectors;

public class UiHelper {


    // GeoNetwork's default UI configuration identifier.
    public static final String UI_IDENTIFIER = "srv";

    protected Logger logger = LogManager.getLogger(UiHelper.class);
    protected UiApi api;

    public UiHelper(UiApi api) {
        this.api = api;
    }

    public UiApi getApi() { return api; }

    /**
     * Deep-merge each incoming fragment into the live UI configuration.
     * Re-running is a no-op (the same leaves are written back)
     * so it is safe to call on every setup.
     *
     * @param config - list of fragment json strings
     * @return the merge status per fragment
     */
    public List<Status> updateUiConfig(List<String> config) {
        return config.stream()
                .map(fragment -> {
                    Status status = new Status();
                    status.setFileContent(fragment);

                    try {
                        boolean exists = true;
                        UiSetting uiSetting;
                        try {
                            ResponseEntity<UiSetting> live = api.getUiConfigurationWithHttpInfo(UI_IDENTIFIER);
                            uiSetting = live.getBody();
                        }
                        catch (HttpClientErrorException.NotFound notFound) {
                            exists = false;
                            uiSetting = new UiSetting();
                            uiSetting.setId(UI_IDENTIFIER);
                            uiSetting.setConfiguration(null);
                        }

                        // Get live GN default UI config (empty when newly created)
                        String configuration = uiSetting.getConfiguration();
                        JSONObject live = (configuration == null || configuration.isBlank())
                                ? new JSONObject()
                                : new JSONObject(configuration);

                        // Merge the fragment into the live config
                        JSONObject merge = new JSONObject(fragment);
                        deepMerge(live, merge);
                        uiSetting.setConfiguration(live.toString());

                        // Update the existing record, or create it when absent
                        if (exists) {
                            api.updateUiConfigurationWithHttpInfo(UI_IDENTIFIER, uiSetting);
                        }
                        else {
                            api.putUiConfigurationWithHttpInfo(uiSetting);
                        }

                        status.setStatus(HttpStatus.OK);
                        String action = exists ? "Merged UI config fragment into" : "Created UI config";
                        status.setMessage(action + " " + UI_IDENTIFIER);
                    }
                    catch (Exception e) {
                        status.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
                        status.setMessage("Failed to merge UI config fragment: " + e.getMessage());
                    }
                    return status;
                })
                .collect(Collectors.toList());
    }

    protected void deepMerge(JSONObject target, JSONObject source) {
        for (String key : source.keySet()) {
            Object sourceValue = source.get(key);
            Object targetValue = target.opt(key);

            if (sourceValue instanceof JSONObject && targetValue instanceof JSONObject) {
                deepMerge((JSONObject) targetValue, (JSONObject) sourceValue);
            }
            else {
                target.put(key, sourceValue);
            }
        }
    }
}
