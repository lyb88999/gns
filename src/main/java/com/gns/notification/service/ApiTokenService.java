package com.gns.notification.service;

import com.gns.notification.dto.ApiTokenRequest;
import com.gns.notification.dto.ApiTokenResponse;
import com.gns.notification.dto.PageResult;
import org.springframework.data.domain.Pageable;

public interface ApiTokenService {

    ApiTokenResponse createToken(ApiTokenRequest request);

    PageResult<ApiTokenResponse> listTokens(Long userId, Pageable pageable);

    ApiTokenResponse getToken(Long id);

    void deleteToken(Long id);
}
