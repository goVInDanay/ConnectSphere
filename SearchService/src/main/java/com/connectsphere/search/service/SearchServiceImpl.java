package com.connectsphere.search.service;

import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import com.connectsphere.search.clients.AuthServiceClient;
import com.connectsphere.search.clients.PostServiceClient;
import com.connectsphere.search.entity.Hashtag;
import com.connectsphere.search.entity.PostHashtag;
import com.connectsphere.search.repository.HashtagRepository;
import com.connectsphere.search.repository.SearchRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

	private final SearchRepository searchRepository;
	private final HashtagRepository hashtagRepository;
	private final PostServiceClient postServiceClient;
	private final AuthServiceClient authServiceClient;

	private static final Pattern HASHTAG_PATTERN = Pattern.compile("#([\\w\\u00C0-\\u024F]{1,100})");

	@Override
	public void indexPost(int postId, String content) {
		if (content == null || content.isBlank()) {
			return;
		}

		Set<String> tags = extractTags(content);

		LocalDateTime now = LocalDateTime.now();

		int newMappings = 0;

		for (String tag : tags) {
			if (hashtagRepository.existsByTag(tag)) {
				hashtagRepository.incrementPostCount(tag, now);
			} else {
				hashtagRepository.save(Hashtag.builder().tag(tag).postCount(1).lastUsedAt(now).build());
			}

			if (!searchRepository.existsByPostIdAndHashtagTag(postId, tag)) {
				Hashtag hashtag = hashtagRepository.findByTag(tag)
						.orElseThrow(() -> new IllegalStateException("Hashtag disappeared after upsert: " + tag));
				searchRepository.save(PostHashtag.builder().postId(postId).hashtag(hashtag).build());
				newMappings++;
			}
		}

		log.info("Indexed postId={}: {} tags ({} new mappings)", postId, tags.size(), newMappings);

	}

	@Override
	public void removePostIndex(int postId) {
		List<PostHashtag> mappings = searchRepository.findByPostId(postId);
		if (mappings.isEmpty()) {
			return;
		}

		for (PostHashtag mapping : mappings) {
			hashtagRepository.decrementPostCount(mapping.getHashtag().getTag());
		}

		searchRepository.deleteAll(mappings);

		log.info("Removed index for postId={}: {} mappings deleted", postId, mappings.size());
	}

	@Override
	@Transactional(readOnly = true)
	public List<Object> searchPosts(String term) {
		try {
			return postServiceClient.searchPosts(term);
		} catch (Exception e) {
			log.error("Post-Service search failed for term='{}':{}", term, e.getMessage());
			return List.of();
		}
	}

	@Override
	@Transactional(readOnly = true)
	public List<Object> searchUsers(String term) {
		try {
			return authServiceClient.searchUsers(term);
		} catch (Exception e) {
			log.error("Auth Service search failed for term = '{}': {}", term, e.getMessage());
			return List.of();
		}
	}

	@Override
	public List<Hashtag> getHashtagsForPost(int postId) {
		return searchRepository.findHashtagsByPostId(postId);
	}

	@Override
	public List<Hashtag> getTrendingHashtags(int limit) {
		int safeLimit = Math.min(limit, 50);
		return searchRepository.findTrendingHashtags(PageRequest.of(0, safeLimit));
	}

	@Override
	@Transactional(readOnly = true)
	public List<Integer> getPostsByHashtag(String tag) {
		return searchRepository.findPostIdsByHashtag(normaliseTag(tag));
	}

	@Override
	@Transactional(readOnly = true)
	public List<Hashtag> searchHashtags(String term) {
		return searchRepository.searchByTagContaining(normaliseTag(term));
	}

	@Override
	@Transactional(readOnly = true)
	public int getHashtagCount(String tag) {
		return hashtagRepository.findByTag(normaliseTag(tag)).map(Hashtag::getPostCount).orElse(0);
	}

	private Set<String> extractTags(String content) {
		Set<String> tags = new LinkedHashSet<>();
		Matcher m = HASHTAG_PATTERN.matcher(content);
		while (m.find()) {
			String tag = m.group(1).toLowerCase();
			if (tag.length() <= 100) {
				tags.add(tag);
			}
		}
		return tags;
	}

	private String normaliseTag(String tag) {
		if (tag == null) {
			return "";
		}
		return tag.trim().toLowerCase().replaceFirst("^#", "");
	}

	private String encode(String s) {
		try {
			return URLEncoder.encode(s, "UTF-8");
		} catch (Exception e) {
			return s;
		}
	}

}
