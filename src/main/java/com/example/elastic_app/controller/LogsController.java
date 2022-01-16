package com.example.elastic_app.controller;

import com.example.elastic_app.model.Logs;
import com.example.elastic_app.service.LogsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
@Log4j2
public class LogsController {

    final static String INDEX = "access-*";

    private final LogsService logsService;

    @GetMapping("/verb")
    public Page<Logs> getByVerb(Pageable pageable){
        return logsService.getLogs("POST", pageable);
    }

    @GetMapping("/verb/count")
    public Long countByVerb(){
        return logsService.countByVerb("GET");
    }

    @GetMapping(value = "/export", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<InputStreamResource> export(String filterField, String start, String end, String... fields) throws IOException {

        var bis = logsService.exportBrowser(filterField, INDEX, start, end, fields);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "inline; filename=customers.pdf");

        return ResponseEntity
                .ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_PDF)
                .body(new InputStreamResource(bis));
    }

    @GetMapping("/count")
    public ResponseEntity<Object> aggregation(String filterField, String start, String end, String... fields) throws IOException {
        var list = logsService.aggregation(filterField, INDEX, start, end, fields);
        return ResponseEntity.ok(list);
    }
}
