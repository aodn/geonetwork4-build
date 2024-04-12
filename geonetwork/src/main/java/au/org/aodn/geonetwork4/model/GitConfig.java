package au.org.aodn.geonetwork4.model;

public class GitConfig {
    protected ConfigTypes type;
    protected String jsonFileName;

    public ConfigTypes getType() { return type; }
    /**
     * We hardcode the path to github main geonetwork4-build so we always get the approved configuration after PR.
     */
    public String getUrl(String branch) {
        return String.format(
                "https://raw.githubusercontent.com/aodn/geonetwork4-build/%s/geonetwork-config/%s/%s",
                branch,
                type,
                jsonFileName);
    }
}
