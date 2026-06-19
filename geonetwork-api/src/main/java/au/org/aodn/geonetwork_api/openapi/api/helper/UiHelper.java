package au.org.aodn.geonetwork_api.openapi.api.helper;

import au.org.aodn.geonetwork_api.openapi.api.Status;
import au.org.aodn.geonetwork_api.openapi.api.UiApi;
import au.org.aodn.geonetwork_api.openapi.model.UiSetting;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;

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
                        UiSetting uiSetting = api.getUiConfiguration(UI_IDENTIFIER);

                        // Get live GN default UI config
                        String configuration = uiSetting.getConfiguration();
                        JSONObject live = (configuration == null || configuration.isBlank())
                                ? new JSONObject()
                                : new JSONObject(configuration);
                        // Merge the fragment into the live config
                        JSONObject merge = new JSONObject(fragment);
                        deepMerge(live, merge);

                        // Update GN UI config with the merged result
                        uiSetting.setConfiguration(live.toString());
                        api.updateUiConfiguration(UI_IDENTIFIER, uiSetting);

                        status.setStatus(HttpStatus.OK);
                        status.setMessage("Merged UI config fragment into " + UI_IDENTIFIER);
                        logger.info("Merged UI config fragment into {}", UI_IDENTIFIER);
                    }
                    catch (Exception e) {
                        status.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
                        status.setMessage("Failed to merge UI config fragment: " + e.getMessage());
                        logger.error("Failed to merge UI config fragment", e);
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
