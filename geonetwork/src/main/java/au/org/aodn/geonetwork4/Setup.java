package au.org.aodn.geonetwork4;

import au.org.aodn.geonetwork_api.openapi.api.LogosApi;
import au.org.aodn.geonetwork_api.openapi.api.MeApi;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import java.util.Arrays;
import java.util.List;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * This class is used for post provision setup after the system starts
 */
public class Setup {

    protected Logger logger = LogManager.getLogger(Setup.class);

    protected MeApi meApi;
    protected LogosApi logosApi;

    protected ObjectMapper mapper = new ObjectMapper();

    public Setup(MeApi meApi, LogosApi logosApi) {
        this.meApi = meApi;
        this.logosApi = logosApi;
    }

    public void getMe() {
        logger.info("Login user is {}", meApi.getMeWithHttpInfo().getBody());
    }

    public void insertLogos(String... filenames) {

        // Extract the json content in the files
        List<Map<String, String>> json = Arrays.stream(filenames)
                .map(n -> {
                    ClassLoader cl = Thread.currentThread().getContextClassLoader(); // or whatever classloader you want to search from

                    try(InputStream stream = cl.getResourceAsStream(n)){
                        return mapper.readValue(stream,
                                new TypeReference<Map<String, String>>() {}
                        );
                    }
                    catch (IOException e) {
                        logger.error("Fail extract file content -> {}", n);
                        return null;
                    }
                })
                .filter(f -> f != null)
                .collect(Collectors.toList());

        List<File> fileList = json.stream()
                .map(j -> {
                    try (InputStream inputStream = new URL(j.get("link")).openStream()) {
                        File temp = new File(System.getProperty("java.io.tmpdir"), j.get("image"));
                        FileUtils.copyInputStreamToFile(inputStream, temp);

                        return temp;
                    }
                    catch(IOException e) {
                        return null;
                    }
                })
                .filter(f -> f != null)
                .collect(Collectors.toList());

        if(!fileList.isEmpty()) {
            logosApi.addLogoWithHttpInfo(fileList, true);
        }
    }
}
