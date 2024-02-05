package au.org.aodn.geonetwork_api.openapi.api.helper;

import au.org.aodn.geonetwork_api.openapi.api.LogosApi;
import au.org.aodn.geonetwork_api.openapi.api.LogosApiExt;
import au.org.aodn.geonetwork_api.openapi.api.Parser;
import au.org.aodn.geonetwork_api.openapi.api.Status;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Enhance the LogosApi function by grouping similar logos functions together, it calls the LogosApi behind
 */
public class LogosHelper {

    protected static final String IMAGE = "image";
    protected Logger logger = LogManager.getLogger(LogosHelper.class);
    protected LogosApiExt api;

    public LogosHelper(LogosApiExt api) {
        this.api = api;
    }

    public LogosApiExt getApi() {
        return api;
    }

    public List<Status> createLogos(List<String> config) {

        return config
                .stream()
                .map(v -> {
                    Parser.Parsed parsed = null;
                    Status status = new Status();
                    status.setFileContent(v);

                    try {
                        Parser parser = new Parser();
                        parsed = parser.parseLogosConfig(v);


                        // Read the link and download the file
                        URL url = new URL(parsed.getJsonObject().getString("link"));

                        try (InputStream is = url.openStream()) {
                            File file = File.createTempFile("img", "img");
                            file.deleteOnExit();

                            FileUtils.copyInputStreamToFile(is, file);
                            ResponseEntity<String> response = getApi().addLogoWithHttpInfo(
                                    file,
                                    parsed.getJsonObject().getString(IMAGE),
                                    Boolean.TRUE);

                            status.setStatus(response.getStatusCode());

                            if (response.getStatusCode().is2xxSuccessful()) {
                                status.setMessage(response.getBody());
                            }
                        }
                    }
                    catch(HttpServerErrorException.InternalServerError | HttpClientErrorException.BadRequest i){
                        status.setStatus(i.getStatusCode());
                        status.setMessage(i.getMessage());
                    }
                    finally {
                        return status;
                    }
                })
                .collect(Collectors.toList());
    }

}
