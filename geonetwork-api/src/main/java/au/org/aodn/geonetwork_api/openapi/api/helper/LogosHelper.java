package au.org.aodn.geonetwork_api.openapi.api.helper;

import au.org.aodn.geonetwork_api.openapi.api.LogosApiExt;
import au.org.aodn.geonetwork_api.openapi.api.Parser;
import au.org.aodn.geonetwork_api.openapi.api.Status;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Enhance the LogosApi function by grouping similar logos functions together, it calls the LogosApi behind
 */
public class LogosHelper {

    protected static final String IMAGE = "image";
    protected Logger logger = LogManager.getLogger(LogosHelper.class);
    protected ResourceLoader resourceLoader;
    protected LogosApiExt api;

    public LogosHelper(LogosApiExt api, ResourceLoader resourceLoader) {
        this.api = api;
        this.resourceLoader = resourceLoader;
    }

    public LogosApiExt getApi() {
        return api;
    }
    /**
     * Based on the incoming config, use the link to download the gif and upload it to geonetwork4 via api call
     *
     * @param config - The json config string
     * @return The upload status
     */
    public List<Status> createLogos(List<String> config) {

        final Parser parser = new Parser();
        return config
                .stream()
                .map(v -> {
                    Parser.Parsed parsed;
                    Status status = new Status();
                    status.setFileContent(v);

                    parsed = parser.parseLogosConfig(v);
                    try {
                        logger.info("Processing logo config -> {}", v);
                        // Read the link and download the file
                        Resource resource = resourceLoader.getResource(parsed.getJsonObject().getString("link"));

                        try (InputStream is = resource.getInputStream()) {
                            // Store in temp folder
                            File file = File.createTempFile("img", "img");
                            file.deleteOnExit();

                            FileUtils.copyInputStreamToFile(is, file);

                            // Delete before add
                            try {
                                getApi().deleteLogoWithHttpInfo(parsed.getJsonObject().getString(IMAGE));
                            }
                            catch(Exception e) {
                                logger.warn("Ignore error because delete file do not exist");
                            }

                            ResponseEntity<String> response = getApi().addLogoWithHttpInfo(
                                    file,
                                    parsed.getJsonObject().getString(IMAGE),
                                    Boolean.TRUE);

                            status.setStatus(response.getStatusCode());

                            if (response.getStatusCode().is2xxSuccessful()) {
                                status.setMessage(response.getBody());
                            }
                        }
                        catch (IOException e) {
                            status.setStatus(HttpStatus.BAD_REQUEST);
                            status.setMessage("Cannot open stream to download file : " +  parsed.getJsonObject().getString("link"));
                            logger.error(status.getMessage());
                        }
                        return status;
                    }
                    catch(HttpServerErrorException.InternalServerError | HttpClientErrorException.BadRequest i) {
                        status.setStatus(i.getStatusCode());
                        status.setMessage("File already exist in folder? " + i.getMessage());
                        logger.error(status.getMessage());
                        return status;
                    }
                    catch(RestClientException restClientException) {
                        // This error indicate file already exist so it is fine
                        logger.info("Ignore error {} for {}", restClientException.getMessage(), v);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public void deleteAllLogos() {
        ResponseEntity<Set<String>> logos = api.getLogosWithHttpInfo();
        if(logos.getStatusCode().is2xxSuccessful()) {
            Objects.requireNonNull(logos.getBody())
                    .forEach(l -> {
                        if(!l.isEmpty()) {
                            logger.info("Delete logo {}", l);
                            api.deleteLogoWithHttpInfo(l);
                        }
                    });
        }
    }

}
