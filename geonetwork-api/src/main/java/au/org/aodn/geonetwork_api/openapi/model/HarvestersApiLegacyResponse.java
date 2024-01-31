package au.org.aodn.geonetwork_api.openapi.model;

import org.springframework.http.HttpStatus;

public class HarvestersApiLegacyResponse {

    protected HttpStatus status;
    protected String id;
    protected String error;
    protected String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public void setStatus(HttpStatus status) {
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
