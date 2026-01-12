package com.gns.notification.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gns.notification.domain.NotificationLog;
import com.gns.notification.domain.NotificationLogMapper;
import com.gns.notification.dto.NotificationLogResponse;
import com.gns.notification.dto.PageResult;
import com.gns.notification.service.NotificationLogService;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class NotificationLogServiceImpl implements NotificationLogService {

    private final NotificationLogMapper mapper;

    public NotificationLogServiceImpl(NotificationLogMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public PageResult<NotificationLogResponse> listLogs(Pageable pageable, String status, String search) {
        Page<NotificationLog> page = new Page<>(pageable.getPageNumber() + 1, pageable.getPageSize());
        LambdaQueryWrapper<NotificationLog> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(status) && !"all".equalsIgnoreCase(status)) {
            wrapper.eq(NotificationLog::getStatus, status);
        }

        if (StringUtils.hasText(search)) {
            wrapper.and(w -> w.like(NotificationLog::getTaskName, search)
                    .or()
                    .like(NotificationLog::getRecipient, search)
                    .or()
                    .like(NotificationLog::getTaskId, search));
        }
        
        wrapper.orderByDesc(NotificationLog::getSentAt);

        Page<NotificationLog> result = mapper.selectPage(page, wrapper);

        List<NotificationLogResponse> responses = result.getRecords().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return new PageResult<>(responses, result.getTotal(), result.getPages());
    }

    private NotificationLogResponse toResponse(NotificationLog log) {
        return NotificationLogResponse.builder()
                .id(log.getId())
                .taskId(log.getTaskId())
                .taskName(log.getTaskName())
                .channel(log.getChannel())
                .recipient(log.getRecipient())
                .status(log.getStatus() == null ? null : log.getStatus().toUpperCase())
                .errorMessage(log.getErrorMessage())
                .sentAt(log.getSentAt())
                .build();
    }
}
