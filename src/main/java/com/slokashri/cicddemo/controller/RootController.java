package com.slokashri.cicddemo.controller;

import com.slokashri.cicddemo.model.Member;
import com.slokashri.cicddemo.repo.MemberRepo;
import com.slokashri.cicddemo.service.MemberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

@RestController
public class RootController {
    @Autowired
    private MemberService memberService;

    @GetMapping(value = "/", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> helloWorld(){
            return ResponseEntity.ok("Welcome to Slokashri's CICD Demo Application");
    }

    @GetMapping(value = "/echo",produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> echo(){
        return ResponseEntity.ok(OffsetDateTime.now(ZoneId.of("UTC")).toString());
    }

    @GetMapping(value = "/member",produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Member>> getMembers(){
        return ResponseEntity.ok(memberService.listMembers());
    }

}
