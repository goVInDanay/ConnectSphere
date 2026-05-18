package com.connectsphere.commentservice.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import com.connectsphere.commentservice.dto.CreateCommentRequest;
import com.connectsphere.commentservice.entity.Comment;
import com.connectsphere.commentservice.exception.ResourceNotFoundException;
import com.connectsphere.commentservice.repository.CommentRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

	private final CommentRepository commentRepository;
	private final RestTemplate postServiceRestTemplate;

	@Value("${services.post-service.base-url:http://localhost:8082}")
	private String postServiceBaseUrl;

	@Override
	public Comment addComment(CreateCommentRequest request) {
		log.info("Adding top-level comment: postId={}, authorId={}", request.getPostId(), request.getAuthorId());

		Comment comment = Comment.builder().postId(request.getPostId()).authorId(request.getAuthorId())
				.content(request.getContent()).parentCommentId(null)
				.likesCount(0).isDeleted(false).build();

		Comment saved = commentRepository.save(comment);
		log.info("Comment saved: commentId={}", saved.getCommentId());
		incrementPostCommentCount(request.getPostId());

		return saved;
	}

	@Override
	public Comment addReply(int parentCommentId, CreateCommentRequest request) {
		log.info("Adding reply to commentId={}, authorId={}", parentCommentId, request.getAuthorId());
		Comment parent = requireComment(parentCommentId);
		if (parent.isReply()) {
			throw new IllegalArgumentException(
					"Replies to replies are not allowed. ConnectSphere supports two-level threading only. "
							+ "Target commentId=" + parentCommentId + " is itself a reply.");
		}

		Comment reply = Comment.builder().postId(parent.getPostId()).authorId(request.getAuthorId())
				.content(request.getContent()).parentCommentId(parentCommentId).likesCount(0).isDeleted(false).build();

		Comment saved = commentRepository.save(reply);
		log.info("Reply saved: commentId={}, parentCommentId={}", saved.getCommentId(), parentCommentId);

		incrementPostCommentCount(parent.getPostId());

		return saved;
	}

	@Override
	@Transactional(readOnly = true)
	public List<Comment> getCommentsByPost(int postId) {
		return commentRepository.findTopLevelByPostId(postId);
	}

	@Override
	@Transactional(readOnly = true)
	public Comment getCommentById(int commentId) {
		return requireComment(commentId);
	}

	@Override
	@Transactional(readOnly = true)
	public List<Comment> getReplies(int parentCommentId) {
		requireComment(parentCommentId);
		return commentRepository.findByParentCommentIdAndIsDeletedFalseOrderByCreatedAtAsc(parentCommentId);
	}

	@Override
	@Transactional(readOnly = true)
	public List<Comment> getCommentsByUser(int userId) {
		return commentRepository.findByAuthorIdAndIsDeletedFalseOrderByCreatedAtDesc(userId);
	}

	@Override
	public Comment updateComment(int commentId, String content) {
		Comment comment = requireComment(commentId);
		comment.setContent(content);
		Comment updated = commentRepository.save(comment);
		log.info("Comment updated: commentId={}", commentId);
		return updated;
	}

	@Override
	public void deleteComment(int commentId) {
		Comment comment = requireComment(commentId);

		commentRepository.softDeleteByCommentId(commentId);
		log.info("Comment soft-deleted: commentId={}", commentId);

		int replyCount = 0;
		if (!comment.isReply()) {
			replyCount = commentRepository.countByParentCommentIdAndIsDeletedFalse(commentId);
			if (replyCount > 0) {
				commentRepository.softDeleteRepliesByParentCommentId(commentId);
				log.info("Cascaded soft-delete to {} replies of commentId={}", replyCount, commentId);
			}
		}

		int totalDeleted = 1 + replyCount;
		for (int i = 0; i < totalDeleted; i++) {
			decrementPostCommentCount(comment.getPostId());
		}
	}

	@Override
	public void likeComment(int commentId) {
		requireComment(commentId);
		commentRepository.incrementLikesCount(commentId);
		log.debug("likesCount incremented for commentId={}", commentId);
	}

	@Override
	public void unlikeComment(int commentId) {
		requireComment(commentId);
		commentRepository.decrementLikesCount(commentId);
		log.debug("likesCount decremented for commentId={}", commentId);
	}

	@Override
	@Transactional(readOnly = true)
	public int getCommentCount(int postId) {
		return commentRepository.countByPostIdAndIsDeletedFalse(postId);
	}

	private Comment requireComment(int commentId) {
		return commentRepository.findByCommentIdAndIsDeletedFalse(commentId).orElseThrow(
				() -> new ResourceNotFoundException("Comment not found or has been deleted: commentId=" + commentId));
	}

	private void incrementPostCommentCount(int postId) {
		try {
			String url = postServiceBaseUrl + "/api/posts/" + postId + "/comments/inc";
			postServiceRestTemplate.postForEntity(url, null, Void.class);
			log.debug("Post-Service commentsCount incremented for postId={}", postId);
		} catch (Exception ex) {
			log.error("Failed to increment commentsCount for postId={}: {}", postId, ex.getMessage());
		}
	}

	private void decrementPostCommentCount(int postId) {
		try {
			String url = postServiceBaseUrl + "/api/posts/" + postId + "/comments/dec";
			postServiceRestTemplate.postForEntity(url, null, Void.class);
			log.debug("Post-Service commentsCount decremented for postId={}", postId);
		} catch (Exception ex) {
			log.error("Failed to decrement commentsCount for postId={}: {}", postId, ex.getMessage());
		}
	}
}