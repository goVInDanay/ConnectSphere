package com.connectsphere.notificationservice.service;

import java.util.List;

import com.connectsphere.notificationservice.dto.CreateNotificationRequest;
import com.connectsphere.notificationservice.entity.Notification;

public interface NotificationService {
	Notification createNotification(CreateNotificationRequest request);

	void sendBulkNotifications(List<Integer> recipientIds, String type, String message);

	void markAsRead(int userId, int notificationId);

	void markAllRead(int recipientId);

	List<Notification> getByRecipient(int recipientId, int page, int size);

	int getUnreadCount(int recipientId);

	void deleteNotification(int userId, int notificationId);

	void sendEmailAlert(Notification notification, String recipientEmail);
}
