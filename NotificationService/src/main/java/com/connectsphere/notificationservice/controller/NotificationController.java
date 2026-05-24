package com.connectsphere.notificationservice.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.connectsphere.notificationservice.dto.BulkNotificationRequest;
import com.connectsphere.notificationservice.dto.CreateNotificationRequest;
import com.connectsphere.notificationservice.entity.Notification;
import com.connectsphere.notificationservice.service.NotificationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {
	private final NotificationService notificationService;

	@PostMapping
	public ResponseEntity<Notification> createNotification(@Valid @RequestBody CreateNotificationRequest request) {
		Notification created = notificationService.createNotification(request);
		if (created == null) {
			return ResponseEntity.noContent().build();
		}
		return ResponseEntity.status(HttpStatus.CREATED).body(created);
	}

	@PostMapping("/bulk")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<Map<String, Object>> sendBulk(@Valid @RequestBody BulkNotificationRequest request) {
		notificationService.sendBulkNotifications(request.getRecipientIds(), request.getType(), request.getMessage());
		return ResponseEntity
				.ok(Map.of("message", "Bulk notification dispatched", "recipients", request.getRecipientIds().size()));
	}

	@GetMapping
	public ResponseEntity<List<Notification>> getByRecipient(@AuthenticationPrincipal Integer userId,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
		return ResponseEntity.ok(notificationService.getByRecipient(userId, page, size));
	}

	@GetMapping("/unread-count")
	public ResponseEntity<Map<String, Integer>> getUnreadCount(@AuthenticationPrincipal Integer userId) {
		return ResponseEntity.ok(Map.of("unreadCount", notificationService.getUnreadCount(userId)));
	}

	@PostMapping("/{id}/read")
	public ResponseEntity<Map<String, String>> markAsRead(@AuthenticationPrincipal int userId, @PathVariable int id) {
		notificationService.markAsRead(userId, id);
		return ResponseEntity.ok(Map.of("message", "Notifications marked as read"));
	}

	@PutMapping("/read-all")
	public ResponseEntity<Map<String, String>> markAllRead(@AuthenticationPrincipal Integer userId) {
		notificationService.markAllRead(userId);
		return ResponseEntity.ok(Map.of("message", "Notifications marked as read"));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Map<String, String>> deleteNotification(@AuthenticationPrincipal int userId,
			@PathVariable int id) {
		notificationService.deleteNotification(userId, id);
		return ResponseEntity.ok(Map.of("message", "Notification deleted"));
	}
}
