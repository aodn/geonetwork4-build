package au.org.aodn.geonetwork4.model;

import java.util.List;

public interface RemoteConfig {
    List<String> readJson(List<RemoteConfigValue> values);
    List<RemoteConfigValue> getDefaultConfig();
    /**
     * Same config read from another git ref (tag, branch or commit), default ignores it
     */
    default RemoteConfig withRef(String ref) { return this; }
    /**
     * Whether the config exists at this ref
     */
    default boolean exists() { return true; }
}
