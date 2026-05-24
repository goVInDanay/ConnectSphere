package com.connectsphere.media.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.connectsphere.media.entity.Media;

@Repository
public interface MediaRepository extends JpaRepository<Media, Integer> {
	Optional<Media> findByMediaIdAndDeleteStatusFalse(int mediaId);

	List<Media> findByUploaderIdAndDeleteStatusFalse(int uploaderId);

	List<Media> findByLinkedPostIdAndDeleteStatusFalse(int linkedPostId);

	List<Media> findByMediaTypeAndDeleteStatusFalse(String mediaType);

	@Modifying
	@Query("UPDATE Media m SET m.deleteStatus = true WHERE m.mediaId = :mediaId")
	void softDeleteByMediaId(@Param("mediaId") int mediaId);

	@Modifying
	@Query("UPDATE Media m SET m.deleteStatus = true WHERE m.linkedPostId = :postId")
	void softDeleteByLinkedPostId(@Param("postId") int postId);

	@Modifying
	@Query("UPDATE Media m SET m.linkedPostId = :postId WHERE m.mediaId = :mediaId AND m.uploaderId = :uploaderId")
	void linkToPost(@Param("mediaId") int mediaId, @Param("postId") int postId, @Param("uploaderId") int uploaderId);

	@Query("SELECT COALESCE(SUM(m.sizeKb), 0) FROM Media m WHERE m.uploaderId = :uploaderId AND m.deleteStatus = false")
	long sumSizeKbByUploaderId(@Param("uploaderId") int uploaderId);
}
