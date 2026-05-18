package com.connectsphere.likeservice.controller;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.connectsphere.likeservice.config.RestTemplateConfig;
import com.connectsphere.likeservice.dto.ReactionCountResponse;
import com.connectsphere.likeservice.dto.ReactionSummaryResponse;
import com.connectsphere.likeservice.entity.Like;
import com.connectsphere.likeservice.service.LikeService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/likes")
@RequiredArgsConstructor
public class LikeController {

	private final LikeService likeService;

	@PostMapping
	public ResponseEntity<Like> likeTarget(@AuthenticationPrincipal Integer userId, @RequestParam int targetId,
			@RequestParam String targetType, @RequestParam(defaultValue = "LIKE") String reactionType) {
		Like like = likeService.likeTarget(userId, targetId, targetType, reactionType);
		return ResponseEntity.status(HttpStatus.CREATED).body(like);
	}

	@DeleteMapping
	public ResponseEntity<Map<String, String>> unlikeTarget(@AuthenticationPrincipal Integer userId,
			@RequestParam int targetId, @RequestParam String targetType) {
		likeService.unlikeTarget(userId, targetId, targetType);
		return ResponseEntity.ok(Map.of("message", "Reaction removed"));
	}

	@GetMapping("/target/{targetType}/{targetId}")
	public ResponseEntity<List<Like>> getLikesByTarget(@PathVariable String targetType,
			@PathVariable Integer targetId) {
		return ResponseEntity.ok(likeService.getLikesByTarget(targetId, targetType));
	}

	@GetMapping("/target/{targetType}/{targetId}/count")
	public ResponseEntity<Map<String, Long>> getLikeCount(@PathVariable String targetType, @PathVariable int targetId) {

		return ResponseEntity.ok(Map.of("count", likeService.getLikeCount(targetId, targetType)));
	}

	@GetMapping("/target/{targetType}/{targetId}/count-by-type")
	public ResponseEntity<ReactionCountResponse> getLikeCountByType(@PathVariable String targetType,
			@PathVariable int targetId, @RequestParam String reactionType) {

		Long count = likeService.getLikeCountByType(targetId, targetType, reactionType);
		return ResponseEntity.ok(new ReactionCountResponse(reactionType, count));
	}

	@GetMapping("/target/{targetType}/{targetId}/summary")
	public ResponseEntity<ReactionSummaryResponse> getReactionSummary(@PathVariable String targetType,
			@PathVariable int targetId) {
		return ResponseEntity.ok(new ReactionSummaryResponse(targetId, targetType,
				likeService.getReactionSummary(targetId, targetType)));
	}

	@GetMapping("/target/{targetType}/{targetId}/me")
	public ResponseEntity<Like> getUserReaction(@AuthenticationPrincipal Integer userId,
			@PathVariable String targetType, @PathVariable int targetId) {
		Optional<Like> reaction = likeService.getUserReaction(userId, targetId, targetType);
		return reaction.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
	}

	@GetMapping("/target/{targetType}/{targetId}/has-liked")
	public ResponseEntity<Map<String, Boolean>> hasLiked(@AuthenticationPrincipal Integer userId,
			@PathVariable String targetType, @PathVariable int targetId) {
		return ResponseEntity.ok(Map.of("hasLiked", likeService.hasLiked(userId, targetId, targetType)));
	}

	@GetMapping("/user/{userId}")
	public ResponseEntity<List<Like>> getLikesByUser(@PathVariable int userId) {
		return ResponseEntity.ok(likeService.getLikesByUser(userId));
	}

	@PutMapping
	public ResponseEntity<Like> changeReaction(@AuthenticationPrincipal Integer userId, @RequestParam int targetId,
			@RequestParam String targetType, @RequestParam String newReaction) {
		Like updated = likeService.changeReaction(userId, targetId, targetType, newReaction);
		return ResponseEntity.ok(updated);
	}
}
