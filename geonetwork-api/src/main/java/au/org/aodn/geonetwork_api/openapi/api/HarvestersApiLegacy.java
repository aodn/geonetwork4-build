package au.org.aodn.geonetwork_api.openapi.api;

import au.org.aodn.geonetwork_api.openapi.invoker.ApiClient;
import au.org.aodn.geonetwork_api.openapi.model.Group;
import au.org.aodn.geonetwork_api.openapi.model.HarvestersApiLegacyResponse;
import au.org.aodn.geonetwork_api.openapi.model.MetadataCategory;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * The current api provided limited support on harvester operation, hence we implement the missing. We can demise
 * the code here in the future when api get mature.
 */
public class HarvestersApiLegacy extends HarvestersApi {

    protected final static String ENDPOINT_HARVESTER_ADD = "/admin.harvester.add";
    protected Logger logger = LogManager.getLogger(HarvestersApiLegacy.class);

    protected GroupsHelper groupsHelper;
    protected TagsHelper tagsHelper;

    // We use aop proxy, so we cannot use "this" for any function call but call the bean
    @Autowired
    protected HarvestersApiLegacy proxyHarvestersApiLegacy;

    public HarvestersApiLegacy(ApiClient client, GroupsApi groupApi, TagsApi tagsApi) {
        super(client);
        this.groupsHelper = new GroupsHelper(groupApi);
        this.tagsHelper = new TagsHelper(tagsApi);
        client.setBasePath("http://localhost:8080/geonetwork/srv/eng");
    }

    @Override
    public String assignHarvestedRecordToSource(String harvesterUuid, String source) throws RestClientException {
        throw new NotImplementedException("Please use HarvesterApi instead");
    }

    @Override
    public ResponseEntity<String> assignHarvestedRecordToSourceWithHttpInfo(String harvesterUuid, String source) throws RestClientException {
        throw new NotImplementedException("Please use HarvesterApi instead");
    }

    @Override
    public String checkHarvesterPropertyExist(String property, String exist) throws RestClientException {
        throw new NotImplementedException("Please use HarvesterApi instead");
    }

    @Override
    public ResponseEntity<String> checkHarvesterPropertyExistWithHttpInfo(String property, String exist) throws RestClientException {
        throw new NotImplementedException("Please use HarvesterApi instead");
    }
    /**
     * It won't check duplicate
     * @param config
     * @return
     */
    public List<HarvestersApiLegacyResponse> createHarvesters(List<String> config) {

         return config
                .stream()
                .map(v -> {
                    Parser.Parsed parsed = null;
                    try {
                        Parser parser = new Parser();
                        parsed = parser.convertHarvestersJsonToXml(v);

                        // Logic copy from legacy code, if group exist in the config, we need to setup the group id.
                        Optional<JSONObject> groupAttr = groupsHelper.getHarvestersOwnerGroup(parsed.getJsonObject());
                        if(groupAttr.isPresent()) {
                            // Check if group name already exist, if yes we get the group id and set the
                            // group id.
                            Optional<Group> group = groupsHelper.findGroup(groupAttr.get().getString("name"));
                            if(group.isPresent()) {
                                // Re-parse the jsonobject due to value updated
                                parsed = parser.convertHarvestersJsonToXml(
                                        groupsHelper.updateHarvestersOwnerGroup(parsed.getJsonObject(), group.get()).toString());
                            }
                        }

                        Optional<JSONArray> categories = tagsHelper.getHarvestersCategories(parsed.getJsonObject());
                        if(categories.isPresent()) {
                            // TODO: In the legacy, there is a comment to said support first category only, Why???
                            // @craig may have info
                            Optional<MetadataCategory> category = tagsHelper.findTag(
                                    categories
                                            .get()
                                            .getJSONObject(0)
                                            .getJSONObject("category")
                                            .getString("@id"));

                            if(category.isPresent()) {
                                parsed = parser.convertHarvestersJsonToXml(
                                        tagsHelper.updateHarvestersCategories(parsed.getJsonObject(), category.get()).toString());
                            }
                        }

                        logger.info("Adding harvestors config {}", parsed.getXml());

                        ResponseEntity<Map<String, Object>> r = proxyHarvestersApiLegacy.createHarvestersWithHttpInfo(parsed);

                        HarvestersApiLegacyResponse hr = new HarvestersApiLegacyResponse();
                        hr.setStatus(r.getStatusCode());
                        hr.setName(parsed.getJsonObject().getString("name"));

                        if(hr.getStatus().is2xxSuccessful()) {
                            hr.setId(r.getBody().get("id").toString());
                        }
                        return hr;
                    }
                    catch (HttpServerErrorException.InternalServerError i) {
                        logger.error("Fail to add the config, error is {} {}",
                                i.getStatusCode(),
                                i.getResponseBodyAsString());

                        HarvestersApiLegacyResponse hr = new HarvestersApiLegacyResponse();
                        hr.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
                        hr.setError(i.getMessage());

                        if(parsed != null) {
                            hr.setName(parsed.getJsonObject().getString("name"));
                        }

                        return hr;
                    }
                    catch (JsonProcessingException e) {
                        HarvestersApiLegacyResponse hr = new HarvestersApiLegacyResponse();
                        hr.setStatus(HttpStatus.BAD_REQUEST);
                        hr.setError(String.format("Cannot parse config -> %s", v));

                        return hr;
                    }
                })
                .collect(Collectors.toList());
    }
    /**
     * This method is needed to allow AspectJ intercept
     * @param parsed
     * @return
     */
    public ResponseEntity<Map<String, Object>> createHarvestersWithHttpInfo(Parser.Parsed parsed) {

        HttpHeaders localVarHeaderParams = new HttpHeaders();

        MultiValueMap<String, String> localVarQueryParams = new LinkedMultiValueMap();
        MultiValueMap<String, String> localVarCookieParams = new LinkedMultiValueMap();
        MultiValueMap<String, Object> localVarFormParams = new LinkedMultiValueMap();

        String[] localVarAuthNames = new String[0];
        ParameterizedTypeReference<Map<String, Object>> localReturnType = new ParameterizedTypeReference<>() {};

        String[] localVarAccepts = new String[]{"*/*", "application/json"};
        List<MediaType> localVarAccept = this.getApiClient().selectHeaderAccept(localVarAccepts);


        return proxyHarvestersApiLegacy.getApiClient()
                .invokeAPI(
                        ENDPOINT_HARVESTER_ADD,
                        HttpMethod.POST,
                        Collections.EMPTY_MAP,
                        localVarQueryParams,
                        parsed.getXml(),
                        localVarHeaderParams,
                        localVarCookieParams,
                        localVarFormParams,
                        localVarAccept,
                        MediaType.APPLICATION_XML,
                        localVarAuthNames,
                        localReturnType
                );
    }
}
