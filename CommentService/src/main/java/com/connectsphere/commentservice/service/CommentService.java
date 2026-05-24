package com.connectsphere.commentservice.service;

import java.util.List;

import com.connectsphere.commentservice.dto.CreateCommentRequest;
import com.connectsphere.commentservice.entity.Comment;

public interface CommentService {

	Comment addComment(CreateCommentRequest request);

	Comment addReply(int parentCommentId, CreateCommentRequest request);

	List<Comment> getCommentsByPost(int postId);

	Comment getCommentById(int commentId);

	List<Comment> getReplies(int parentCommentId);

	Comment updateComment(int commentId, String content);

	void deleteComment(int commentId);

	List<Comment> getCommentsByUser(int userId);

	void likeComment(int commentId);

	void unlikeComment(int commentId);

	int getCommentCount(int postId);

	List<Comment> getAllComments();

	void adminDeleteComment(int commentId);

	List<Comment> getFlaggedComments();

	void approveComment(int commentId);

	void rejectComment(int commentId);

	public void reportComment(int commentId);
}