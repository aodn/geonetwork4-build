package au.org.aodn.geonetwork4.model;


import au.org.aodn.geonetwork4.enumeration.Environment;
import com.fasterxml.jackson.annotation.JsonProperty;

public class RemoteConfigValue {

    protected ConfigTypes type;
    protected Environment environment = Environment.any;
    protected String jsonFileName;

    @JsonProperty("type")
    public void setType(ConfigTypes type) {
        this.type = type;
    }

    @JsonProperty("jsonFileName")
    public void setJsonFileName(String jsonFileName) {
        this.jsonFileName = jsonFileName;
    }

    @JsonProperty("env")
    public void setEnvironment(Environment environment) { this.environment = environment; }

    public ConfigTypes getType() { return this.type; }

    public String getJsonFileName() { return this.jsonFileName; }

    public Environment getEnvironment() { return this.environment; }
}
