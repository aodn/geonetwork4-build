package au.org.aodn.geonetwork4.model;

import java.util.List;

public interface RemoteConfig {
    List<String> readJson(List<RemoteConfigValue> values);
    List<RemoteConfigValue> getDefaultConfig();
}
