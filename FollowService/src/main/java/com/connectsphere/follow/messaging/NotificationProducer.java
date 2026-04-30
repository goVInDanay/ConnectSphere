package com.connectsphere.follow.messaging;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.connectsphere.follow.dto.CreateNotificationRequest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationProducer {
	private final RabbitTemplate rabbitTemplate;

	@Value("${rabbitmq.exchange}")
	private String exchange;

	@Value("${rabbitmq.routing.key}")
	private String routingKey;

	public void sendNotification(CreateNotificationRequest request) {
		log.debug("Sending notification event: {}", request);
		rabbitTemplate.convertAndSend(exchange, routingKey, request);
	}
}
