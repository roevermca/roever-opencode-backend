package com.ams.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ams.dto.CleanupRequest;
import com.ams.dto.CleanupResponse;
import com.ams.dto.StorageStatusResponse;
import com.ams.security.SecurityUtils;
import com.ams.service.DatabaseStorageService;

@RestController
@RequestMapping("/api/system")
public class SystemController {

    private final DatabaseStorageService storageService;

    public SystemController(DatabaseStorageService storageService) {
        this.storageService = storageService;
    }

    @GetMapping("/storage-status")
    public ResponseEntity<StorageStatusResponse> getStorageStatus() {
        SecurityUtils.enforceSystemAdminAccess();
        return ResponseEntity.ok(storageService.getStorageStatus());
    }

    @PostMapping("/cleanup")
    public ResponseEntity<CleanupResponse> triggerCleanup(@RequestBody(required = false) CleanupRequest request) {
        SecurityUtils.enforceSystemAdminAccess();
        CleanupRequest req = request != null ? request : new CleanupRequest(null, false);
        return ResponseEntity.ok(storageService.performManualCleanup(req.getCutoffDate(), req.isDryRun()));
    }
}
