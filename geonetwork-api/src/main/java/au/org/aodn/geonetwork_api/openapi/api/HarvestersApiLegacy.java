package au.org.aodn.geonetwork_api.openapi.api;

import au.org.aodn.geonetwork_api.openapi.invoker.ApiClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;

import java.util.Collections;
import java.util.List;

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

    public ResponseEntity<String> createHarvesterWithHttpInfo(String config) {

        HttpHeaders localVarHeaderParams = new HttpHeaders();

        MultiValueMap<String, String> localVarQueryParams = new LinkedMultiValueMap();
        MultiValueMap<String, String> localVarCookieParams = new LinkedMultiValueMap();
        MultiValueMap<String, Object> localVarFormParams = new LinkedMultiValueMap();

        String[] localVarAuthNames = new String[0];
        ParameterizedTypeReference<String> localReturnType = new ParameterizedTypeReference<String>() {};

        String[] localVarAccepts = new String[]{"*/*", "application/json"};
        List<MediaType> localVarAccept = this.getApiClient().selectHeaderAccept(localVarAccepts);

        return this.getApiClient().invokeAPI(
                ENDPOINT_HARVESTER_ADD,
                HttpMethod.POST,
                Collections.EMPTY_MAP,
                localVarQueryParams,
                config,
                localVarHeaderParams,
                localVarCookieParams,
                localVarFormParams,
                localVarAccept,
                MediaType.APPLICATION_JSON,
                localVarAuthNames,
                localReturnType
        );
    }
}
