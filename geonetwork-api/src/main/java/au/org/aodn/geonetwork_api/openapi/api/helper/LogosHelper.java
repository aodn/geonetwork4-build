package au.org.aodn.geonetwork_api.openapi.api.helper;

import au.org.aodn.geonetwork_api.openapi.api.LogosApiExt;
import au.org.aodn.geonetwork_api.openapi.api.Parser;
import au.org.aodn.geonetwork_api.openapi.api.Status;
import jeeves.server.context.ServiceContext;
import jeeves.server.dispatchers.ServiceManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.fao.geonet.ApplicationContextHolder;
import org.fao.geonet.resources.Resources;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Enhance the LogosApi function by grouping similar logos functions together, it calls the LogosApi behind
 */
public class LogosHelper {

    protected static final String IMAGE = "image";
    protected static final String LINK = "link";
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
     * Based on the incoming config, use the link to download the image and store it as a logo.
     * <p>
     * The image is written directly through GeoNetwork's internal {@link Resources} store rather
     * than the REST API. The REST API cannot replace an in-use logo in one step: its {@code delete}
     * is refused while a group still references the logo (see {@code LogosApi.deleteLogo} ->
     * {@code GroupRepository.findByLogo}), and its {@code add} cannot overwrite an existing file
     * (it copies without {@code REPLACE_EXISTING}). Going through the internal store overwrites the
     * file in place regardless of any group reference, so no group detach/re-attach dance is needed.
     *
     * @param config - The json config string
     * @return The upload status
     */
    public List<Status> createLogos(List<String> config) {

        final Parser parser = new Parser();
        return config
                .stream()
                .map(v -> {
                    Status status = new Status();
                    status.setFileContent(v);

                    Parser.Parsed parsed = parser.parseLogosConfig(v);
                    String link = parsed.getJsonObject().getString(LINK);
                    String image = parsed.getJsonObject().getString(IMAGE);

                    logger.info("Processing logo config -> {}", v);

                    // Read the link and stream the image straight into the logo store.
                    Resource resource = resourceLoader.getResource(link);
                    try (InputStream is = resource.getInputStream()) {
                        writeLogo(is, image);
                        status.setStatus(HttpStatus.CREATED);
                        status.setMessage("Logo " + image + " written");
                    }
                    catch (IOException e) {
                        status.setStatus(HttpStatus.BAD_REQUEST);
                        status.setMessage("Cannot write logo " + image + " from : " + link);
                        logger.error(status.getMessage(), e);
                    }
                    return status;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Write (or overwrite) a logo file in GeoNetwork's harvester logos directory using the internal
     * {@link Resources} store, so the bytes are persisted correctly whatever the backing store is
     * (filesystem, S3, CMIS, ...).
     *
     * @param is    - The image bytes to write
     * @param image - The logo filename (e.g. "AIMS_logo.gif")
     */
    protected void writeLogo(InputStream is, String image) throws IOException {
        ConfigurableApplicationContext appContext = ApplicationContextHolder.get();
        Resources resources = appContext.getBean(Resources.class);

        // setup runs on a plain Spring MVC thread where no Jeeves ServiceContext is set, so build
        // one. The request-free variant wires in the servlet, which the filesystem store needs to
        // resolve its base path.
        ServiceManager serviceManager = appContext.getBean(ServiceManager.class);
        ServiceContext context = serviceManager.createServiceContext("aodn-logo-setup", appContext);

        Path logosDir = resources.locateHarvesterLogosDirSMVC(appContext);
        try (Resources.ResourceHolder holder = resources.getWritableImage(context, image, logosDir)) {
            Files.copy(is, holder.getPath(), StandardCopyOption.REPLACE_EXISTING);
        }
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
