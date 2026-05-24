package com.connectsphere.follow.service;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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

import com.connectsphere.follow.entity.Follows;
import com.connectsphere.follow.messaging.NotificationProducer;
import com.connectsphere.follow.repository.FollowRepository;
import com.connectsphere.follow.service.FollowServiceImpl;

@ExtendWith(MockitoExtension.class)
@DisplayName("FollowService Unit Tests")
class FollowServiceImplTest {

	@Mock
	FollowRepository followRepository;

	@Mock
	NotificationProducer notificationProducer;

	@InjectMocks
	FollowServiceImpl followService;

	private Follows activeFollow;

	@BeforeEach
	void setUp() {
		activeFollow = Follows.builder().followId(1).followerId(10).followeeId(20).status("ACTIVE")
				.createdAt(LocalDateTime.now()).build();
	}

	@Nested
	class Follow {

		@Test
		void newFollow_savesSuccessfully() {
			when(followRepository.findByFollowerIdAndFolloweeId(10, 20)).thenReturn(Optional.empty());
			when(followRepository.save(any(Follows.class))).thenReturn(activeFollow);

			Follows result = followService.follow(10, 20);

			assertThat(result).isNotNull();
			assertThat(result.getFollowerId()).isEqualTo(10);
			assertThat(result.getFolloweeId()).isEqualTo(20);

			verify(notificationProducer).sendNotification(any());

			ArgumentCaptor<Follows> captor = ArgumentCaptor.forClass(Follows.class);
			verify(followRepository).save(captor.capture());
		}

		@Test
		void alreadyFollowing_returnsExisting() {
			when(followRepository.findByFollowerIdAndFolloweeId(10, 20)).thenReturn(Optional.of(activeFollow));

			Follows result = followService.follow(10, 20);

			assertThat(result).isEqualTo(activeFollow);
			verify(followRepository, never()).save(any());
		}

		@Test
		void selfFollow_throws() {
			assertThatThrownBy(() -> followService.follow(10, 10)).isInstanceOf(IllegalArgumentException.class);
		}
	}

	@Nested
	class Unfollow {

		@Test
		void existingFollow_deleted() {
			when(followRepository.findByFollowerIdAndFolloweeId(10, 20)).thenReturn(Optional.of(activeFollow));

			followService.unfollow(10, 20);

			verify(followRepository).deleteByFollowerIdAndFolloweeId(10, 20);
		}

		@Test
		void notFollowing_noop() {
			when(followRepository.findByFollowerIdAndFolloweeId(10, 99)).thenReturn(Optional.empty());

			followService.unfollow(10, 99);

			verify(followRepository, never()).deleteByFollowerIdAndFolloweeId(anyInt(), anyInt());
		}
	}

	@Nested
	class GetFollowers {

		@Test
		void hasFollowers() {
			when(followRepository.findByFolloweeIdAndStatus(20, "ACTIVE")).thenReturn(List.of(activeFollow));

			List<Follows> followers = followService.getFollowers(20);

			assertThat(followers).hasSize(1);
		}

		@Test
		void noFollowers() {
			when(followRepository.findByFolloweeIdAndStatus(99, "ACTIVE")).thenReturn(List.of());

			assertThat(followService.getFollowers(99)).isEmpty();
		}
	}

	@Nested
	class GetFollowing {

		@Test
		void hasFollowing() {
			when(followRepository.findByFollowerIdAndStatus(10, "ACTIVE")).thenReturn(List.of(activeFollow));

			List<Follows> following = followService.getFollowing(10);

			assertThat(following).hasSize(1);
		}

		@Test
		void noFollowing() {
			when(followRepository.findByFollowerIdAndStatus(99, "ACTIVE")).thenReturn(List.of());

			assertThat(followService.getFollowing(99)).isEmpty();
		}
	}

	@Nested
	class IsFollowing {

		@Test
		void activeFollow_returnsTrue() {
			when(followRepository.findByFollowerIdAndFolloweeId(10, 20)).thenReturn(Optional.of(activeFollow));

			assertThat(followService.isFollowing(10, 20)).isTrue();
		}

		@Test
		void noRecord_returnsFalse() {
			when(followRepository.findByFollowerIdAndFolloweeId(10, 99)).thenReturn(Optional.empty());

			assertThat(followService.isFollowing(10, 99)).isFalse();
		}
	}

	@Nested
	class Counts {

		@Test
		void followerCount() {
			when(followRepository.countByFolloweeIdAndStatus(20, "ACTIVE")).thenReturn(150L);

			assertThat(followService.getFollowerCount(20)).isEqualTo(150L);
		}

		@Test
		void followingCount() {
			when(followRepository.countByFollowerIdAndStatus(10, "ACTIVE")).thenReturn(75L);

			assertThat(followService.getFollowingCount(10)).isEqualTo(75L);
		}
	}

	@Test
	void getMutualFollows() {
		when(followRepository.findMutualFollowIds(10)).thenReturn(List.of(20));

		List<Integer> result = followService.getMutualFollows(10);

		assertThat(result).containsExactly(20);
	}

	@Test
	void getFolloweeIds() {
		when(followRepository.findFolloweeIds(10)).thenReturn(List.of(20, 21));

		List<Integer> ids = followService.getFolloweeIds(10);

		assertThat(ids).containsExactlyInAnyOrder(20, 21);
	}

	@Test
	void getFolloweeIds_empty() {
		when(followRepository.findFolloweeIds(99)).thenReturn(List.of());

		assertThat(followService.getFolloweeIds(99)).isEmpty();
	}

	@Test
	void getSuggestedUsers() {
		when(followRepository.findSecondDegreeConnections(10)).thenReturn(List.of(20, 30, 40));

		List<Integer> result = followService.getSuggestedUsers(10);

		assertThat(result).contains(20, 30, 40);
	}
}