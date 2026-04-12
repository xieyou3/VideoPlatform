package com.videoplatform.search.controller;

import com.videoplatform.common.api.ApiResponse;
import com.videoplatform.search.service.SearchSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/search")
@RequiredArgsConstructor
public class SearchSyncController {

    private final SearchSyncService searchSyncService;

    @PostMapping("/sync")
    public ApiResponse<Void> triggerSync() {
        searchSyncService.syncPendingEvents();
        return ApiResponse.success("OK", null);
    }
}
