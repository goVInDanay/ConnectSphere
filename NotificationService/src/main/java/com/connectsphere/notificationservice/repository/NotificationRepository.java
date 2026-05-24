package com.connectsphere.notificationservice.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.connectsphere.notificationservice.entity.Notification;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Integer> {
	List<Notification> findByRecipientIdOrderByCreatedAtDesc(int recipientId, Pageable pageable);

	List<Notification> findByRecipientIdAndReadStatusFalse(int recipientId);

	int countByRecipientIdAndReadStatusFalse(int recipientId);

	@Modifying
	@Query("UPDATE Notification n SET n.readStatus = true WHERE n.recipientId = :recipientId AND n.readStatus = false")
	void markAllAsRead(@Param("recipientId") int recipientId);

	@Modifying
	@Query("UPDATE Notification n SET n.readStatus = true WHERE n.notificationId = :notificationId")
	void markAsReadById(@Param("notificationId") int notificationId);

	Optional<Notification> findByActorIdAndTargetIdAndType(int actorId, int targetId, String type);

	List<Notification> findByRecipientIdAndTypeOrderByCreatedAtDesc(int recipientId, String type);

	void deleteByNotificationId(int notificationId);

	void deleteByRecipientId(int recipientId);

	boolean existsByNotificationIdAndRecipientId(int notificationId, int recipientId);

	Optional<Notification> findByNotificationId(int notificationId);
}
