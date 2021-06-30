package de.ipvs.as.mbp.discovery_repository.endpoints.rest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for responding to REST requests.
 */
@RestController
public class RestMainController {

    @PostMapping("/test")
    public ResponseEntity<String> randomVote() {
        return new ResponseEntity<>("test", HttpStatus.OK);
    }
}
