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

import com.connectsphere.media.clients.PostClient;
import com.connectsphere.media.dto.PostDTO;
import com.connectsphere.media.dto.UploadResult;
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
	private final StorageService storageService;

	private final PostClient postClient;

	@Value("${cloudinary.folder.media:connectsphere/media}")
	private String mediaFolder;

	@Value("${cloudinary.folder.stories:connectsphere/stories}")
	private String storiesFolder;

	private static final Set<String> ALLOWED_MIME_TYPES = Set.of("image/jpeg", "image/png", "image/webp", "video/mp4");

	@Override
	public Media uploadMedia(MultipartFile file, int uploaderId, int linkedPostId) {
		validateFile(file);
		if (linkedPostId != 0) {
			PostDTO post = postClient.getPostById(linkedPostId);

			if (post == null) {
				throw new ResourceNotFoundException("Post not found: " + linkedPostId);
			}

			if (post.getAuthorId() != uploaderId) {
				throw new RuntimeException("You are not allowed to attach media to this post");
			}
		}

		String extension = getExtension(file.getOriginalFilename());
		String fileName = uploaderId + "/" + UUID.randomUUID() + "." + extension;
		String mimeType = file.getContentType();
		boolean isVideo = mimeType != null && mimeType.startsWith("VIDEO");
		String resourceType = isVideo ? "video" : "image";

		String folder = mediaFolder + "/" + uploaderId;
		UploadResult result = storageService.upload(file, folder, resourceType);

		Media media = Media.builder().uploaderId(uploaderId).url(result.getUrl()).filePath(result.getPublicId())
				.mediaType(isVideo ? Media.TYPE_VIDEO : Media.TYPE_IMAGE).sizeKb(result.getBytes() / 1024)
				.mimeType(mimeType).linkedPostId(linkedPostId).originalFileName(file.getOriginalFilename())
				.deleteStatus(false).build();

		Media saved = mediaRepository.save(media);

		log.info("Media uploaded to Cloudinary: mediaId={}, url={}", saved.getMediaId(), saved.getUrl());

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
	public void deleteMedia(int mediaId, int userId) {
		Media media = mediaRepository.findByMediaIdAndDeleteStatusFalse(mediaId)
				.orElseThrow(() -> new ResourceNotFoundException("Media Not Found: " + mediaId));
		if (media.getUploaderId() != userId) {
			throw new InvalidMediaException("You are not allowed to delete this media");
		}
		String resourceType = Media.TYPE_VIDEO.equals(media.getMediaType()) ? "video" : "image";
		storageService.delete(media.getFilePath(), resourceType);
		mediaRepository.softDeleteByMediaId(mediaId);
	}

	@Override
	public void deleteMediaByPost(int postId, int userId) {
		List<Media> mediaList = mediaRepository.findByLinkedPostIdAndDeleteStatusFalse(postId);
		PostDTO post;
		try {
			post = postClient.getPostById(postId);
		} catch (Exception e) {
			throw new ResourceNotFoundException("Post not found: " + postId);
		}
		if (post.getAuthorId() != userId) {
			throw new InvalidMediaException("Not allowed to delete media of this post");
		}
		for (Media media : mediaList) {
			String resourceType = Media.TYPE_VIDEO.equals(media.getMediaType()) ? "video" : "image";
			storageService.delete(media.getFilePath(), resourceType);
		}

		mediaRepository.softDeleteByLinkedPostId(postId);
		log.info("All media deleted from Cloudinary for postId={}", postId);
	}

	@Override
	public Story createStory(MultipartFile file, int authorId, String caption) {
		validateFile(file);
		String mimeType = file.getContentType();
		boolean isVideo = mimeType != null && mimeType.startsWith("video");
		String resourceType = isVideo ? "video" : "image";
		String folder = storiesFolder + "/" + authorId;
		UploadResult result = storageService.upload(file, folder, resourceType);

		Story story = Story.builder().authorId(authorId).mediaUrl(result.getUrl()).mediaPublicId(result.getPublicId())
				.caption(caption).mediaType(isVideo ? Media.TYPE_VIDEO : Media.TYPE_IMAGE).viewCount(0)
				.expiresAt(LocalDateTime.now().plusHours(24)).activeStatus(true).build();

		Story saved = storyRepository.save(story);
		log.info("Story created: storyId={}, url={}", saved.getStoryId(), saved.getMediaUrl());
		return saved;

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
	public void deleteStory(int storyId, int userId) {
		Story story = storyRepository.findByStoryIdAndActiveStatusTrue(storyId)
				.orElseThrow(() -> new ResourceNotFoundException("Story not found: " + storyId));
		if (story.getAuthorId() != userId) {
			throw new InvalidMediaException("You are not allowed to delete this story");
		}
		if (story.getMediaPublicId() != null) {
			String resourceType = Media.TYPE_VIDEO.equals(story.getMediaType()) ? "video" : "image";
			storageService.delete(story.getMediaPublicId(), resourceType);
		}

		story.setActiveStatus(false);
		storyRepository.save(story);
		log.info("Story deleted: storyId={}", storyId);
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
