package com.slatto.domain.recruitment.dto;

import lombok.Builder;
import lombok.Getter;

import java.io.InputStream;

@Getter
@Builder
public class RecruitmentApplicationFileDownloadResponse {

    private String fileName;
    private String contentType;
    private Long fileSize;
    private InputStream inputStream;
}
