package com.connectsphere.postservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.connectsphere.postservice.clients.FollowClient;
import com.connectsphere.postservice.clients.SearchClient;
import com.connectsphere.postservice.clients.UserClient;
import com.connectsphere.postservice.dto.CreatePostRequest;
import com.connectsphere.postservice.dto.UpdatePostRequest;
import com.connectsphere.postservice.entity.Post;
import com.connectsphere.postservice.messaging.NotificationProducer;
import com.connectsphere.postservice.repository.PostRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("PostService Unit Tests")
class PostServiceImplTest {

	@Mock
	PostRepository postRepository;

	@Mock
	FollowClient followClient;

	@Mock
	SearchClient searchClient;

	@Mock
	UserClient userClient;

	@Mock
	NotificationProducer notificationProducer;

	@Mock
	FeedCacheService feedCacheService;

	@InjectMocks
	PostServiceImpl postService;

	private Post storedPost;
	private CreatePostRequest createRequest;

	@BeforeEach
	void setUp() {
		storedPost = Post.builder().postId(1).authorId(42).content("Hello world!").visibility("PUBLIC").likesCount(0)
				.commentsCount(0).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();

		createRequest = new CreatePostRequest();
		createRequest.setAuthorId(42);
		createRequest.setContent("Hello world!");
		createRequest.setVisibility("PUBLIC");
		createRequest.setMediaUrls(List.of());
	}

	@Nested
	class CreatePost {

		@Test
		void savesAndReturns() {
			when(postRepository.save(any(Post.class))).thenReturn(storedPost);
			when(followClient.getFollowerIds(42)).thenReturn(List.of());

			Post result = postService.createPost(createRequest);

			assertThat(result).isNotNull();
			assertThat(result.getContent()).isEqualTo("Hello world!");
			assertThat(result.getAuthorId()).isEqualTo(42);

			verify(postRepository).save(any(Post.class));
		}

		@Test
		void setsInitialCountsToZero() {
			when(postRepository.save(any(Post.class))).thenReturn(storedPost);
			when(followClient.getFollowerIds(42)).thenReturn(List.of());

			Post result = postService.createPost(createRequest);

			assertThat(result.getLikesCount()).isZero();
			assertThat(result.getCommentsCount()).isZero();
		}
	}

	@Nested
	class GetPostById {

		@Test
		void found() {
			when(postRepository.findByPostIdAndIsDeletedFalse(1)).thenReturn(Optional.of(storedPost));

			Optional<Post> result = postService.getPostById(1);

			assertThat(result).isPresent();
			assertThat(result.get().getPostId()).isEqualTo(1);
		}

		@Test
		void notFound() {
			when(postRepository.findByPostIdAndIsDeletedFalse(999)).thenReturn(Optional.empty());

			Optional<Post> result = postService.getPostById(999);

			assertThat(result).isEmpty();
		}
	}

	@Nested
	class UpdatePost {

		@Test
		void ownerUpdatesSuccess() {
			when(postRepository.findByPostIdAndIsDeletedFalse(1)).thenReturn(Optional.of(storedPost));
			when(postRepository.save(any(Post.class))).thenAnswer(i -> i.getArgument(0));

			UpdatePostRequest req = new UpdatePostRequest();
			req.setContent("Updated content");

			Post result = postService.updatePost(1, req, 42);

			assertThat(result.getContent()).isEqualTo("Updated content");
			verify(postRepository).save(any(Post.class));
		}

		@Test
		void nonOwnerThrows() {
			when(postRepository.findByPostIdAndIsDeletedFalse(1)).thenReturn(Optional.of(storedPost));

			UpdatePostRequest req = new UpdatePostRequest();
			req.setContent("hack");

			assertThatThrownBy(() -> postService.updatePost(1, req, 99));
			verify(postRepository, never()).save(any());
		}

		@Test
		void postNotFound() {
			when(postRepository.findByPostIdAndIsDeletedFalse(999)).thenReturn(Optional.empty());

			UpdatePostRequest req = new UpdatePostRequest();
			req.setContent("ghost");

			assertThatThrownBy(() -> postService.updatePost(999, req, 42));
		}
	}

	@Nested
	class DeletePost {

		@Test
		void ownerDeletes() {
			when(postRepository.findByPostIdAndIsDeletedFalse(1)).thenReturn(Optional.of(storedPost));

			postService.deletePost(1, 42);

			verify(postRepository).softDeleteByPostId(1);
		}

		@Test
		void nonOwnerThrows() {
			when(postRepository.findByPostIdAndIsDeletedFalse(1)).thenReturn(Optional.of(storedPost));

			assertThatThrownBy(() -> postService.deletePost(1, 99));
			verify(postRepository, never()).softDeleteByPostId(1);
		}
	}

	@Test
	void getPostCount() {
		when(postRepository.countByAuthorIdAndIsDeletedFalse(42)).thenReturn(7);

		int count = postService.getPostCount(42);

		assertThat(count).isEqualTo(7);
	}

	@Test
	void getFlaggedPosts() {
		storedPost.setFlagged(true);
		when(postRepository.findByIsFlaggedTrueAndIsDeletedFalse()).thenReturn(List.of(storedPost));

		List<Post> flagged = postService.getFlaggedPosts();

		assertThat(flagged).hasSize(1);
		assertThat(flagged.get(0).isFlagged()).isTrue();
	}
}