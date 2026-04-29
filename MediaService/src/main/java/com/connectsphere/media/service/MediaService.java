package com.connectsphere.media.service;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.connectsphere.media.entity.Media;
import com.connectsphere.media.entity.Story;

public interface MediaService {
	Media uploadMedia(MultipartFile file, int uploaderId, int linkedPostId);

	List<Media> getMediaByPost(int postId);

	Media getMediaById(int mediaId);

	void deleteMedia(int mediaId);

	void deleteMediaByPost(int postId);

	Story createStory(MultipartFile file, int authorId, String caption);

	List<Story> getActiveStories(List<Integer> authorIds);

	Story viewStory(int storyId);

	void deleteStory(int storyId);

	List<Story> getStoriesByUser(int userId);

	int expireOldStories();
}
