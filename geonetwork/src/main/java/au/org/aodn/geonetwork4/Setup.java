package au.org.aodn.geonetwork4;

import au.org.aodn.geonetwork_api.openapi.api.LogosApi;
import au.org.aodn.geonetwork_api.openapi.invoker.ApiClient;
import au.org.aodn.geonetwork_api.openapi.invoker.ApiException;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This class is used for post provision setup after the system starts
 */

public class Setup {

    protected Logger logger = LogManager.getLogger(Setup.class);

    protected LogosApi logosApi;

    public Setup(ApiClient client) {
        this.logosApi = new LogosApi(client);
    }

    public void injectLogos(String... filename) throws ApiException {
        List<File> fileList = Arrays.stream(filename)
                .map(n -> {
                    try {
                        return ResourceUtils.getFile(n);
                    }
                    catch (FileNotFoundException e) {
                        logger.error("Fail to inject logo with json file -> {}", n);
                        return null;
                    }
                })
                .filter(f -> f != null)
                .collect(Collectors.toList());

        logosApi.addLogo(fileList, true);
    }
}
