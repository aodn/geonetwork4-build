package au.org.aodn.geonetwork_api.openapi.api;

import au.org.aodn.geonetwork_api.openapi.invoker.ApiClient;
import au.org.aodn.geonetwork_api.openapi.model.ThesaurusInfo;
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
import java.util.Locale;

public class RegistriesApiExt extends RegistriesApi {

    public RegistriesApiExt(ApiClient apiClient) {
        super(apiClient);
    }

    /**
     * The generated code in RegistersApi is wrong, mainly the MediaType should be text/xml, this function just
     * copy the generated one and changed the MediaType.
     *
     * @param url If set, try to download from the Internet. (optional)
     * @param registryUrl If set, try to download from a registry. (optional)
     * @param registryType If using registryUrl, then define the type of registry. If not set, default mode is re3gistry. (optional)
     * @param registryLanguage Languages to download from a registry. (optional)
     * @param type Local or external (default). (optional, default to external)
     * @param dir Type of thesaurus, usually one of the ISO thesaurus type codelist value. Default is theme. (optional, default to theme)
     * @param stylesheet XSL to be use to convert the thesaurus before load. Default _none_. (optional, default to _none_)
     * @param thesaurusInfo  (optional)
     * @return
     * @throws RestClientException
     */
    @Override
    public ResponseEntity<String> uploadThesaurusFromUrlWithHttpInfo(String url, String registryUrl, String registryType, List<String> registryLanguage, String type, String dir, String stylesheet, ThesaurusInfo thesaurusInfo) throws RestClientException {
        Object localVarPostBody = thesaurusInfo;

        final MultiValueMap<String, String> localVarQueryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders localVarHeaderParams = new HttpHeaders();
        final MultiValueMap<String, String> localVarCookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> localVarFormParams = new LinkedMultiValueMap<String, Object>();

        localVarQueryParams.putAll(getApiClient().parameterToMultiValueMap(null, "url", url));
        localVarQueryParams.putAll(getApiClient().parameterToMultiValueMap(null, "registryUrl", registryUrl));
        localVarQueryParams.putAll(getApiClient().parameterToMultiValueMap(null, "registryType", registryType));
        localVarQueryParams.putAll(getApiClient().parameterToMultiValueMap(ApiClient.CollectionFormat.valueOf("multi".toUpperCase(Locale.ROOT)), "registryLanguage", registryLanguage));
        localVarQueryParams.putAll(getApiClient().parameterToMultiValueMap(null, "type", type));
        localVarQueryParams.putAll(getApiClient().parameterToMultiValueMap(null, "dir", dir));
        localVarQueryParams.putAll(getApiClient().parameterToMultiValueMap(null, "stylesheet", stylesheet));

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<String> localReturnType = new ParameterizedTypeReference<>() {};
        return getApiClient().invokeAPI(
                "/registries/vocabularies",
                HttpMethod.PUT,
                Collections.<String, Object>emptyMap(),
                localVarQueryParams,
                localVarPostBody,
                localVarHeaderParams,
                localVarCookieParams,
                localVarFormParams,
                List.of(MediaType.TEXT_XML, MediaType.APPLICATION_JSON),
                MediaType.TEXT_XML,
                localVarAuthNames,
                localReturnType
        );
    }

}
