package au.org.aodn.geonetwork4.model;

import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

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
}
