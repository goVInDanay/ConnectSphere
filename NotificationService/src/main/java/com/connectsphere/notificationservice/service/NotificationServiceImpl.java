package com.connectsphere.notificationservice.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.connectsphere.notificationservice.client.UserClient;
import com.connectsphere.notificationservice.dto.CreateNotificationRequest;
import com.connectsphere.notificationservice.entity.Notification;
import com.connectsphere.notificationservice.repository.NotificationRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

	private final NotificationRepository notificationRepository;
	private final JavaMailSender mailSender;
	private final UserClient userClient;

	@Value("${app.mail.from:noreply@connectsphere.com}")
	private String fromEmail;

	@Value("${app.frontend-base-url:http://localhost:3000}")
	private String frontendBaseUrl;

	@Override
	public Notification createNotification(CreateNotificationRequest request) {
		if (request.getActorId() == request.getRecipientId()) {
			log.debug("Self notify skipped, Id = {}", request.getActorId());
			return null;
		}

		if (request.getTargetId() > 0) {
			boolean duplicate = notificationRepository
					.findByActorIdAndTargetIdAndType(request.getActorId(), request.getTargetId(), request.getType())
					.isPresent();
			if (duplicate) {
				log.debug("Duplicate notification skipped: actor={}, target={}, type={}", request.getActorId(),
						request.getTargetId(), request.getType());
				return null;
			}
		}

		Notification notification = Notification.builder().recipientId(request.getRecipientId())
				.actorId(request.getActorId()).type(request.getType()).message(request.getMessage())
				.targetId(request.getTargetId()).targetType(request.getTargetType())
				.deepLinkUrl(request.getDeepLinkUrl()).readStatus(false).build();

		Notification saved = notificationRepository.save(notification);
		String email = null;

		try {
			email = userClient.getUserEmail(request.getRecipientId()).getEmail();
		} catch (Exception e) {
			log.warn("Failed to fetch email for userId={}", request.getRecipientId());
		}
		log.info("Notification created : id={}, recipient={} type={}", saved.getNotificationId(),
				saved.getRecipientId(), saved.getType());
		if (saved.isHighPriority() && email != null && !email.isBlank() && email.contains("@")) {
			log.info("Sending email");
			sendEmailAlert(saved, email);
		} else {
			log.warn("Skipping email, invalid recipient: {}", email);
		}

		return saved;
	}

	@Override
	public void sendBulkNotifications(List<Integer> recipientIds, String type, String message) {
		if (recipientIds == null || recipientIds.isEmpty()) {
			return;
		}

		List<Notification> batch = new ArrayList<>(recipientIds.size());

		for (int recipientId : recipientIds) {
			batch.add(Notification.builder().recipientId(recipientId).actorId(0).type(type).message(message)
					.readStatus(false).build());
		}
		notificationRepository.saveAll(batch);
		log.info("Bulk notification dispatched: type = {}, recipients = {}", type, recipientIds.size());
	}

	@Override
	public void markAsRead(int userId, int notificationId) {
		Notification notification = notificationRepository.findByNotificationId(notificationId)
				.orElseThrow(() -> new RuntimeException("Notification not found"));

		if (notification.getRecipientId() != userId) {
			throw new RuntimeException("Unauthorized access to notification");
		}

		notification.setReadStatus(true);
		notificationRepository.save(notification);
		log.debug("Notification marked read: id={}", notificationId);
	}

	@Override
	public void markAllRead(int recipientId) {
		notificationRepository.markAllAsRead(recipientId);
		log.info("All notifications marked read for recipientId={}", recipientId);
	}

	@Override
	@Transactional(readOnly = true)
	public List<Notification> getByRecipient(int recipientId, int page, int size) {
		int maxSize = Math.min(size, 50);
		return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(recipientId, PageRequest.of(page, maxSize));
	}

	@Override
	@Transactional(readOnly = true)
	public int getUnreadCount(int recipientId) {
		return notificationRepository.countByRecipientIdAndReadStatusFalse(recipientId);
	}

	@Override
	public void deleteNotification(int userId, int notificationId) {
		Notification notification = notificationRepository.findByNotificationId(notificationId)
				.orElseThrow(() -> new RuntimeException("Notification not found"));

		if (notification.getRecipientId() != userId) {
			throw new RuntimeException("Unauthorized delete attempt");
		}

		notificationRepository.delete(notification);

		log.info("Notification deleted: id={}", notificationId);

	}

	@Override
	public void sendEmailAlert(Notification notification, String recipientEmail) {
		try {

			SimpleMailMessage mail = new SimpleMailMessage();
			mail.setFrom(fromEmail);
			mail.setTo(recipientEmail);
			mail.setSubject(buildEmailSubject(notification));
			mail.setText(buildEmailBody(notification));
			mailSender.send(mail);
			log.info("Email alert sent: notificationId={}, to={}", notification.getNotificationId(), recipientEmail);
		} catch (Exception e) {
			log.error("{}", e);
			log.error("Email alert failed for notificationId={}:{}", notification.getNotificationId(), e.getMessage());
		}
	}

	private String buildEmailSubject(Notification notification) {
		return switch (notification.getType()) {
		case Notification.TYPE_FOLLOW -> "You have a new follower on ConnectSphere";
		case Notification.TYPE_ACCOUNT_ACTION -> "Important: Account changes on ConnectSphere";
		default -> "New activity on ConnectSphere";
		};
	}

	private String buildEmailBody(Notification notification) {
		String link = (notification.getDeepLinkUrl() != null) ? frontendBaseUrl + notification.getDeepLinkUrl()
				: frontendBaseUrl;
		return notification.getMessage() + "\n\n View it here: " + link;
	}

}
