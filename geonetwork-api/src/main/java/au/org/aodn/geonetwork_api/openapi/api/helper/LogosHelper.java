package au.org.aodn.geonetwork_api.openapi.api.helper;

import au.org.aodn.geonetwork_api.openapi.api.LogosApiExt;
import au.org.aodn.geonetwork_api.openapi.api.Parser;
import au.org.aodn.geonetwork_api.openapi.api.Status;
import au.org.aodn.geonetwork_api.openapi.model.Group;
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
import java.util.Collections;
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
    // Optional: when wired in, allows a logo to be replaced even while groups reference it.
    protected GroupsHelper groupsHelper;

    public LogosHelper(LogosApiExt api, ResourceLoader resourceLoader) {
        this(api, resourceLoader, null);
    }

    public LogosHelper(LogosApiExt api, ResourceLoader resourceLoader, GroupsHelper groupsHelper) {
        this.api = api;
        this.resourceLoader = resourceLoader;
        this.groupsHelper = groupsHelper;
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

                            String image = parsed.getJsonObject().getString(IMAGE);

                            // The server refuses to delete a logo that is still referenced by a group,
                            // and its addLogo cannot overwrite an existing file. So a replace of an
                            // in-use logo silently fails. Temporarily detach the logo from those
                            // groups, replace the file, then re-attach the same filename to the new
                            // image. (Source/harvester references do not block the delete.)
                            List<Group> groupsUsingLogo = detachLogoFromGroups(image);
                            try {
                                // Delete before add (now unblocked). A genuine "not found" is fine.
                                try {
                                    getApi().deleteLogoWithHttpInfo(image);
                                }
                                catch(Exception e) {
                                    logger.warn("Ignore delete error for logo {} (likely not present yet): {}",
                                            image, e.getMessage());
                                }

                                ResponseEntity<String> response = getApi().addLogoWithHttpInfo(
                                        file,
                                        image,
                                        Boolean.TRUE);

                                status.setStatus(response.getStatusCode());

                                if (response.getStatusCode().is2xxSuccessful()) {
                                    status.setMessage(response.getBody());
                                }
                            }
                            finally {
                                // Always re-attach, even if the replace failed, so groups never end
                                // up logo-less.
                                reattachLogoToGroups(groupsUsingLogo, image);
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

    /**
     * Detach the given logo filename from every group that references it, so the logo can be
     * deleted and replaced. Returns the groups that were detached so they can be re-attached after
     * the replacement. No-op (empty list) when group handling is not wired in.
     *
     * @param image - The logo filename
     * @return the groups the logo was detached from
     */
    protected List<Group> detachLogoFromGroups(String image) {
        if (groupsHelper == null) {
            return Collections.emptyList();
        }
        List<Group> groups = groupsHelper.findGroupsByLogo(image);
        groups.forEach(g -> {
            logger.info("Temporarily detaching logo {} from group {}", image, g.getName());
            groupsHelper.setGroupLogo(g, null);
        });
        return groups;
    }

    /**
     * Re-attach the (now replaced) logo filename to the groups it was detached from.
     *
     * @param groups - The groups to re-attach the logo to
     * @param image  - The logo filename
     */
    protected void reattachLogoToGroups(List<Group> groups, String image) {
        if (groupsHelper == null) {
            return;
        }
        groups.forEach(g -> {
            logger.info("Re-attaching logo {} to group {}", image, g.getName());
            groupsHelper.setGroupLogo(g, image);
        });
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
