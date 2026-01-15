package com.gns.notification.controller;

import com.gns.notification.dto.ApiTokenRequest;
import com.gns.notification.dto.ApiTokenResponse;
import com.gns.notification.dto.PageResult;
import com.gns.notification.service.ApiTokenService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tokens")
public class ApiTokenController {

    private final ApiTokenService apiTokenService;

    public ApiTokenController(ApiTokenService apiTokenService) {
        this.apiTokenService = apiTokenService;
    }

    @PostMapping
    public ResponseEntity<ApiTokenResponse> createToken(@Valid @RequestBody ApiTokenRequest request) {
        return ResponseEntity.ok(apiTokenService.createToken(request));
    }

    @GetMapping
    public ResponseEntity<PageResult<ApiTokenResponse>> listTokens(
        @RequestParam(required = false) Long userId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(required = false) String search) {
        return ResponseEntity.ok(apiTokenService.listTokens(userId, PageRequest.of(page, size), search));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiTokenResponse> getToken(@PathVariable Long id) {
        return ResponseEntity.ok(apiTokenService.getToken(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteToken(@PathVariable Long id) {
        apiTokenService.deleteToken(id);
        return ResponseEntity.noContent().build();
    }
}
