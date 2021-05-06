package io.github.sogis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MainController {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    Settings settings;
    
    @GetMapping("/settings")
    public ResponseEntity<?> getSettings() {
        return ResponseEntity.ok().body(settings);
    }
    
    @GetMapping("/ping")
    public ResponseEntity<String> ping()  {
        return new ResponseEntity<String>("modelfinder", HttpStatus.OK);
    }

}
