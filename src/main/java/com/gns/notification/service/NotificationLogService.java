package com.gns.notification.service;

import com.gns.notification.dto.NotificationLogResponse;
import com.gns.notification.dto.PageResult;
import org.springframework.data.domain.Pageable;

public interface NotificationLogService {
    PageResult<NotificationLogResponse> listLogs(Pageable pageable, String status, String search);
}
