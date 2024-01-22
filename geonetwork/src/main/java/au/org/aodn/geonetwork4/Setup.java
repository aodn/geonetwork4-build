package au.org.aodn.geonetwork4;

import au.org.aodn.geonetwork_api.openapi.api.LogosApi;
import au.org.aodn.geonetwork_api.openapi.invoker.ApiClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.FileNotFoundException;
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

    public void injectLogos(String... filename) {
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
