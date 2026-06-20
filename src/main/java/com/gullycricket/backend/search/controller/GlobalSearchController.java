package com.gullycricket.backend.search.controller;

import com.gullycricket.backend.search.DTOs.GlobalSearchResponseDto;
import com.gullycricket.backend.search.service.GlobalSearchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("api/search")
public class GlobalSearchController {

    @Autowired
    private GlobalSearchService globalSearchService;

    @GetMapping
    public ResponseEntity<GlobalSearchResponseDto> globalSearch(@RequestParam String query){
        log.info("search requested with the query {}",query);
        return ResponseEntity.ok(globalSearchService.globalSearch(query));
    }

}
