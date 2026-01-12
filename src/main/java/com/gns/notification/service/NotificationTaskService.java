package com.gns.notification.service;

import com.gns.notification.dto.NotificationTaskRequest;
import com.gns.notification.dto.NotificationTaskResponse;
import com.gns.notification.dto.PageResult;
import org.springframework.data.domain.Pageable;

public interface NotificationTaskService {

    NotificationTaskResponse createTask(NotificationTaskRequest request);

    NotificationTaskResponse updateTask(String taskId, NotificationTaskRequest request);

    NotificationTaskResponse getTask(String taskId);

    PageResult<NotificationTaskResponse> listTasks(Pageable pageable);

    void deleteTask(String taskId);

    void markStreamEntry(String taskId, String streamEntryId);
}
