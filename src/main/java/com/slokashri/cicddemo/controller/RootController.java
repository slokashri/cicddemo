package com.slokashri.cicddemo.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.time.ZoneId;

@RestController
public class RootController {
    @GetMapping(value = "/", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> helloWorld(){
            return ResponseEntity.ok("Welcome to Slokashri's CICD Demo Application");
    }

    @GetMapping(value = "/echo",produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> echo(){
        return ResponseEntity.ok(OffsetDateTime.now(ZoneId.of("UTC")).toString());
    }
}
