package com.gns.notification.service;

import com.gns.notification.dto.NotificationSendRequest;
import com.gns.notification.dto.NotificationTaskResponse;

public interface NotificationSendService {

    NotificationTaskResponse send(NotificationSendRequest request);
}
