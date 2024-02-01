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
 * The reason we create this class because the generated class function is wrong, so do not use LogoApi directly
 *
 */
public class LogosApiExt extends LogosApi {

    public LogosApiExt(ApiClient apiClient) {
        super(apiClient);
    }

    /**
     * Implement the function again to fix two problems with the generated addLogoWithHttpInfo.
     * 1. It is not possible to upload with MultiValueMap with List<File> directly in the generated one.
     * 2. It used the wrong parameter to set the MultiValueMap, hence you will never get the upload right. The correct one is localVarFormParams
     * 3. The header missed content type.
     *
     * @param file - A local file
     * @param filename
     * @param overwrite
     * @return
     * @throws RestClientException
     * @throws IOException
     */
    public ResponseEntity<String> addLogoWithHttpInfo(File file, String filename, Boolean overwrite) throws RestClientException, IOException {

        if (file == null || filename == null) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Missing the required parameter 'file' or 'filename' when calling addLogo");
        }
        else {

            HttpHeaders localVarHeaderParams = new HttpHeaders();
            localVarHeaderParams.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, String> localVarCookieParams = new LinkedMultiValueMap();
            MultiValueMap<String, Object> localVarFormParams = new LinkedMultiValueMap();

            // Must wrap it inside this object so the multipart works
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

            // Check if exist, if yes, then we cannot update it.
            try {
                ResponseEntity<Void> logo = this.getLogoWithHttpInfo(filename);
                return ResponseEntity.badRequest().body("Logo name exist " + filename);
            }
            catch(HttpClientErrorException.NotFound notFound) {
                // If not found we can add
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
}
