package com.connectsphere.commentservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.connectsphere.commentservice.client.PostServiceClient;
import com.connectsphere.commentservice.dto.CreateCommentRequest;
import com.connectsphere.commentservice.entity.Comment;
import com.connectsphere.commentservice.messaging.NotificationProducer;
import com.connectsphere.commentservice.repository.CommentRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("CommentService Unit Tests")
class CommentServiceImplTest {

	@Mock
	CommentRepository commentRepository;

	@Mock
	PostServiceClient postServiceClient;

	@Mock
	NotificationProducer notificationProducer;

	@InjectMocks
	CommentServiceImpl commentService;

	private Comment storedComment;
	private CreateCommentRequest createRequest;

	@BeforeEach
	void setUp() {
		storedComment = Comment.builder().commentId(1).postId(10).authorId(42).content("Great post!").likesCount(0)
				.isDeleted(false).build();

		createRequest = new CreateCommentRequest();
		createRequest.setPostId(10);
		createRequest.setAuthorId(42);
		createRequest.setContent("Great post!");
	}

	@Test
	void addComment_success() {
		when(commentRepository.save(any(Comment.class))).thenReturn(storedComment);
		when(postServiceClient.getPostOwnerId(10)).thenReturn(100);

		Comment result = commentService.addComment(createRequest);

		assertThat(result).isNotNull();
		verify(commentRepository).save(any(Comment.class));
		verify(postServiceClient).incrementCommentCount(10);
	}

	@Test
	void addReply_success() {
		when(commentRepository.findByCommentIdAndIsDeletedFalse(1)).thenReturn(Optional.of(storedComment));

		when(commentRepository.save(any(Comment.class))).thenReturn(storedComment);

		Comment result = commentService.addReply(1, createRequest);

		assertThat(result).isNotNull();
		verify(commentRepository).save(any(Comment.class));
	}

	@Test
	void addReply_parentNotFound() {
		when(commentRepository.findByCommentIdAndIsDeletedFalse(1)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> commentService.addReply(1, createRequest)).isInstanceOf(RuntimeException.class);
	}

	@Test
	void getCommentById_found() {
		when(commentRepository.findByCommentIdAndIsDeletedFalse(1)).thenReturn(Optional.of(storedComment));

		Comment result = commentService.getCommentById(1);

		assertThat(result.getCommentId()).isEqualTo(1);
	}

	@Test
	void getCommentsByPost() {
		when(commentRepository.findTopLevelByPostId(10)).thenReturn(List.of(storedComment));

		List<Comment> comments = commentService.getCommentsByPost(10);

		assertThat(comments).hasSize(1);
	}

	@Test
	void updateComment() {
		when(commentRepository.findByCommentIdAndIsDeletedFalse(1)).thenReturn(Optional.of(storedComment));

		when(commentRepository.save(any(Comment.class))).thenAnswer(i -> i.getArgument(0));

		Comment result = commentService.updateComment(1, "Updated");

		assertThat(result.getContent()).isEqualTo("Updated");
	}

	@Test
	void deleteComment() {
		when(commentRepository.findByCommentIdAndIsDeletedFalse(1)).thenReturn(Optional.of(storedComment));

		when(commentRepository.countByParentCommentIdAndIsDeletedFalse(1)).thenReturn(0);

		commentService.deleteComment(1);

		verify(commentRepository).softDeleteByCommentId(1);
	}

	@Test
	void likeComment() {
		when(commentRepository.findByCommentIdAndIsDeletedFalse(1)).thenReturn(Optional.of(storedComment));

		commentService.likeComment(1);

		verify(commentRepository).incrementLikesCount(1);
	}

	@Test
	void unlikeComment() {
		when(commentRepository.findByCommentIdAndIsDeletedFalse(1)).thenReturn(Optional.of(storedComment));

		commentService.unlikeComment(1);

		verify(commentRepository).decrementLikesCount(1);
	}

	@Test
	void getCommentCount() {
		when(commentRepository.countByPostIdAndIsDeletedFalse(10)).thenReturn(5);

		int count = commentService.getCommentCount(10);

		assertThat(count).isEqualTo(5);
	}

	@Test
	void getFlaggedComments() {
		storedComment.setFlagged(true);

		when(commentRepository.findByIsFlaggedTrueAndIsDeletedFalse()).thenReturn(List.of(storedComment));

		List<Comment> result = commentService.getFlaggedComments();

		assertThat(result).hasSize(1);
	}
}