package au.org.aodn.geonetwork_api.openapi.api.helper;

import au.org.aodn.geonetwork_api.openapi.api.LogosApi;
import au.org.aodn.geonetwork_api.openapi.api.LogosApiExt;
import au.org.aodn.geonetwork_api.openapi.api.Parser;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.*;
import org.springframework.web.client.HttpServerErrorException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Enhance the LogosApi function by grouping similar logos functions together, it calls the LogosApi behind
 */
public class LogosHelper {

    protected Logger logger = LogManager.getLogger(LogosHelper.class);
    protected LogosApiExt api;

    public LogosHelper(LogosApiExt api) {
        this.api = api;
    }

    public LogosApi getApi() {
        return api;
    }

    public List<String> createLogos(List<String> config) {
        return config
                .stream()
                .map(v -> {
                    Parser.Parsed parsed = null;
                    try {
                        Parser parser = new Parser();
                        parsed = parser.parseLogosConfig(v);

                        // Read the link and download the file
                        URL url = new URL(parsed.getJsonObject().getString("link"));

                        try(InputStream is = url.openStream()) {
                            File file = File.createTempFile("img","img");
                            file.deleteOnExit();

                            FileUtils.copyInputStreamToFile(is, file);
                            ResponseEntity<String> response = api.addLogoWithHttpInfo(
                                    file,
                                    parsed.getJsonObject().getString("image"),
                                    Boolean.TRUE);

                            if(response.getStatusCode().is2xxSuccessful()) {
                                return response.getBody();
                            }
                            else {
                                return null;
                            }
                        }
                    }
                    catch(HttpServerErrorException.InternalServerError i){
                        return null;
                    }
                    catch (IOException e) {
                        return null;
                    }
                })
                .collect(Collectors.toList());
    }

}
