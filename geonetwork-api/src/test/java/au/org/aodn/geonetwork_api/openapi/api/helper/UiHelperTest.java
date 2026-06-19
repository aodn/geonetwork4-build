package au.org.aodn.geonetwork_api.openapi.api.helper;

import au.org.aodn.geonetwork_api.openapi.api.Status;
import au.org.aodn.geonetwork_api.openapi.api.UiApi;
import au.org.aodn.geonetwork_api.openapi.model.UiSetting;
import org.json.JSONObject;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

public class UiHelperTest {

    @Test
    public void verifyDeepMergeKeepsSiblings() {
        UiApi api = Mockito.mock(UiApi.class);

        UiSetting live = new UiSetting();
        live.setConfiguration("{\"mods\":{\"editor\":{\"fluidEditorLayout\":true}}}");
        when(api.getUiConfigurationWithHttpInfo(eq(UiHelper.UI_IDENTIFIER)))
                .thenReturn(ResponseEntity.ok(live));

        UiHelper helper = new UiHelper(api);

        String fragment = "{\"mods\":{\"editor\":{\"facetConfig\":{\"cat\":\"cat.keyword\"}}}}";
        List<Status> result = helper.updateUiConfig(List.of(fragment));

        assertEquals("One status per fragment", 1, result.size());
        assertEquals(HttpStatus.OK, result.get(0).getStatus());

        ArgumentCaptor<UiSetting> captor = ArgumentCaptor.forClass(UiSetting.class);
        Mockito.verify(api).updateUiConfigurationWithHttpInfo(eq(UiHelper.UI_IDENTIFIER), captor.capture());

        JSONObject merged = new JSONObject(captor.getValue().getConfiguration())
                .getJSONObject("mods").getJSONObject("editor");
        assertTrue("Sibling preserved", merged.getBoolean("fluidEditorLayout"));
        assertEquals("Fragment leaf written",
                "cat.keyword", merged.getJSONObject("facetConfig").getString("cat"));
    }

    @Test
    public void verifyNullConfigurationStartsEmpty() {
        UiApi api = Mockito.mock(UiApi.class);

        UiSetting live = new UiSetting();
        live.setConfiguration(null);
        when(api.getUiConfigurationWithHttpInfo(eq(UiHelper.UI_IDENTIFIER)))
                .thenReturn(ResponseEntity.ok(live));

        UiHelper helper = new UiHelper(api);

        List<Status> result = helper.updateUiConfig(List.of("{\"a\":{\"b\":1}}"));

        assertEquals(HttpStatus.OK, result.get(0).getStatus());

        ArgumentCaptor<UiSetting> captor = ArgumentCaptor.forClass(UiSetting.class);
        Mockito.verify(api).updateUiConfigurationWithHttpInfo(eq(UiHelper.UI_IDENTIFIER), captor.capture());
        assertEquals(1, new JSONObject(captor.getValue().getConfiguration())
                .getJSONObject("a").getInt("b"));
    }


    @Test
    public void verifyMissingConfigIsCreated() {
        UiApi api = Mockito.mock(UiApi.class);

        // Fresh catalogue: no 'srv' record yet -> GET returns 404
        when(api.getUiConfigurationWithHttpInfo(eq(UiHelper.UI_IDENTIFIER)))
                .thenThrow(HttpClientErrorException.create(
                        HttpStatus.NOT_FOUND, "Not Found",
                        new HttpHeaders(), new byte[0], StandardCharsets.UTF_8));

        UiHelper helper = new UiHelper(api);

        String fragment = "{\"mods\":{\"editor\":{\"facetConfig\":{\"cat\":\"cat.keyword\"}}}}";
        List<Status> result = helper.updateUiConfig(List.of(fragment));

        assertEquals(HttpStatus.OK, result.get(0).getStatus());

        // It must create (PUT /ui) rather than update (PUT /ui/{id})
        ArgumentCaptor<UiSetting> captor = ArgumentCaptor.forClass(UiSetting.class);
        Mockito.verify(api).putUiConfigurationWithHttpInfo(captor.capture());
        Mockito.verify(api, never()).updateUiConfigurationWithHttpInfo(Mockito.anyString(), Mockito.any());

        UiSetting created = captor.getValue();
        assertEquals(UiHelper.UI_IDENTIFIER, created.getId());
        assertEquals("cat.keyword", new JSONObject(created.getConfiguration())
                .getJSONObject("mods").getJSONObject("editor")
                .getJSONObject("facetConfig").getString("cat"));
    }

    @Test
    public void verifyApiFailureReportedAsError() {
        UiApi api = Mockito.mock(UiApi.class);
        when(api.getUiConfigurationWithHttpInfo(eq(UiHelper.UI_IDENTIFIER)))
                .thenThrow(new RuntimeException("boom"));

        UiHelper helper = new UiHelper(api);

        List<Status> result = helper.updateUiConfig(List.of("{\"a\":1}"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result.get(0).getStatus());
    }
}
