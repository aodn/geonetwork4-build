package au.org.aodn.geonetwork_api.openapi.api.helper;

import au.org.aodn.geonetwork_api.openapi.api.RegistriesApi;
import au.org.aodn.geonetwork_api.openapi.api.Status;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

import java.util.List;
import java.util.stream.Collectors;

public class VocabulariesHelper {

    protected RegistriesApi api;
    protected Logger logger = LogManager.getLogger(VocabulariesHelper.class);

    public VocabulariesHelper(RegistriesApi api) {
        this.api = api;
    }

    public List<Status> createVocabularies(List<String> json) {
        return json.stream()
                .map(m -> {
                    JSONObject jsonObject = new JSONObject(m);

                    Status status = new Status();
                    status.setFileContent(m);

                    ResponseEntity<String> response = null;
                    try {
                        logger.info("Processing category - {}", jsonObject.getString("title"));
                        response = api.uploadThesaurusFromUrlWithHttpInfo(
                                jsonObject.getString("url"),
                                null,
                                null,
                                null,
                                jsonObject.getString("type"),
                                jsonObject.getString("dname"),
                                null,
                                null
                        );
                    }
                    catch(HttpClientErrorException.BadRequest badRequest) {
                        status.setStatus(HttpStatus.BAD_REQUEST);
                        status.setMessage(badRequest.getMessage());
                    }
                    finally {
                        logger.info("Processed category - {}", jsonObject.getString("title"));
                        if(response != null) {
                            status.setStatus(response.getStatusCode());
                        }
                        return status;
                    }

                })
                .collect(Collectors.toList());
    }
}
