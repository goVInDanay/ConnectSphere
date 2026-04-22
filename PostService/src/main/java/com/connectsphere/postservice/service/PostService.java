package com.connectsphere.postservice.service;

import java.util.List;
import java.util.Optional;

import com.connectsphere.postservice.dto.CreatePostRequest;
import com.connectsphere.postservice.dto.UpdatePostRequest;
import com.connectsphere.postservice.entity.Post;

public interface PostService {

	Post createPost(CreatePostRequest request);

	Optional<Post> getPostById(int postId);

	List<Post> getPostsByUser(int userId);

	List<Post> getFeedForUser(int userId, List<Integer> followeeIds);

	Post updatePost(int postId, UpdatePostRequest request);

	void deletePost(int postId);

	List<Post> searchPosts(String term);

	void incrementLikes(int postId);

	void decrementLikes(int postId);

	void incrementComments(int postId);

	void decrementComments(int postId);

	void changeVisibility(int postId, String visibility);

	int getPostCount(int userId);
}