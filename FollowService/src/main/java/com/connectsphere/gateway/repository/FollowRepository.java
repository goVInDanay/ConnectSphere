package com.connectsphere.gateway.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.connectsphere.gateway.entity.Follows;
import java.util.List;

@Repository
public interface FollowRepository extends JpaRepository<Follows, Integer> {
	Optional<Follows> findByFollowerIdAndFolloweeId(int followerId, int followeeId);

	boolean existsByFollowerIdAndFolloweeId(int followerId, int followeeId);

	void deleteByFollowerIdAndFolloweeId(int followerId, int followeeId);

	List<Follows> findByFollowerId(int followerId);

	List<Follows> findByFolloweeId(int followeeId);

	List<Follows> findByFollowerIdAndStatus(int followerId, String status);

	List<Follows> findByFolloweeIdAndStatus(int followeeId, String status);

	long countByFolloweeIdAndStatus(int followeeId, String status);

	long countByFollowerIdAndStatus(int followerId, String status);

	@Query("Select f.followeeId FROM Follows f WHERE f.followerId = :followerId AND f.status = 'ACTIVE'")
	List<Integer> findFolloweeIds(@Param("followerId") int followerId);

	@Query("SELECT f1.followeeId FROM Follows f1 WHERE f1.followerId = :userId AND f1.status = 'ACTIVE' "
			+ "AND EXISTS (SELECT 1 FROM Follows f2 WHERE f2.followerId = f1.followeeId "
			+ "  AND f2.followeeId = :userId AND f2.status = 'ACTIVE')")
	List<Integer> findMutualFollowIds(@Param("userId") int userId);

	@Query(value = "SELECT DISTINCT f2.followee_id FROM follows f1 "
			+ "JOIN follows f2 ON f1.followee_id = f2.follower_id WHERE f1.follower_id = :userId "
			+ "  AND f2.followee_id != :userId AND f1.status = 'ACTIVE' AND f2.status = 'ACTIVE' "
			+ "  AND NOT EXISTS (SELECT 1 FROM follows f3 WHERE f3.follower_id = :userId"
			+ " AND f3.followee_id = f2.followee_id) LIMIT 50", nativeQuery = true)
	List<Integer> findSecondDegreeConnections(@Param("userId") int userId);
}
