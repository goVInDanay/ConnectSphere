package com.connectsphere.follow.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.connectsphere.follow.entity.Follows;
import com.connectsphere.follow.service.FollowService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/follows")
@RequiredArgsConstructor
public class FollowController {
	private final FollowService followService;

	@PostMapping("/{followeeId}")
	public ResponseEntity<Follows> follow(@AuthenticationPrincipal Integer userId, @PathVariable int followeeId) {
		Follows follow = followService.follow(userId, followeeId);
		return ResponseEntity.status(HttpStatus.CREATED).body(follow);
	}

	@DeleteMapping("/{followeeId}")
	public ResponseEntity<Map<String, String>> unfollow(@AuthenticationPrincipal Integer userId,
			@PathVariable int followeeId) {
		followService.unfollow(userId, followeeId);
		return ResponseEntity.ok(Map.of("message", "Unfollowed successfully"));
	}

	@GetMapping("/{userId}/followers")
	public ResponseEntity<List<Follows>> getFollowers(@PathVariable Integer userId) {
		return ResponseEntity.ok(followService.getFollowers(userId));
	}

	@GetMapping("/{userId}/following")
	public ResponseEntity<List<Follows>> getFollowing(@PathVariable Integer userId) {
		return ResponseEntity.ok(followService.getFollowing(userId));
	}

	@GetMapping("/{userId}/follower-count")
	public ResponseEntity<Map<String, Long>> getFollowerCount(@PathVariable Integer userId) {
		return ResponseEntity.ok(Map.of("followerCount", followService.getFollowerCount(userId)));
	}

	@GetMapping("/{userId}/following-count")
	public ResponseEntity<Map<String, Long>> getFollowingCount(@PathVariable Integer userId) {
		return ResponseEntity.ok(Map.of("followingCount", followService.getFollowingCount(userId)));
	}

	@GetMapping("/is-following/{followeeId}")
	public ResponseEntity<Map<String, Boolean>> isFollowing(@AuthenticationPrincipal Integer userId,
			@PathVariable int followeeId) {
		return ResponseEntity.ok(Map.of("isFollowing", followService.isFollowing(userId, followeeId)));
	}

	@GetMapping("/is-following")
	public ResponseEntity<Boolean> isFollowingInternal(@RequestParam Integer followerId,
			@RequestParam Integer followeeId) {
		return ResponseEntity.ok(followService.isFollowing(followerId, followeeId));
	}

	@GetMapping("/mutual")
	public ResponseEntity<Map<String, List<Integer>>> getMutualFollows(@AuthenticationPrincipal Integer userId) {
		return ResponseEntity.ok(Map.of("mutualFollowIds", followService.getMutualFollows(userId)));
	}

	@GetMapping("/suggested")
	public ResponseEntity<Map<String, List<Integer>>> getSuggestedUsers(@AuthenticationPrincipal Integer userId) {
		return ResponseEntity.ok(Map.of("suggestedUserIds", followService.getSuggestedUsers(userId)));
	}

	@GetMapping("/followee-ids")
	public ResponseEntity<Map<String, List<Integer>>> getFolloweeIds(@AuthenticationPrincipal Integer userId) {
		return ResponseEntity.ok(Map.of("followeeIds", followService.getFolloweeIds(userId)));
	}

	@GetMapping("/{userId}/follower-ids")
	public ResponseEntity<List<Integer>> getFollowerIds(@PathVariable int userId) {
		return ResponseEntity.ok(followService.getFollowerIds(userId));
	}
}
