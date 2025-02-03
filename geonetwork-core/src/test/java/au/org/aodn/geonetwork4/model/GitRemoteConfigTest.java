package au.org.aodn.geonetwork4.model;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ResourceUtils;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class GitRemoteConfigTest {

    @Test
    public void verifyGetUrl() {
        GitRemoteConfig gitRemoteConfig1 = new GitRemoteConfig(null, null, "main");

        RemoteConfigValue remoteConfigValue1 = new RemoteConfigValue();

        remoteConfigValue1.setType(ConfigTypes.groups);
        remoteConfigValue1.setJsonFileName("imos-{active_profile}.json");

        assertEquals("URL matched when no active profile",
                "https://raw.githubusercontent.com/aodn/geonetwork4-build/main/geonetwork-config/groups/imos-{active_profile}.json",
                gitRemoteConfig1.getUrl(remoteConfigValue1));


        GitRemoteConfig gitRemoteConfig2 = new GitRemoteConfig(null, "edge", "main");

        assertEquals("URL matched with active profile",
                "https://raw.githubusercontent.com/aodn/geonetwork4-build/main/geonetwork-config/groups/imos-edge.json",
                gitRemoteConfig2.getUrl(remoteConfigValue1));
    }

    @Test
    public void verifyReadJsonCorrect() {
        RestTemplate template = Mockito.mock(RestTemplate.class);
        GitRemoteConfig gitRemoteConfig = new GitRemoteConfig(template, "edge", "main");

        RemoteConfigValue remoteConfigValue1 = new RemoteConfigValue();
        remoteConfigValue1.setType(ConfigTypes.groups);
        remoteConfigValue1.setJsonFileName("imos-{active_profile}.json");

        RemoteConfigValue remoteConfigValue2 = new RemoteConfigValue();
        remoteConfigValue2.setType(ConfigTypes.groups);
        remoteConfigValue2.setJsonFileName("imos-abc.json");

        when(template.getForEntity(
                eq("https://raw.githubusercontent.com/aodn/geonetwork4-build/main/geonetwork-config/groups/imos-edge.json"),
                eq(String.class))
        )
                .thenReturn(ResponseEntity.ok("imos-edge.json"));

        when(template.getForEntity(
                eq("https://raw.githubusercontent.com/aodn/geonetwork4-build/main/geonetwork-config/groups/imos-abc.json"),
                eq(String.class))
        )
                .thenReturn(ResponseEntity.ok("imos-abc.json"));

        List<String> values = gitRemoteConfig.readJson(List.of(remoteConfigValue1, remoteConfigValue2));

        assertEquals("Two values", 2, values.size());
        assertTrue("Contains imos-abc.json", values.contains("imos-abc.json"));
        assertTrue("Contains imos-edge.json", values.contains("imos-edge.json"));
    }

    @Test
    public void verifyEnvironmentAwareConfigCorrect() throws IOException {
        File file = ResourceUtils.getFile("classpath:config.json");
        ObjectMapper mapper = new ObjectMapper();

        List<RemoteConfigValue> configs = mapper.readValue(file, new TypeReference<>() {});

        RestTemplate template = Mockito.mock(RestTemplate.class);
        GitRemoteConfig gitRemoteConfig = new GitRemoteConfig(template, "staging", "main");

        when(template.getForEntity(anyString(), eq(String.class)))
                .thenReturn(ResponseEntity.ok(null));

        gitRemoteConfig.readJson(configs);

        verify(template, times(1)).getForEntity("https://raw.githubusercontent.com/aodn/geonetwork4-build/main/geonetwork-config/logos/nesp_logo.json", String.class);
        verify(template, times(1)).getForEntity("https://raw.githubusercontent.com/aodn/geonetwork4-build/main/geonetwork-config/groups/aims.json", String.class);

        verify(template, times(1)).getForEntity("https://raw.githubusercontent.com/aodn/geonetwork4-build/main/geonetwork-config/categories/aad.json", String.class);
        verify(template, times(1)).getForEntity("https://raw.githubusercontent.com/aodn/geonetwork4-build/main/geonetwork-config/categories/z3950Servers.json", String.class);

        verify(template, times(1)).getForEntity("https://raw.githubusercontent.com/aodn/geonetwork4-build/main/geonetwork-config/settings/imos-staging.json", String.class);

        // Setting for edge only
        verify(template, times(0)).getForEntity("https://raw.githubusercontent.com/aodn/geonetwork4-build/main/geonetwork-config/harvesters/1_imos-craig-catalogue.json", String.class);
        verify(template, times(0)).getForEntity("https://raw.githubusercontent.com/aodn/geonetwork4-build/main/geonetwork-config/harvesters/2_aims_nrs.json", String.class);

        // Prod have same setting so we expect only called once
        verify(template, times(1)).getForEntity("https://raw.githubusercontent.com/aodn/geonetwork4-build/main/geonetwork-config/harvesters/catalog-aodn.json", String.class);
        verify(template, times(1)).getForEntity("https://raw.githubusercontent.com/aodn/geonetwork4-build/main/geonetwork-config/harvesters/catalog-imos.json", String.class);
    }
}
