package com.connectsphere.gateway.service;

import java.util.List;

import com.connectsphere.gateway.entity.Follows;

public interface FollowService {
	Follows follow(int followerId, int followeeId);

	void unfollow(int followeId, int followeeId);

	boolean isFollowing(int followerId, int followeeId);

	List<Follows> getFollowers(int userId);

	List<Follows> getFollowing(int userId);

	long getFollowerCount(int userId);

	long getFollowingCount(int userId);

	List<Integer> getMutualFollows(int userId);

	List<Integer> getSuggestedUsers(int userId);

	List<Integer> getFolloweeIds(int userId);
}
