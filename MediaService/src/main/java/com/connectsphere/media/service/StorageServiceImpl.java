package com.connectsphere.media.service;

import java.io.IOException;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.connectsphere.media.dto.UploadResult;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class StorageServiceImpl implements StorageService {

	private final Cloudinary cloudinary;

	@Override
	public UploadResult upload(MultipartFile file, String folder, String resourceType) {
		try {
			Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(),
					ObjectUtils.asMap("folder", folder, "resource_type", resourceType, "quality", "auto",
							"fetch_format", "auto", "eager",
							resourceType.equals("video") ? "c_limit, h_1080, w_1920/q_auto" : null));
			String publicId = (String) result.get("public_id");
			String url = (String) result.get("secure_url");
			String format = (String) result.get("format");
			long bytes = toLong(result.get("bytes"));
			int width = toInt(result.get("width"));
			int height = toInt(result.get("height"));
			log.info("Uploaded to Cloudinary: publicId={}, url={}, bytes={}", publicId, url, bytes);
			return new UploadResult(publicId, url, format, bytes, width, height);
		} catch (IOException e) {
			log.error("Cloudinary upload failed: {}", e.getMessage());
			throw new RuntimeException("Failed to upload file to Cloudinary: " + e.getMessage(), e);
		}
	}

	@Override
	public void delete(String publicId, String resourceType) {
		if (publicId == null || publicId.isBlank()) {
			log.warn("delete() called with blank publicId");
			return;
		}
		try {
			Map<?, ?> result = cloudinary.uploader().destroy(publicId,
					ObjectUtils.asMap("resource_type", resourceType));
			log.info("Deleted from cloudinary : publicId = {}, result={}", publicId, result.get("result"));
		} catch (IOException e) {
			log.error("Cloudinary delete failed for publicId={}: {}", publicId, e.getMessage());
		}
	}

	private long toLong(Object val) {
		if (val instanceof Number n)
			return n.longValue();
		return 0L;
	}

	private int toInt(Object val) {
		if (val instanceof Number n)
			return n.intValue();
		return 0;
	}

}
