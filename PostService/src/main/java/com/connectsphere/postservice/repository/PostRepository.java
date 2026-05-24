package com.connectsphere.postservice.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.connectsphere.postservice.entity.Post;

@Repository
public interface PostRepository extends JpaRepository<Post, Integer> {
	Optional<Post> findByPostIdAndIsDeletedFalse(int postId);

	List<Post> findByAuthorIdAndIsDeletedFalseOrderByCreatedAtDesc(int authorId);

	List<Post> findByAuthorIdAndVisibilityAndIsDeletedFalse(int authorId, String visibility);

	int countByAuthorIdAndIsDeletedFalse(int authorId);

	@Query("SELECT p FROM Post p WHERE p.authorId= :ownerId AND p.visibility='PUBLIC' AND p.isDeleted=false ORDER BY p.createdAt DESC")
	List<Post> findPublicPosts(int ownerId);

	@Query("SELECT p FROM Post p WHERE p.authorId = :ownerId AND p.visibility IN ('PUBLIC', 'FOLLOWERS') AND p.isDeleted = false ORDER BY p.createdAt DESC")
	List<Post> findVisibleToFollowers(int ownerId);

	List<Post> findByVisibilityAndIsDeletedFalseOrderByCreatedAtDesc(String visibility);

	@Query("SELECT p FROM Post p " + "WHERE p.authorId IN :userIds " + "AND p.isDeleted = false "
			+ "AND p.visibility IN ('PUBLIC', 'FOLLOWERS') " + "ORDER BY p.createdAt DESC")
	List<Post> findFeedByUserIds(@Param("userIds") List<Integer> userIds);

	@Query("SELECT p FROM Post p " + "WHERE LOWER(p.content) LIKE LOWER(CONCAT('%', :term, '%')) "
			+ "AND p.isDeleted = false " + "AND p.visibility = 'PUBLIC' " + "ORDER BY p.createdAt DESC")
	List<Post> searchByContent(@Param("term") String term);

	@Modifying
	@Query("UPDATE Post p SET p.likesCount = p.likesCount + 1 WHERE p.postId = :postId")
	void incrementLikesCount(@Param("postId") int postId);

	@Modifying
	@Query("UPDATE Post p SET p.likesCount = GREATEST(p.likesCount - 1, 0) WHERE p.postId = :postId")
	void decrementLikesCount(@Param("postId") int postId);

	@Modifying
	@Query("UPDATE Post p SET p.commentsCount = p.commentsCount + 1 WHERE p.postId = :postId")
	void incrementCommentsCount(@Param("postId") int postId);

	@Modifying
	@Query("UPDATE Post p SET p.commentsCount = GREATEST(p.commentsCount - 1, 0) WHERE p.postId = :postId")
	void decrementCommentsCount(@Param("postId") int postId);

	@Modifying
	@Query("UPDATE Post p SET p.isDeleted = true WHERE p.postId = :postId")
	void softDeleteByPostId(@Param("postId") int postId);

	List<Post> findByAuthorIdOrderByCreatedAtDesc(int authorId);

	List<Post> findByIsFlaggedTrueAndIsDeletedFalse();
}