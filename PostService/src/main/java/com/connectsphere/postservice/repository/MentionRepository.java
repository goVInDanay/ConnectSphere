package com.connectsphere.postservice.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.connectsphere.postservice.entity.Mention;

@Repository
public interface MentionRepository extends JpaRepository<Mention, Integer> {
	List<Mention> findByPostId(int postId);
}