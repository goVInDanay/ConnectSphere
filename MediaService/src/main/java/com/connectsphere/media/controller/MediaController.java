package com.connectsphere.media.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.connectsphere.media.entity.Media;
import com.connectsphere.media.entity.Story;
import com.connectsphere.media.service.MediaService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class MediaController {
	private final MediaService mediaService;

	@PostMapping(value = "/media/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<Media> uploadMedia(@AuthenticationPrincipal Integer userId,
			@RequestPart("file") MultipartFile file, @RequestParam(defaultValue = "0") int linkedPostId) {
		Media uploaded = mediaService.uploadMedia(file, userId, linkedPostId);
		return ResponseEntity.status(HttpStatus.CREATED).body(uploaded);
	}

	@GetMapping("/post/{postId}")
	public ResponseEntity<List<Media>> getMediaByPost(@PathVariable int postId) {
		return ResponseEntity.ok(mediaService.getMediaByPost(postId));
	}

	@GetMapping("/media/{mediaId}")
	public ResponseEntity<Media> getMediaById(@PathVariable int mediaId) {
		return ResponseEntity.ok(mediaService.getMediaById(mediaId));
	}

	@DeleteMapping("/media/{mediaId}")
	public ResponseEntity<Map<String, String>> deleteMedia(@AuthenticationPrincipal int userId,
			@PathVariable int mediaId) {
		mediaService.deleteMedia(mediaId, userId);
		return ResponseEntity.ok(Map.of("message", "Media deleted"));
	}

	@DeleteMapping("/media/post/{postId}")
	public ResponseEntity<Map<String, String>> deleteMediaByPost(@AuthenticationPrincipal int userId,
			@PathVariable int postId) {
		mediaService.deleteMediaByPost(postId, userId);
		return ResponseEntity.ok(Map.of("message", "All media for post " + postId + " deleted"));
	}

	@PostMapping(value = "/stories/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<Story> createStory(@AuthenticationPrincipal Integer userId,
			@RequestPart("file") MultipartFile file,
			@RequestParam(required = false, defaultValue = "") String caption) {

		Story story = mediaService.createStory(file, userId, caption);
		return ResponseEntity.status(HttpStatus.CREATED).body(story);
	}

	@GetMapping("/stories/feed")
	public ResponseEntity<List<Story>> getActiveStories(@RequestParam List<Integer> authorIds) {
		return ResponseEntity.ok(mediaService.getActiveStories(authorIds));
	}

	@GetMapping("/stories/{storyId}")
	public ResponseEntity<Story> viewStory(@PathVariable int storyId) {
		return ResponseEntity.ok(mediaService.viewStory(storyId));
	}

	@GetMapping("/stories/user/{userId}")
	public ResponseEntity<List<Story>> getStoriesByUser(@PathVariable int userId) {
		return ResponseEntity.ok(mediaService.getStoriesByUser(userId));
	}

	@DeleteMapping("/stories/{storyId}")
	public ResponseEntity<Map<String, String>> deleteStory(@AuthenticationPrincipal int userId,
			@PathVariable int storyId) {
		mediaService.deleteStory(storyId, userId);
		return ResponseEntity.ok(Map.of("message", "Story deleted"));
	}

}
