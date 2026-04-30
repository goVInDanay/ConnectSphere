package com.connectsphere.search.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.connectsphere.search.entity.Hashtag;

@Repository
public interface HashtagRepository extends JpaRepository<Hashtag, Integer> {

	Optional<Hashtag> findByTag(String tag);

	boolean existsByTag(String tag);

	@Modifying
	@Query("UPDATE Hashtag h SET h.postCount = h.postCount + 1, h.lastUsedAt = :now WHERE h.tag = :tag")
	void incrementPostCount(@Param("tag") String tag, @Param("now") LocalDateTime now);

	@Modifying
	@Query("UPDATE Hashtag h SET h.postCount = GREATEST(h.postCount - 1, 0) WHERE h.tag = :tag")
	void decrementPostCount(@Param("tag") String tag);
}
