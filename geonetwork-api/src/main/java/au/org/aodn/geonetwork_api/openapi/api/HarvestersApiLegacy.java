package au.org.aodn.geonetwork_api.openapi.api;

import au.org.aodn.geonetwork_api.openapi.invoker.ApiClient;
import au.org.aodn.geonetwork_api.openapi.model.HarvestersApiLegacyResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
import java.util.stream.Collectors;

/**
 * The current api provided limited support on harvester operation, hence we implement the missing. We can demise
 * the code here in the future when api get mature.
 */
public class HarvestersApiLegacy extends HarvestersApi {

    protected final static String ENDPOINT_HARVESTER_ADD = "/admin.harvester.add";
    protected Logger logger = LogManager.getLogger(HarvestersApiLegacy.class);

    public HarvestersApiLegacy(ApiClient client) {
        super(client);
        client.setBasePath("http://localhost:8080/geonetwork/srv/eng");
    }

    // We use aop proxy, so we cannot use "this" for any function call but call the bean
    @Autowired
    protected HarvestersApiLegacy apiLegacy;

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
                        parsed = new Parser().convertHarvestersJsonToXml(v);

                        logger.info("Adding harvestors config {}", parsed.getXml());

                        ResponseEntity<Map<String, Object>> r = apiLegacy.createHarvestersWithHttpInfo(parsed);

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


        return apiLegacy.getApiClient()
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
