package com.connectsphere.likeservice.service;

import com.connectsphere.likeservice.entity.Like;
import com.connectsphere.likeservice.repository.LikeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LikeService Unit Tests")
class LikeServiceImplTest {

	@Mock
	LikeRepository likeRepository;

	@InjectMocks
	LikeServiceImpl likeService;

	private Like storedLike;

	@BeforeEach
	void setUp() {
		storedLike = Like.builder().likeId(1L).userId(42).targetId(10).targetType("POST").reactionType("LIKE")
				.createdAt(LocalDateTime.now()).build();
	}

	@Test
	void likeTarget_firstLike() {
		when(likeRepository.findByUserIdAndTargetIdAndTargetType(42, 10, "POST")).thenReturn(Optional.empty());
		when(likeRepository.save(any(Like.class))).thenReturn(storedLike);

		Like result = likeService.likeTarget(42, 10, "POST", "LIKE");

		assertThat(result).isNotNull();
		assertThat(result.getReactionType()).isEqualTo("LIKE");
	}

	@Test
	void unlikeTarget_existingLike() {
		when(likeRepository.findByUserIdAndTargetIdAndTargetType(42, 10, "POST")).thenReturn(Optional.of(storedLike));

		likeService.unlikeTarget(42, 10, "POST");

		verify(likeRepository).deleteByUserIdAndTargetIdAndTargetType(42, 10, "POST");
	}

	@Test
	void getLikeCount_test() {
		when(likeRepository.countByTargetIdAndTargetType(10, "POST")).thenReturn(5L);

		long count = likeService.getLikeCount(10, "POST");

		assertThat(count).isEqualTo(5L);
	}

	@Test
	void hasLiked_true() {
		when(likeRepository.existsByUserIdAndTargetIdAndTargetType(42, 10, "POST")).thenReturn(true);
		boolean result = likeService.hasLiked(42, 10, "POST");
		assertThat(result).isTrue();
	}

	@Test
	void changeReaction_test() {
		when(likeRepository.findByUserIdAndTargetIdAndTargetType(42, 10, "POST")).thenReturn(Optional.of(storedLike));
		when(likeRepository.save(any(Like.class))).thenAnswer(i -> i.getArgument(0));

		Like result = likeService.changeReaction(42, 10, "POST", "LOVE");

		assertThat(result.getReactionType()).isEqualTo("LOVE");
	}
}
