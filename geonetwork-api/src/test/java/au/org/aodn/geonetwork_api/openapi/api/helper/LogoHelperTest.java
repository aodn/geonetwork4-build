package au.org.aodn.geonetwork_api.openapi.api.helper;

import au.org.aodn.geonetwork_api.openapi.api.LogosApiExt;
import au.org.aodn.geonetwork_api.openapi.model.Group;
import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ResourceUtils;
import org.springframework.web.client.RestClientException;
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

    /**
     * When a logo is referenced by a group, the server refuses to delete it and cannot overwrite it.
     * Verify createLogos detaches the logo from the group, replaces the file, then re-attaches the
     * same filename to the new image - in that order.
     */
    @Test
    public void verifyReplaceLogoUsedByGroupWorks() throws IOException {
        LogosApiExt api = Mockito.mock(LogosApiExt.class);
        GroupsHelper groupsHelper = Mockito.mock(GroupsHelper.class);
        LogosHelper helper = new LogosHelper(api, new DefaultResourceLoader(), groupsHelper);

        Group group = new Group();
        group.setId(123);
        group.setName("AIMS");
        group.setLogo("AAD_logo.gif");

        when(groupsHelper.findGroupsByLogo(anyString())).thenReturn(List.of(group));
        when(api.addLogoWithHttpInfo(any(File.class), anyString(), eq(Boolean.TRUE)))
                .thenReturn(ResponseEntity.ok("uploaded"));

        String json = FileUtils.readFileToString(
                ResourceUtils.getFile("classpath:aad_logo.json"),
                StandardCharsets.UTF_8);

        helper.createLogos(List.of(json));

        InOrder inOrder = inOrder(groupsHelper, api);
        // 1. detach (logo set to null)
        inOrder.verify(groupsHelper).setGroupLogo(eq(group), isNull());
        // 2. delete old file
        inOrder.verify(api).deleteLogoWithHttpInfo("AAD_logo.gif");
        // 3. add new bytes under same filename
        inOrder.verify(api).addLogoWithHttpInfo(any(File.class), eq("AAD_logo.gif"), eq(Boolean.TRUE));
        // 4. re-attach same filename to the new image
        inOrder.verify(groupsHelper).setGroupLogo(eq(group), eq("AAD_logo.gif"));
    }

    /**
     * Even if the add (replace) fails, the logo must be re-attached to the group so the group is
     * never left without a logo.
     */
    @Test
    public void verifyGroupReattachedEvenWhenAddFails() throws IOException {
        LogosApiExt api = Mockito.mock(LogosApiExt.class);
        GroupsHelper groupsHelper = Mockito.mock(GroupsHelper.class);
        LogosHelper helper = new LogosHelper(api, new DefaultResourceLoader(), groupsHelper);

        Group group = new Group();
        group.setId(123);
        group.setName("AIMS");
        group.setLogo("AAD_logo.gif");

        when(groupsHelper.findGroupsByLogo(anyString())).thenReturn(List.of(group));
        when(api.addLogoWithHttpInfo(any(File.class), anyString(), eq(Boolean.TRUE)))
                .thenThrow(new RestClientException("upload failed"));

        String json = FileUtils.readFileToString(
                ResourceUtils.getFile("classpath:aad_logo.json"),
                StandardCharsets.UTF_8);

        helper.createLogos(List.of(json));

        // detach then re-attach must both happen despite the failed add
        verify(groupsHelper).setGroupLogo(eq(group), isNull());
        verify(groupsHelper).setGroupLogo(eq(group), eq("AAD_logo.gif"));
    }
}
