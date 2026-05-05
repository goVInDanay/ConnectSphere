package com.connectsphere.likeservice.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.connectsphere.likeservice.client.CommentServiceClient;
import com.connectsphere.likeservice.client.PostServiceClient;
import com.connectsphere.likeservice.dto.CreateNotificationRequest;
import com.connectsphere.likeservice.entity.Like;
import com.connectsphere.likeservice.exception.ResourceNotFoundException;
import com.connectsphere.likeservice.messaging.NotificationProducer;
import com.connectsphere.likeservice.repository.LikeRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class LikeServiceImpl implements LikeService {

	private final LikeRepository likeRepository;
	private final PostServiceClient postServiceClient;
	private final CommentServiceClient commentServiceClient;
	private final NotificationProducer notificationProducer;

	@Override
	public Like likeTarget(int userId, int targetId, String targetType, String reactionType) {
		validateReactionType(reactionType);
		validateTargetType(targetType);

		Optional<Like> existing = likeRepository.findByUserIdAndTargetIdAndTargetType(userId, targetId, targetType);

		if (existing.isPresent()) {
			Like existingLike = existing.get();
			if (existingLike.getReactionType().equals(reactionType)) {
				log.debug("Idempotent like: userId={}, targetId={}, type={}", userId, targetId, reactionType);
				return existingLike;
			}

			existingLike.setReactionType(reactionType);
			Like updated = likeRepository.save(existingLike);
			log.info("Reaction changed in-place: userId={}, targetId={}, {}", userId, targetId, reactionType);
			return updated;
		}

		Like newLike = Like.builder().userId(userId).targetId(targetId).targetType(targetType)
				.reactionType(reactionType).build();

		Like saved;
		try {
			saved = likeRepository.save(newLike);
		} catch (DataIntegrityViolationException e) {
			log.warn("Concurrent like detected — loading existing: userId={}, targetId={}", userId, targetId);
			return likeRepository.findByUserIdAndTargetIdAndTargetType(userId, targetId, targetType)
					.orElseThrow(() -> new IllegalStateException("Concurrent like inconsistency"));
		}

		incrementTargetCounter(targetId, targetType);
		sendLikeNotification(userId, targetId, targetType);
		log.info("Reaction added: likeId={}, userId={}, targetId={}, targetType={}, reaction={}", saved.getLikeId(),
				userId, targetId, targetId, reactionType);

		return saved;
	}

	@Override
	public void unlikeTarget(int userId, int targetId, String targetType) {
		validateTargetType(targetType);

		Optional<Like> existing = likeRepository.findByUserIdAndTargetIdAndTargetType(userId, targetId, targetType);
		if (existing.isEmpty()) {
			log.debug("Unlike no-op: no reaction found for userId={}, targetId={}", userId, targetId);
			return;
		}

		likeRepository.deleteByUserIdAndTargetIdAndTargetType(userId, targetId, targetType);
		decrementTargetCounter(targetId, targetType);
		log.info("Reaction removed: userId={}, targetId={}, targetType={}", userId, targetId, targetType);
	}

	@Override
	@Transactional(readOnly = true)
	public boolean hasLiked(int userId, int targetId, String targetType) {
		return likeRepository.existsByUserIdAndTargetIdAndTargetType(userId, targetId, targetType);
	}

	@Override
	@Transactional(readOnly = true)
	public List<Like> getLikesByUser(int userId) {
		return likeRepository.findByUserId(userId);
	}

	@Override
	@Transactional(readOnly = true)
	public List<Like> getLikesByTarget(int targetId, String targetType) {
		validateTargetType(targetType);
		return likeRepository.findByTargetIdAndTargetType(targetId, targetType);
	}

	@Override
	@Transactional(readOnly = true)
	public long getLikeCount(int targetId, String targetType) {
		validateTargetType(targetType);
		return likeRepository.countByTargetIdAndTargetType(targetId, targetType);
	}

	@Override
	@Transactional(readOnly = true)
	public long getLikeCountByType(int targetId, String targetType, String reactionType) {
		validateReactionType(reactionType);
		validateTargetType(targetType);
		return likeRepository.countByTargetIdAndTargetTypeAndReactionType(targetId, targetType, reactionType);
	}

	@Override
	@Transactional(readOnly = true)
	public Map<String, Long> getReactionSummary(int targetId, String targetType) {
		validateTargetType(targetType);
		List<Object[]> rows = likeRepository.findReactionSummaryRaw(targetId, targetType);
		Map<String, Long> summary = new LinkedHashMap<>();
		for (Object[] row : rows) {
			summary.put((String) row[0], (Long) row[1]);
		}
		return summary;
	}

	@Override
	public Like changeReaction(int userId, int targetId, String targetType, String newReactionType) {
		validateReactionType(newReactionType);
		validateTargetType(targetType);
		Like existing = likeRepository.findByUserIdAndTargetIdAndTargetType(userId, targetId, targetType)
				.orElseThrow(() -> new ResourceNotFoundException(
						"No reaction found for userId = " + userId + ", targetId = " + targetId));
		String oldType = existing.getReactionType();
		if (oldType.equals(newReactionType)) {
			log.debug("changeReaction no-op: same type={}", newReactionType);
			return existing;
		}

		existing.setReactionType(newReactionType);
		Like updated = likeRepository.save(existing);
		log.info("Reaction changed: userId={}, targetId={}, {} -> {}", userId, targetId, oldType, newReactionType);
		return updated;
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<Like> getUserReaction(int userId, int targetId, String targetType) {
		return likeRepository.findByUserIdAndTargetIdAndTargetType(userId, targetId, targetType);
	}

	private void validateReactionType(String reactionType) {
		Set<String> valid = Set.of(Like.REACTION_LIKE, Like.REACTION_LOVE, Like.REACTION_HAHA, Like.REACTION_WOW,
				Like.REACTION_SAD, Like.REACTION_ANGRY);
		if (!valid.contains(reactionType)) {
			throw new IllegalArgumentException(
					"Invalid reactionType: '" + reactionType + "'. Must be one of: " + valid);
		}
	}

	private void validateTargetType(String targetType) {
		if (!Like.TARGET_POST.equals(targetType) && !Like.TARGET_COMMENT.equals(targetType)) {
			throw new IllegalArgumentException("Invalid targetType: '" + targetType + "'. Must be POST or COMMENT.");
		}
	}

	private void incrementTargetCounter(int targetId, String targetType) {
		try {
			if (Like.TARGET_POST.equals(targetType)) {
				postServiceClient.incrementLikeCount(targetId);
				log.debug("Post-Service likesCount incremented: postId={}", targetId);
			} else if (Like.TARGET_COMMENT.equals(targetType)) {
				commentServiceClient.incrementCommentCount(targetId);
				log.debug("Comment-Service likesCount incremented: commentId={}", targetId);
			}
		} catch (Exception ex) {
			log.error("Counter increment failed: targetId={}, targetType={}: {}", targetId, targetType,
					ex.getMessage());
		}
	}

	private void decrementTargetCounter(int targetId, String targetType) {
		try {
			if (Like.TARGET_POST.equals(targetType)) {
				postServiceClient.decrementLikeCount(targetId);
				log.debug("Post-Service likesCount decremented: postId={}", targetId);
			} else if (Like.TARGET_COMMENT.equals(targetType)) {
				commentServiceClient.decrementCommentCount(targetId);
				log.debug("Comment-Service likesCount decremented: commentId={}", targetId);
			}
		} catch (Exception ex) {
			log.error("Counter decrement failed: targetId={}, targetType={}: {}", targetId, targetType,
					ex.getMessage());
		}
	}

	private void sendLikeNotification(int userId, int targetId, String targetType) {
		try {
			int recipientId;
			if (Like.TARGET_POST.equals(targetType)) {
				recipientId = postServiceClient.getPostAuthor(targetId);
			} else if (Like.TARGET_COMMENT.equals(targetType)) {
				recipientId = commentServiceClient.getCommentAuthor(targetId);

			} else {
				return;
			}
			if (recipientId == userId) {
				return;
			}
			CreateNotificationRequest notification = CreateNotificationRequest.builder().recipientId(recipientId)
					.actorId(userId).type("LIKE").message("Someone liked your " + targetType.toLowerCase())
					.targetId(targetId).targetType(targetType)
					.deepLinkUrl("/" + targetType.toLowerCase() + "s/" + targetId).build();

			notificationProducer.sendNotification(notification);

			log.debug("Like notification sent: actor={}, recipient={}, target={}", userId, recipientId, targetId);

		} catch (Exception ex) {
			log.error("Failed to send like notification: {}", ex.getMessage());
		}
	}
}
