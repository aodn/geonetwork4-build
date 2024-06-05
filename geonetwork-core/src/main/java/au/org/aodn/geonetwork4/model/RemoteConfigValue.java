package au.org.aodn.geonetwork4.model;


import com.fasterxml.jackson.annotation.JsonProperty;

public class RemoteConfigValue {

    protected ConfigTypes type;
    protected String jsonFileName;

    @JsonProperty("type")
    public void setType(ConfigTypes type) {
        this.type = type;
    }

    @JsonProperty("jsonFileName")
    public void setJsonFileName(String jsonFileName) {
        this.jsonFileName = jsonFileName;
    }

    public ConfigTypes getType() { return type; }

    public String getJsonFileName() { return jsonFileName; }
}
