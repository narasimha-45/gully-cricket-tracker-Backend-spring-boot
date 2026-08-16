package com.gullycricket.backend.search.controller;

import com.gullycricket.backend.search.dto.GlobalSearchResponseDto;
import com.gullycricket.backend.search.service.GlobalSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class GlobalSearchController {

    private final GlobalSearchService globalSearchService;

    @GetMapping
    public ResponseEntity<GlobalSearchResponseDto> globalSearch(@RequestParam String query) {
        return ResponseEntity.ok(globalSearchService.globalSearch(query));
    }
}
