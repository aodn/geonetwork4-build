package au.org.aodn.geonetwork_api.openapi.api.helper;

import au.org.aodn.geonetwork_api.openapi.api.LogosApiExt;
import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.util.ResourceUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class LogoHelperTest {

    /**
     * createLogos writes the logo through the internal store. We override the writeLogo seam so the
     * test does not need a running GeoNetwork context, then assert the right filename and bytes are
     * written. The internal store overwrites in place, so an in-use (group-referenced) logo needs no
     * special handling here.
     */
    @Test
    public void verifyAddNewLogoWritesFile() throws IOException {
        LogosApiExt api = Mockito.mock(LogosApiExt.class);

        List<String> writtenImages = new ArrayList<>();
        Path tempDir = Files.createTempDirectory("logo-test");
        tempDir.toFile().deleteOnExit();

        LogosHelper helper = new LogosHelper(api, new DefaultResourceLoader()) {
            @Override
            protected void writeLogo(InputStream is, String image) throws IOException {
                writtenImages.add(image);
                Files.copy(is, tempDir.resolve(image));
            }
        };

        String json = FileUtils.readFileToString(
                ResourceUtils.getFile("classpath:aad_logo.json"),
                StandardCharsets.UTF_8);

        helper.createLogos(List.of(json));

        // The configured filename is written, with the bytes of the linked source image.
        assertEquals(List.of("AAD_logo.gif"), writtenImages);
        assertTrue(Files.exists(tempDir.resolve("AAD_logo.gif")));
        assertArrayEquals(
                FileUtils.readFileToByteArray(ResourceUtils.getFile("classpath:AAD_logo.png")),
                Files.readAllBytes(tempDir.resolve("AAD_logo.gif")));
    }

    /**
     * When the linked source cannot be opened (the gif does not exist), the logo is not written and
     * the status reports a BAD_REQUEST instead of silently succeeding.
     */
    @Test
    public void verifyMissingSourceDoesNotWrite() throws IOException {
        LogosApiExt api = Mockito.mock(LogosApiExt.class);

        List<String> writtenImages = new ArrayList<>();
        LogosHelper helper = new LogosHelper(api, new DefaultResourceLoader()) {
            @Override
            protected void writeLogo(InputStream is, String image) {
                writtenImages.add(image);
            }
        };

        String json = FileUtils.readFileToString(
                ResourceUtils.getFile("classpath:not_exist_logo.json"),
                StandardCharsets.UTF_8);

        var statuses = helper.createLogos(List.of(json));

        assertTrue("Nothing should be written when the source is missing", writtenImages.isEmpty());
        assertEquals(1, statuses.size());
        assertEquals(org.springframework.http.HttpStatus.BAD_REQUEST, statuses.get(0).getStatus());
    }
}
