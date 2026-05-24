package com.connectsphere.notificationservice.messaging;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import com.connectsphere.notificationservice.dto.CreateNotificationRequest;
import com.connectsphere.notificationservice.service.NotificationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationConsumer {
	private final NotificationService notificationService;

	@RabbitListener(queues = "${rabbitmq.queue}")
	public void consume(CreateNotificationRequest request) {
		log.info("Received notification request: {}", request);

		try {
			notificationService.createNotification(request);
		} catch (Exception e) {
			log.error("Error processing request {}: {}", request, e);
			throw e;
		}
	}
}