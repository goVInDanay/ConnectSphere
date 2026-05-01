package com.connectsphere.follow.service;

import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.connectsphere.follow.dto.CreateNotificationRequest;
import com.connectsphere.follow.entity.Follows;
import com.connectsphere.follow.messaging.NotificationProducer;
import com.connectsphere.follow.repository.FollowRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class FollowServiceImpl implements FollowService {

	private final FollowRepository followRepository;
	private final NotificationProducer notificationProducer;

	@Override
	public Follows follow(int followerId, int followeeId) {
		if (followerId == followeeId) {
			throw new IllegalArgumentException("User cannot follow themselves: userId = " + followerId);
		}

		return followRepository.findByFollowerIdAndFolloweeId(followerId, followeeId).orElseGet(() -> {
			Follows edge = Follows.builder().followerId(followerId).followeeId(followeeId).status(Follows.STATUS_ACTIVE)
					.build();
			try {
				Follows saved = followRepository.save(edge);
				notificationProducer.sendNotification(CreateNotificationRequest.builder().recipientId(followeeId)
						.actorId(followerId).type("FOLLOW").message("started following you").targetId(followerId)
						.targetType("USER").deepLinkUrl("/profile/" + followerId).build());
				log.info("Follow created {} -> {}", followerId, followeeId);
				return saved;
			} catch (DataIntegrityViolationException e) {
				log.warn("Concurrent follow detected : {} -> {}", followerId, followeeId);
				return followRepository.findByFollowerIdAndFolloweeId(followerId, followeeId)
						.orElseThrow(() -> new IllegalStateException("Concurrent follow inconsistency"));
			}
		});
	}

	@Override
	public void unfollow(int followerId, int followeeId) {
		if (followRepository.findByFollowerIdAndFolloweeId(followerId, followeeId).isEmpty()) {
			log.debug("Unfollow no-op: edge not found {} -> {}", followerId, followeeId);
			return;
		}

		followRepository.deleteByFollowerIdAndFolloweeId(followerId, followeeId);
		log.info("Follow removed: {} -> {}", followerId, followeeId);
	}

	@Override
	@Transactional(readOnly = true)
	public boolean isFollowing(int followerId, int followeeId) {
		return followRepository.findByFollowerIdAndFolloweeId(followerId, followeeId).map(Follows::isActive)
				.orElse(false);
	}

	@Override
	@Transactional(readOnly = true)
	public List<Follows> getFollowers(int userId) {
		return followRepository.findByFolloweeIdAndStatus(userId, Follows.STATUS_ACTIVE);
	}

	@Override
	@Transactional(readOnly = true)
	public List<Follows> getFollowing(int userId) {
		return followRepository.findByFollowerIdAndStatus(userId, Follows.STATUS_ACTIVE);
	}

	@Override
	@Transactional(readOnly = true)
	public long getFollowerCount(int userId) {
		return followRepository.countByFolloweeIdAndStatus(userId, Follows.STATUS_ACTIVE);
	}

	@Override
	@Transactional(readOnly = true)
	public long getFollowingCount(int userId) {
		return followRepository.countByFollowerIdAndStatus(userId, Follows.STATUS_ACTIVE);
	}

	@Override
	@Transactional(readOnly = true)
	public List<Integer> getMutualFollows(int userId) {
		return followRepository.findMutualFollowIds(userId);
	}

	@Override
	@Transactional(readOnly = true)
	public List<Integer> getSuggestedUsers(int userId) {
		List<Integer> candidates = followRepository.findSecondDegreeConnections(userId);
		return candidates.stream().limit(10).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<Integer> getFolloweeIds(int userId) {
		return followRepository.findFolloweeIds(userId);
	}

}
