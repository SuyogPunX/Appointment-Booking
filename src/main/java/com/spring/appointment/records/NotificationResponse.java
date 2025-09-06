package com.spring.appointment.records;

import com.spring.appointment.model.Notification;
import com.spring.appointment.enums.NotificationStatus;

import java.time.LocalDateTime;

public record NotificationResponse(
        Long notificationId,
        String message,
        NotificationStatus status,
        LocalDateTime createdAt
) {
    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getNotificationId(),
                notification.getMessage(),
                notification.getStatus(),
                notification.getCreatedAt()
        );
    }
}