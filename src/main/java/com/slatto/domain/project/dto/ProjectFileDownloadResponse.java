package com.slatto.domain.project.dto;

import lombok.Builder;
import lombok.Getter;

import java.io.InputStream;

@Getter
@Builder
public class ProjectFileDownloadResponse {

    private String fileName;

    private String contentType;

    private Long fileSize;

    private InputStream inputStream;
}
