package com.connectsphere.media.service;

import org.springframework.web.multipart.MultipartFile;

import com.connectsphere.media.dto.UploadResult;

public interface StorageService {

	UploadResult upload(MultipartFile file, String folder, String resourceType);

	void delete(String publicId, String resourceType);
}
