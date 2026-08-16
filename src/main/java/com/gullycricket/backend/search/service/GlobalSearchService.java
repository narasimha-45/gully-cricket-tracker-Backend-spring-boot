package com.gullycricket.backend.search.service;

import com.gullycricket.backend.search.dto.GlobalSearchResponseDto;
import com.gullycricket.backend.search.repository.SearchReadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GlobalSearchService {

    private final SearchReadRepository searchReadRepository;

    public GlobalSearchResponseDto globalSearch(String query) {
        String normalized = query == null ? "" : query.trim();
        if (normalized.length() < 2) {
            return new GlobalSearchResponseDto(List.of(), List.of(), List.of());
        }
        // One JDBC round trip returns players + match counts + teams + seasons.
        return searchReadRepository.globalSearch(normalized);
    }
}
