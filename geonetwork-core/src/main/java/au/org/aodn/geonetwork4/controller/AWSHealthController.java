package au.org.aodn.geonetwork4.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The actuator health endpoint only produces application/json (and actuator vendor types), so a client that
 * sends no Accept header, or an unsupported one, may be rejected. This endpoint reuses the same health result
 * but does not restrict the Accept header, and always answers with JSON.
 * Url: {management.endpoints.web.base-path}/healthcheck, e.g. /srv/api/manage/healthcheck
 */
@RestController
@RequestMapping("${management.endpoints.web.base-path:/actuator}")
public class AWSHealthController {

    @GetMapping(value = "/awscheck", produces = MediaType.ALL_VALUE)
    public ResponseEntity<Void> awsCheck() {
        return ResponseEntity
                .status(HttpStatus.OK)
                .build();
    }
}
