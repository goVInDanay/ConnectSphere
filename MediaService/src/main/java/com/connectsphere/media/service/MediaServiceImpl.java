package com.connectsphere.media.service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.connectsphere.media.entity.Media;
import com.connectsphere.media.entity.Story;
import com.connectsphere.media.exception.InvalidMediaException;
import com.connectsphere.media.exception.ResourceNotFoundException;
import com.connectsphere.media.repository.MediaRepository;
import com.connectsphere.media.repository.StoryRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class MediaServiceImpl implements MediaService {

	private final MediaRepository mediaRepository;
	private final StoryRepository storyRepository;

	@Value("${file.upload-dir}")
	private String uploadDir;

	@Value("${app.base-url}")
	private String baseUrl;

	private static final Set<String> ALLOWED_MIME_TYPES = Set.of("image/jpeg", "image/png", "image/webp", "video/mp4");

	@Override
	public Media uploadMedia(MultipartFile file, int uploaderId, int linkedPostId) {
		validateFile(file);

		String extension = getExtension(file.getOriginalFilename());
		String fileName = uploaderId + "/" + UUID.randomUUID() + "." + extension;
		String mimeType = file.getContentType();

		Path filePath = Paths.get(uploadDir, fileName);
		String fileUrl = baseUrl + "/uploads/" + fileName;

		try {
			Files.createDirectories(filePath.getParent());
			Files.write(filePath, file.getBytes());
			log.info("File stored locally: {}", filePath);
		} catch (Exception e) {
			throw new RuntimeException("Local file storage failed " + e.getMessage(), e);
		}

		Media media = Media.builder().uploaderId(uploaderId).url(fileUrl).filePath(fileName)
				.mediaType(mimeType.startsWith("video") ? Media.TYPE_VIDEO : Media.TYPE_IMAGE)
				.sizeKb(file.getSize() / 1024).mimeType(mimeType).linkedPostId(linkedPostId)
				.originalFileName(file.getOriginalFilename()).deleteStatus(false).build();

		Media saved = mediaRepository.save(media);

		log.info("Media persisted: mediaId={}, url={}", saved.getMediaId(), saved.getUrl());

		return saved;
	}

	@Override
	@Transactional(readOnly = true)
	public List<Media> getMediaByPost(int postId) {
		return mediaRepository.findByLinkedPostIdAndDeleteStatusFalse(postId);
	}

	@Override
	@Transactional(readOnly = true)
	public Media getMediaById(int mediaId) {
		return mediaRepository.findByMediaIdAndDeleteStatusFalse(mediaId)
				.orElseThrow(() -> new ResourceNotFoundException("Media Not Found: " + mediaId));
	}

	@Override
	public void deleteMedia(int mediaId) {
		Media media = mediaRepository.findByMediaIdAndDeleteStatusFalse(mediaId)
				.orElseThrow(() -> new ResourceNotFoundException("Media Not Found: " + mediaId));
		try {
			Path filePath = Paths.get(uploadDir, media.getFilePath());
			Files.deleteIfExists(filePath);
			log.info("File deleted: {}", filePath);
		} catch (Exception e) {
			log.warn("Failed to delete file: {}", media.getFilePath());
		}
		mediaRepository.softDeleteByMediaId(mediaId);
	}

	@Override
	public void deleteMediaByPost(int postId) {
		List<Media> mediaList = mediaRepository.findByLinkedPostIdAndDeleteStatusFalse(postId);
		for (Media media : mediaList) {
			try {
				Path filePath = Paths.get(uploadDir, media.getFilePath());
				Files.deleteIfExists(filePath);
			} catch (Exception e) {
				log.warn("Failed to delete file: {}", media.getFilePath());
			}
		}

		mediaRepository.softDeleteByLinkedPostId(postId);
	}

	@Override
	public Story createStory(MultipartFile file, int authorId, String caption) {
		validateFile(file);
		Media uploaded = uploadMedia(file, authorId, 0);
		LocalDateTime now = LocalDateTime.now();
		Story story = Story.builder().authorId(authorId).mediaUrl(uploaded.getUrl()).caption(caption)
				.mediaType(uploaded.getMediaType()).viewCount(0).expiresAt(now.plusHours(24)).activeStatus(true)
				.build();

		return storyRepository.save(story);
	}

	@Override
	@Transactional(readOnly = true)
	public List<Story> getActiveStories(List<Integer> authorIds) {
		if (authorIds == null || authorIds.isEmpty()) {
			return List.of();
		}

		return storyRepository.findActiveByAuthorIds(authorIds);
	}

	@Override
	public Story viewStory(int storyId) {
		Story story = storyRepository.findByStoryIdAndActiveStatusTrue(storyId)
				.orElseThrow(() -> new ResourceNotFoundException("Story not found: " + storyId));

		storyRepository.incrementViewsCount(storyId);
		return story;
	}

	@Override
	public void deleteStory(int storyId) {
		Story story = storyRepository.findByStoryIdAndActiveStatusTrue(storyId)
				.orElseThrow(() -> new ResourceNotFoundException("Story not found: " + storyId));
		story.setActiveStatus(false);
		storyRepository.save(story);
	}

	@Override
	@Transactional(readOnly = true)
	public List<Story> getStoriesByUser(int userId) {
		return storyRepository.findByAuthorIdAndActiveStatusTrueOrderByCreatedAtDesc(userId);
	}

	@Override
	@Scheduled(fixedDelay = 300000)
	public int expireOldStories() {
		int count = storyRepository.deactivateExpiredStories(LocalDateTime.now());
		if (count > 0) {
			log.info("Expired stories count: {}", count);
		}
		return count;
	}

	private void validateFile(MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new InvalidMediaException("File must not be empty");
		}

		String mime = file.getContentType();
		if (mime == null || !ALLOWED_MIME_TYPES.contains(mime)) {
			throw new InvalidMediaException("Unsupported file type: " + mime);
		}

		boolean isVideo = mime.startsWith("video");

		long sizeKb = file.getSize() / 1024;
		long limit = isVideo ? Media.MAX_VIDEO_SIZE_KB : Media.MAX_IMAGE_SIZE_KB;

		if (sizeKb > limit) {
			throw new InvalidMediaException("File too large");
		}
	}

	private String getExtension(String fileName) {
		if (fileName == null || !fileName.contains(".")) {
			return "bin";
		}
		return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
	}
}
