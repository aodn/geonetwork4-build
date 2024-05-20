package au.org.aodn.geonetwork_api.openapi.api.helper;

import au.org.aodn.geonetwork_api.openapi.api.SiteApi;
import au.org.aodn.geonetwork_api.openapi.model.Setting;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class SiteHelperTest {

    /**
     * Function test only, we do not have docker image to run for this test, the
     * integration test in geonetwork should test it more.
     */
    @Test
    public void verifyGetAllSettingsDetails() {
        Setting s1 = new Setting();
        s1.setName(SiteHelper.HOST);
        s1.setValue("geonetwork-edge.edge.aodn.org.au");

        Setting s2 = new Setting();
        s2.setName(SiteHelper.PROTOCOL);
        s2.setValue("https");

        List<Setting> result = new ArrayList<>();
        result.add(s1);
        result.add(s2);

        SiteApi api = Mockito.mock(SiteApi.class);
        when(api.getSettingsDetailsWithHttpInfo(eq(null), eq(null)))
                .thenReturn(ResponseEntity.ok(result));

        SiteHelper helper = new SiteHelper(api);

        Set<String> v = helper.getAllSettingsDetails().keySet();
        assertTrue("Contains host", v.contains(SiteHelper.HOST));
        assertTrue("Contains protocol", v.contains(SiteHelper.PROTOCOL));

        assertEquals("Host equals",
                "geonetwork-edge.edge.aodn.org.au",
                helper.getAllSettingsDetails().get(SiteHelper.HOST).getValue());
    }
}
