package com.connectsphere.media.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.connectsphere.media.entity.Story;

@Repository
public interface StoryRepository extends JpaRepository<Story, Integer> {
	Optional<Story> findByStoryIdAndActiveStatusTrue(int storyId);

	List<Story> findByAuthorIdAndActiveStatusTrueOrderByCreatedAtDesc(int authorId);

	@Query("Select s FROM Story s WHERE s.authorId IN :authorIds AND s.activeStatus = true ORDER BY s.createdAt Desc")
	List<Story> findActiveByAuthorIds(@Param("authorIds") List<Integer> authorIds);

	@Query("Select s FROM Story s WHERE s.expiresAt < :now AND s.activeStatus = true")
	List<Story> findExpiredStories(@Param("now") LocalDateTime now);

	@Modifying
	@Query("UPDATE Story s SET s.viewCount = s.viewCount + 1 WHERE s.storyId = :storyId")
	void incrementViewsCount(@Param("storyId") int storyId);

	@Modifying
	@Query("UPDATE Story s SET s.activeStatus = false WHERE s.expiresAt < :now AND s.activeStatus = true")
	int deactivateExpiredStories(@Param("now") LocalDateTime now);
}
