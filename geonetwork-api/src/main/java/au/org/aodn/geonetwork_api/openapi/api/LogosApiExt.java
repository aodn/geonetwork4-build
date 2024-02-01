package au.org.aodn.geonetwork_api.openapi.api;

import au.org.aodn.geonetwork_api.openapi.invoker.ApiClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;

import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;

/**
 * One function generated incoorectly
 */
public class LogosApiExt extends LogosApi {

    public LogosApiExt(ApiClient apiClient) {
        super(apiClient);
    }

    public ResponseEntity<String> addLogoWithHttpInfo(File file, String filename, Boolean overwrite) throws RestClientException, IOException {

        if (file == null) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Missing the required parameter '_file' when calling addLogo");
        }
        else {

            HttpHeaders localVarHeaderParams = new HttpHeaders();
            localVarHeaderParams.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, String> localVarCookieParams = new LinkedMultiValueMap();
            MultiValueMap<String, Object> localVarFormParams = new LinkedMultiValueMap();

            ByteArrayResource fileAsResource = new ByteArrayResource(Files.readAllBytes(file.toPath())) {
                @Override
                public String getFilename(){
                    return filename;
                }
            };

            localVarFormParams.add("file", fileAsResource);
            localVarFormParams.add("overwrite", overwrite);

            String[] localVarAccepts = new String[]{"application/json"};
            List<MediaType> localVarAccept = this.getApiClient().selectHeaderAccept(localVarAccepts);

            String[] localVarAuthNames = new String[0];
            ParameterizedTypeReference<String> localReturnType = new ParameterizedTypeReference<>() {};

            return this.getApiClient()
                    .invokeAPI(
                            "/logos",
                            HttpMethod.POST,
                            Collections.emptyMap(),
                            null,
                            null,
                            localVarHeaderParams,
                            localVarCookieParams,
                            localVarFormParams,
                            localVarAccept,
                            MediaType.MULTIPART_FORM_DATA,
                            localVarAuthNames,
                            localReturnType
                    );
        }
    }
}
