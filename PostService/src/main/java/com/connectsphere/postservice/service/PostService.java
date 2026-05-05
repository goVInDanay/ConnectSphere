package com.connectsphere.postservice.service;

import java.util.List;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;

import com.connectsphere.postservice.dto.CreatePostRequest;
import com.connectsphere.postservice.dto.UpdatePostRequest;
import com.connectsphere.postservice.entity.Post;

public interface PostService {

	Post createPost(CreatePostRequest request);

	Optional<Post> getPostById(int postId);

	List<Post> getPostsByUser(int userId);

	List<Post> getFeedForUser(int userId, List<Integer> followeeIds);

	Post updatePost(int postId, UpdatePostRequest request, int userId);

	void deletePost(int postId, int userid);

	List<Post> searchPosts(String term);

	void incrementLikes(int postId);

	void decrementLikes(int postId);

	void incrementComments(int postId);

	void decrementComments(int postId);

	void changeVisibility(int postId, String visibility, int userId);

	int getPostCount(int userId);

	List<Post> getAllPosts();

	public void adminDeletePost(int postId);

	public List<Post> getVisiblePosts(int userId, Integer viewerId);

	public boolean canUserViewPost(Integer viewerId, Post post);

	public List<Post> getFlaggedPosts();

	public void approvePost(int postId);

	public void rejectPost(int postId);

	public void reportPost(int postId);
}