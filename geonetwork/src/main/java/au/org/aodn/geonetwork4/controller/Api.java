package au.org.aodn.geonetwork4.controller;

import au.org.aodn.geonetwork4.Setup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class Api {

    @Autowired
    protected Setup setup;

    @GetMapping("/setup")
    public ResponseEntity<?> setup() {
        setup.getMe();
        setup.insertLogos("/config/logos/aad_logo.json");

        return ResponseEntity.ok(null);
    }
}
