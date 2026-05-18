package com.connectsphere.postservice.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import com.connectsphere.postservice.clients.FollowClient;
import com.connectsphere.postservice.clients.SearchClient;
import com.connectsphere.postservice.clients.UserClient;
import com.connectsphere.postservice.dto.CreateNotificationRequest;
import com.connectsphere.postservice.dto.CreatePostRequest;
import com.connectsphere.postservice.dto.UpdatePostRequest;
import com.connectsphere.postservice.dto.UserDTO;
import com.connectsphere.postservice.entity.Mention;
import com.connectsphere.postservice.entity.Post;
import com.connectsphere.postservice.exceptions.PostNotFoundException;
import com.connectsphere.postservice.messaging.NotificationProducer;
import com.connectsphere.postservice.repository.MentionRepository;
import com.connectsphere.postservice.repository.PostRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

	private final PostRepository postRepository;
	private final MentionRepository mentionRepository;
	private final SearchClient searchClient;
	private final UserClient userClient;
	private final FollowClient followClient;
	private final NotificationProducer notificationProducer;
	private final FeedCacheService feedCacheService;

	private static final Pattern MENTION_PATTERN = Pattern.compile("@(\\w+)");

	@Override
	public Post createPost(CreatePostRequest request) {
		log.info("Creating post for authorId={}", request.getAuthorId());

		String resolvedType = derivePostType(request.getPostType(), request.getMediaUrls());

		Post post = Post.builder().authorId(request.getAuthorId()).content(request.getContent())
				.mediaUrls(request.getMediaUrls() != null ? request.getMediaUrls() : new ArrayList<>())
				.postType(resolvedType).visibility(request.getVisibility() != null ? request.getVisibility() : "PUBLIC")
				.likesCount(0).commentsCount(0).sharesCount(0).isDeleted(false).build();
		Set<String> usernames = new HashSet<>(extractMentions(post.getContent()));
		Post saved = postRepository.save(post);
		if (!usernames.isEmpty()) {
			List<UserDTO> users = userClient.getUsersByUsernames(new ArrayList<>(usernames));
			List<Mention> mentions = new ArrayList<>();
			for (UserDTO user : users) {
				if (user.getUserId() == saved.getAuthorId()) {
					continue;
				}
				Mention mention = new Mention();
				mention.setPostId(saved.getPostId());
				mention.setMentionedUserId(user.getUserId());
				mentions.add(mention);
				CreateNotificationRequest notification = CreateNotificationRequest.builder()
						.recipientId(user.getUserId()).actorId(saved.getAuthorId()).type("MENTION")
						.message("You were mentioned in a post").targetId(saved.getPostId()).targetType("POST")
						.deepLinkUrl("/posts/" + saved.getPostId()).build();
				notificationProducer.sendNotification(notification);
			}
			mentionRepository.saveAll(mentions);
		}
		log.info("Post created: postId={}, authorId={}, visibility={}", saved.getPostId(), saved.getAuthorId(),
				saved.getVisibility());
		feedCacheService.evictFollowerFeeds(followClient.getFollowerIds(saved.getAuthorId()));
		feedCacheService.evictFeed(saved.getAuthorId());
		return saved;
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<Post> getPostById(int postId) {
		Optional<Post> cached = feedCacheService.getCachedPost(postId);
		if (cached.isPresent()) {
			return cached;
		}
		Optional<Post> post = postRepository.findByPostIdAndIsDeletedFalse(postId);
		post.ifPresent(feedCacheService::storePost);
		return post;
	}

	@Override
	@Transactional(readOnly = true)
	public List<Post> getPostsByUser(int userId) {
		return postRepository.findByAuthorIdAndIsDeletedFalseOrderByCreatedAtDesc(userId);
	}

	@Override
	@Transactional(readOnly = true)
	public List<Post> getFeedForUser(int userId, List<Integer> followeeIds) {
		log.debug("Generating feed for user={}", userId);

		Optional<List<Post>> cached = feedCacheService.getCachedFeed(userId);
		if (cached.isPresent()) {
			return cached.get();
		}

		List<Post> feed = new ArrayList<>();

		if (!CollectionUtils.isEmpty(followeeIds)) {
			List<Post> followeePosts = postRepository.findFeedByUserIds(followeeIds);
			feed.addAll(followeePosts);
			feed.addAll(postRepository.findByAuthorIdAndIsDeletedFalseOrderByCreatedAtDesc(userId));
		}

		List<Integer> trendingPostIds = searchClient.getTrendingPostIds(20);

		if (trendingPostIds != null && !trendingPostIds.isEmpty()) {
			List<Post> trendingPosts = postRepository.findAllById(trendingPostIds);
			feed.addAll(trendingPosts);
		}

		feed.addAll(postRepository.findByVisibilityAndIsDeletedFalseOrderByCreatedAtDesc("PUBLIC"));

		Map<Integer, Post> unique = new LinkedHashMap<>();
		for (Post p : feed) {
			if (!p.isDeleted()) {
				unique.put(p.getPostId(), p);
			}
		}
		List<Post> finalFeed = new ArrayList<>(unique.values());

		finalFeed.sort((p1, p2) -> {
			long hours1 = Duration.between(p1.getCreatedAt(), LocalDateTime.now()).toHours();
			long hours2 = Duration.between(p2.getCreatedAt(), LocalDateTime.now()).toHours();

			double score1 = (p1.getLikesCount() * 2 + p1.getCommentsCount() * 3) / (hours1 + 1.0);
			double score2 = (p2.getLikesCount() * 2 + p2.getCommentsCount() * 3) / (hours2 + 1.0);

			return Double.compare(score2, score1);
		});

		List<Post> result = finalFeed.stream().limit(50).toList();

		feedCacheService.storeFeed(userId, result);
		result.forEach(feedCacheService::storePost);

		return result;
	}

	@Override
	public Post updatePost(int postId, UpdatePostRequest request, int userId) {
		Post post = requirePost(postId);

		if (post.getAuthorId() != userId) {
			throw new RuntimeException("You are not allowed to update this post");
		}
		if (request.getContent() != null) {
			post.setContent(request.getContent());
		}
		if (request.getMediaUrls() != null) {
			post.setMediaUrls(request.getMediaUrls());
			post.setPostType(derivePostType(null, request.getMediaUrls()));
		}

		Post updated = postRepository.save(post);
		log.info("Post updated: postId={}", postId);
		feedCacheService.evictPost(postId);
		feedCacheService.evictFeed(userId);
		return updated;
	}

	@Override
	public void deletePost(int postId, int userId) {
		Post post = requirePost(postId);
		if (post.getAuthorId() != userId) {
			throw new RuntimeException("You are not allowed to delete this post");
		}
		postRepository.softDeleteByPostId(postId);
		feedCacheService.evictPost(postId);
		feedCacheService.evictFeed(userId);
		List<Integer> followers = followClient.getFollowerIds(userId);
	    followers.add(userId);
	    feedCacheService.evictFollowerFeeds(followers);
		log.info("Post soft-deleted: postId={}", postId);
	}

	@Override
	@Transactional(readOnly = true)
	public List<Post> searchPosts(String term) {
		log.debug("Searching posts for term='{}'", term);
		return postRepository.searchByContent(term);
	}

	@Override
	public void incrementLikes(int postId) {
		requirePost(postId);
		postRepository.incrementLikesCount(postId);
		feedCacheService.evictPost(postId);
		log.debug("Likes incremented for postId={}", postId);
	}

	@Override
	public void decrementLikes(int postId) {
		requirePost(postId);
		postRepository.decrementLikesCount(postId);
		feedCacheService.evictPost(postId);
		log.debug("Likes decremented for postId={}", postId);
	}

	@Override
	public void incrementComments(int postId) {
		requirePost(postId);
		postRepository.incrementCommentsCount(postId);
		feedCacheService.evictPost(postId);
		log.debug("Comments incremented for postId={}", postId);
	}

	@Override
	public void decrementComments(int postId) {
		requirePost(postId);
		postRepository.decrementCommentsCount(postId);
		feedCacheService.evictPost(postId);
		log.debug("Comments decremented for postId={}", postId);
	}

	@Override
	public void changeVisibility(int postId, String visibility, int userId) {
		Post post = requirePost(postId);
		if (post.getAuthorId() != userId) {
			throw new RuntimeException("You are not allowed to delete this post");
		}

		String oldVisibility = post.getVisibility();
		post.setVisibility(visibility);
		postRepository.save(post);
		feedCacheService.evictPost(postId);
		feedCacheService.evictFeed(userId);
		log.info("Post {} visibility changed: {} → {}", postId, oldVisibility, visibility);
	}

	@Override
	@Transactional(readOnly = true)
	public int getPostCount(int userId) {
		return postRepository.countByAuthorIdAndIsDeletedFalse(userId);
	}

	@Override
	public void adminDeletePost(int postId) {
		postRepository.softDeleteByPostId(postId);
	}

	@Override
	@Transactional(readOnly = true)
	public List<Post> getAllPosts() {
		return postRepository.findAll();
	}

	@Override
	@Transactional(readOnly = true)
	public List<Post> getVisiblePosts(int userId, Integer viewerId) {

		if (viewerId == null) {
			return postRepository.findPublicPosts(userId);
		}

		if (userId == viewerId) {
			return postRepository.findByAuthorIdAndIsDeletedFalseOrderByCreatedAtDesc(userId);
		}
		boolean isFollower = followClient.isFollowing(viewerId, userId);

		if (isFollower) {
			return postRepository.findVisibleToFollowers(userId);
		} else {
			return postRepository.findPublicPosts(userId);
		}
	}

	@Override
	public boolean canUserViewPost(Integer viewerId, Post post) {

		if (viewerId == null) {
			return "PUBLIC".equals(post.getVisibility());
		}

		if (post.getAuthorId() == viewerId)
			return true;

		switch (post.getVisibility()) {
		case "PUBLIC":
			return true;

		case "FOLLOWERS_ONLY":
			return followClient.isFollowing(viewerId, post.getAuthorId());

		case "PRIVATE":
			return false;

		default:
			return false;
		}
	}

	@Override
	@Transactional(readOnly = true)
	public List<Post> getFlaggedPosts() {
		return postRepository.findByIsFlaggedTrueAndIsDeletedFalse();
	}

	@Override
	public void approvePost(int postId) {
		Post post = requirePost(postId);
		post.setApproved(true);
		post.setFlagged(false);
		post.setReportCount(0);
		postRepository.save(post);
		log.info("Post approved by admin: {}", postId);
	}

	@Override
	public void rejectPost(int postId) {
		Post post = requirePost(postId);
		post.setDeleted(true);
		postRepository.save(post);
		notificationProducer.sendNotification(CreateNotificationRequest.builder().recipientId(post.getAuthorId())
				.type("ADMIN_ACTION").message("Your post was removed due to policy violation").targetId(postId)
				.targetType("POST").build());
		log.warn("Post rejected and deleted by admin: {}", postId);
	}

	@Override
	public void reportPost(int postId) {
		Post post = requirePost(postId);
		post.setReportCount(post.getReportCount() + 1);
		if (post.getReportCount() >= 5) {
			post.setFlagged(true);
			log.warn("Post {} auto-flagged due to reports", postId);
		}
		postRepository.save(post);
	}

	private Post requirePost(int postId) {
		Optional<Post> cached = feedCacheService.getCachedPost(postId);
		if (cached.isPresent()) {
			return cached.get();
		}
		Post post = postRepository.findByPostIdAndIsDeletedFalse(postId)
				.orElseThrow(() -> new PostNotFoundException("Post not found: " + postId));
		feedCacheService.storePost(post);
		return post;
	}

	private String derivePostType(String explicitType, List<String> mediaUrls) {
		if (explicitType != null && !explicitType.equals("TEXT")) {
			return explicitType;
		}
		if (CollectionUtils.isEmpty(mediaUrls)) {
			return "TEXT";
		}
		boolean hasVideo = mediaUrls.stream().anyMatch(u -> u.endsWith(".mp4") || u.contains("/video/"));
		boolean hasImage = mediaUrls.stream()
				.anyMatch(u -> u.endsWith(".jpg") || u.endsWith(".jpeg") || u.endsWith(".png") || u.endsWith(".webp"));

		if (hasVideo && hasImage)
			return "MIXED";
		if (hasVideo)
			return "VIDEO";
		return "IMAGE";
	}

	private List<String> extractMentions(String content) {
		List<String> usernames = new ArrayList<>();
		Matcher matcher = MENTION_PATTERN.matcher(content);

		while (matcher.find()) {
			usernames.add(matcher.group(1));
		}

		return usernames;
	}

}
