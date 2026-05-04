package com.connectsphere.search.service;

import java.util.List;

import com.connectsphere.search.entity.Hashtag;

public interface SearchService {
	void indexPost(int postId, String content);

	void removePostIndex(int postId);

	List<Object> searchPosts(String term);

	List<Object> searchUsers(String term);

	List<Hashtag> getHashtagsForPost(int postId);

	List<Hashtag> getTrendingHashtags(int limit);

	List<Integer> getPostsByHashtag(String tag);

	List<Hashtag> searchHashtags(String term);

	int getHashtagCount(String tag);

	List<Integer> getTrendingPostIds(int limit);
}
