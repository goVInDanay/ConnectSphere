package com.connectsphere.postservice.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import com.connectsphere.postservice.dto.CreatePostRequest;
import com.connectsphere.postservice.dto.UpdatePostRequest;
import com.connectsphere.postservice.entity.Post;
import com.connectsphere.postservice.exceptions.PostNotFoundException;
import com.connectsphere.postservice.repository.PostRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

	private final PostRepository postRepository;

	@Override
	public Post createPost(CreatePostRequest request) {
		log.info("Creating post for authorId={}", request.getAuthorId());

		String resolvedType = derivePostType(request.getPostType(), request.getMediaUrls());

		Post post = Post.builder().authorId(request.getAuthorId()).content(request.getContent())
				.mediaUrls(request.getMediaUrls() != null ? request.getMediaUrls() : new ArrayList<>())
				.postType(resolvedType).visibility(request.getVisibility() != null ? request.getVisibility() : "PUBLIC")
				.likesCount(0).commentsCount(0).sharesCount(0).isDeleted(false).build();

		Post saved = postRepository.save(post);
		log.info("Post created: postId={}, authorId={}, visibility={}", saved.getPostId(), saved.getAuthorId(),
				saved.getVisibility());
		return saved;
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<Post> getPostById(int postId) {
		return postRepository.findByPostIdAndIsDeletedFalse(postId);
	}

	@Override
	@Transactional(readOnly = true)
	public List<Post> getPostsByUser(int userId) {
		return postRepository.findByAuthorIdAndIsDeletedFalseOrderByCreatedAtDesc(userId);
	}

	@Override
	@Transactional(readOnly = true)
	public List<Post> getFeedForUser(int userId, List<Integer> followeeIds) {
		if (CollectionUtils.isEmpty(followeeIds)) {
			log.debug("User {} has no followees — returning empty feed", userId);
			return new ArrayList<>();
		}
		log.debug("Generating feed for userId={} from {} followees", userId, followeeIds.size());
		return postRepository.findFeedByUserIds(followeeIds);
	}

	@Override
	public Post updatePost(int postId, UpdatePostRequest request) {
		Post post = requirePost(postId);

		if (request.getContent() != null) {
			post.setContent(request.getContent());
		}
		if (request.getMediaUrls() != null) {
			post.setMediaUrls(request.getMediaUrls());
			post.setPostType(derivePostType(null, request.getMediaUrls()));
		}

		Post updated = postRepository.save(post);
		log.info("Post updated: postId={}", postId);
		return updated;
	}

	@Override
	public void deletePost(int postId) {
		requirePost(postId);
		postRepository.softDeleteByPostId(postId);
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
		log.debug("Likes incremented for postId={}", postId);
	}

	@Override
	public void decrementLikes(int postId) {
		requirePost(postId);
		postRepository.decrementLikesCount(postId);
		log.debug("Likes decremented for postId={}", postId);
	}

	@Override
	public void incrementComments(int postId) {
		requirePost(postId);
		postRepository.incrementCommentsCount(postId);
		log.debug("Comments incremented for postId={}", postId);
	}

	@Override
	public void decrementComments(int postId) {
		requirePost(postId);
		postRepository.decrementCommentsCount(postId);
		log.debug("Comments decremented for postId={}", postId);
	}

	@Override
	public void changeVisibility(int postId, String visibility) {
		Post post = requirePost(postId);
		String oldVisibility = post.getVisibility();
		post.setVisibility(visibility);
		postRepository.save(post);
		log.info("Post {} visibility changed: {} → {}", postId, oldVisibility, visibility);
	}

	@Override
	@Transactional(readOnly = true)
	public int getPostCount(int userId) {
		return postRepository.countByAuthorIdAndIsDeletedFalse(userId);
	}

	private Post requirePost(int postId) {
		return postRepository.findByPostIdAndIsDeletedFalse(postId)
				.orElseThrow(() -> new PostNotFoundException("Post not found: " + postId));
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
}