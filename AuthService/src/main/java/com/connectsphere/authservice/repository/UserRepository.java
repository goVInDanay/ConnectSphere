package com.connectsphere.authservice.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.connectsphere.authservice.entities.User;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
	Optional<User> findByEmail(String email);

	Optional<User> findByUsername(String username);

	Optional<User> findByUserId(Integer userId);

	Boolean existsByEmail(String email);

	Boolean existsByUsername(String username);

	@Query("SELECT u FROM User u WHERE LOWER(u.username) LIKE LOWER(CONCAT('%', :term, '%')) AND u.isActive = true")
	List<User> searchByUsername(@Param("term") String term);

	@Query("SELECT u FROM User u WHERE " + "(LOWER(u.username) LIKE LOWER(CONCAT('%', :term, '%')) "
			+ "OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :term, '%'))) " + "AND u.isActive = true")
	List<User> searchUsers(@Param("term") String term);

	void deleteByUserId(int userId);

	Optional<User> findByProviderAndProviderId(String provider, String providerId);

	List<User> findAllByRole(String role);
}
