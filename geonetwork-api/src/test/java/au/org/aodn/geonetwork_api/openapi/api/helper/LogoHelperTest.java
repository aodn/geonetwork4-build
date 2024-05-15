package au.org.aodn.geonetwork_api.openapi.api.helper;

import au.org.aodn.geonetwork_api.openapi.api.LogosApiExt;
import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ResourceUtils;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class LogoHelperTest {
    /**
     * The create logo function will try to delete logo before add, this test is used to verify even
     * the logo do not exist and not found exception throw from server, the function still works.
     */
    @Test
    public void verifyAddNewLogoWorks() throws IOException {
        LogosApiExt api = Mockito.mock(LogosApiExt.class);
        LogosHelper helper = new LogosHelper(api, new DefaultResourceLoader());

        when(api.deleteLogoWithHttpInfo(anyString()))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "entity not found"));

        when(api.addLogoWithHttpInfo(any(File.class), anyString(), eq(Boolean.TRUE)))
                .thenReturn(ResponseEntity.ok(null));

        String json1 = FileUtils.readFileToString(
                ResourceUtils.getFile("classpath:aad_logo.json"),
                StandardCharsets.UTF_8);

        // The logo gif do not exist in the test resources, so this is a negative test case
        String json2 = FileUtils.readFileToString(
                ResourceUtils.getFile("classpath:not_exist_logo.json"),
                StandardCharsets.UTF_8);

        helper.createLogos(List.of(json1, json2));

        // Only called once because if the logo file not exist then it won't call addLogo
        verify(api, times(1)).addLogoWithHttpInfo(any(File.class), anyString(), eq(Boolean.TRUE));
    }
}
