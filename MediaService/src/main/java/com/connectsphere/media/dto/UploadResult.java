package com.connectsphere.media.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UploadResult {
	private String publicId;
	private String url;
	private String format;
	private long bytes;
	private int width;
	private int height;
}
