package com.connectsphere.postservice.service;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.connectsphere.postservice.entity.Post;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeedCacheService {
	private final RedisTemplate<String, Object> redisTemplate;

	private static final String FEED_KEY = "feed:user:%d";
	private final String POST_KEY = "post:%d";

	@Value("${app.feed.cache.ttl-seconds:300}")
	private long feedTtlSeconds;

	@Value("${app.feed.post.ttl-seconds:300}")
	private long postTtlSeconds;

	public Optional<List<Post>> getCachedFeed(int userId) {
		String key = feedKey(userId);
		try {
			Object cached = redisTemplate.opsForValue().get(key);
			if (cached instanceof List<?> list) {
				log.debug("Feed cached HIT for user = {}, {}", userId, list);
				return Optional.of((List<Post>) list);
			}
		} catch (Exception e) {
			log.warn("Redis read error for key = {}: {}", key, e.getMessage());
		}
		log.debug("Feed cache MISS for user={}", userId);
		return Optional.empty();
	}

	public void storeFeed(int userId, List<Post> feed) {
		String key = feedKey(userId);
		try {
			redisTemplate.opsForValue().set(key, feed, Duration.ofSeconds(feedTtlSeconds));
			log.debug("Feed cached for user={}, size ={}, ttl = {} s, {}", userId, feed.size(), feedTtlSeconds, feed);
		} catch (Exception e) {
			log.warn("Redis write error for key = {}:{}", key, e.getMessage());
		}
	}

	public void evictFeed(int userId) {
		String key = feedKey(userId);
		try {
			redisTemplate.delete(key);
			log.debug("Feed evicted for user={}", userId);
		} catch (Exception e) {
			log.warn("Redis evict error for userId={}: {}", userId, e.getMessage());
		}
	}

	public void evictFollowerFeeds(List<Integer> followerIds) {
		if (followerIds == null || followerIds.isEmpty()) {
			return;
		}

		try {
			List<String> keys = followerIds.stream().map(this::feedKey).toList();
			redisTemplate.delete(keys);
			log.debug("Feed evicted for {} followers", keys.size());
		} catch (Exception e) {
			log.warn("Redis bulk evict error: {}", e.getMessage());
		}
	}

	public Optional<Post> getCachedPost(int postId) {
		String key = postKey(postId);
		try {
			Object cached = redisTemplate.opsForValue().get(key);
			if (cached instanceof Post post) {
				log.debug("Post cache HIT postId = {},{}", postId, post);
				return Optional.of(post);
			}
		} catch (Exception e) {
			log.warn("Redis read error for key = {}: {}", key, e.getMessage());
		}
		return Optional.empty();
	}

	public void storePost(Post post) {
		String key = postKey(post.getPostId());
		try {
			redisTemplate.opsForValue().set(key, post, Duration.ofSeconds(postTtlSeconds));
			log.debug("Post cached postId = {}", post.getPostId());
		} catch (Exception e) {
			log.warn("Redis write error for key = {}:{}", key, e.getMessage());
		}
	}

	public void evictPost(int postId) {
		String key = postKey(postId);
		try {
			redisTemplate.delete(key);
			log.debug("Post evicted for postId={}", postId);
		} catch (Exception e) {
			log.warn("Redis evict error for key = {}:{}", key, e.getMessage());
		}
	}

	private String feedKey(int userId) {
		return String.format(FEED_KEY, userId);
	}

	private String feedKey(Integer userId) {
		return String.format(FEED_KEY, userId);
	}

	private String postKey(int postId) {
		return String.format(POST_KEY, postId);
	}
}
