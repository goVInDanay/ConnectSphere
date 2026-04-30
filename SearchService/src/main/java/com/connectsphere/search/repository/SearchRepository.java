package com.connectsphere.search.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.connectsphere.search.entity.Hashtag;
import com.connectsphere.search.entity.PostHashtag;

@Repository
public interface SearchRepository extends JpaRepository<PostHashtag, Integer> {

	@Query("SELECT h FROM Hashtag h ORDER BY h.postCount DESC, h.lastUsedAt DESC")
	List<Hashtag> findTrendingHashtags(Pageable pageable);

	@Query("SELECT h FROM Hashtag h WHERE LOWER(h.tag) LIKE LOWER(CONCAT('%', :term, '%')) ORDER BY h.postCount DESC")
	List<Hashtag> searchByTagContaining(@Param("term") String term);

	@Query("SELECT ph.postId FROM PostHashtag ph WHERE ph.hashtag.tag = :tag ORDER BY ph.createdAt DESC")
	List<Integer> findPostIdsByHashtag(@Param("tag") String tag);

	@Query("SELECT ph.hashtag FROM PostHashtag ph WHERE ph.postId = :postId")
	List<Hashtag> findHashtagsByPostId(@Param("postId") int postId);

	List<PostHashtag> findByPostId(int postId);

	@Query("SELECT COUNT(ph) FROM PostHashtag ph WHERE ph.hashtag.tag = :tag")
	int countPostsByHashtag(@Param("tag") String tag);

	boolean existsByPostIdAndHashtagTag(int postId, String tag);
}
