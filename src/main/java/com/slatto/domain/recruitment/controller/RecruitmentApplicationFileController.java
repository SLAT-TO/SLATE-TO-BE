package com.slatto.domain.recruitment.controller;

import com.slatto.domain.recruitment.dto.RecruitmentApplicationFileDownloadResponse;
import com.slatto.domain.recruitment.dto.RecruitmentApplicationFileResponse;
import com.slatto.domain.recruitment.service.RecruitmentApplicationFileService;
import com.slatto.global.config.ApiErrorCodes;
import com.slatto.global.response.ApiResponse;
import com.slatto.global.response.code.CommonSuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;

@Tag(name = "Recruitment Application File", description = "구인구직 지원 첨부 파일 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/recruitments/{recruitmentId}")
public class RecruitmentApplicationFileController {

    private final RecruitmentApplicationFileService recruitmentApplicationFileService;

    @Operation(
        summary = "지원 첨부 파일 업로드",
        description = """
            파일을 하나씩 올리고 반환된 `id` 를 모아 지원 API 의 `fileIds` 로 보낸다.
            지원 API 는 마감이나 중복 지원으로 실패할 수 있어, 파일을 함께 보내면 실패할 때마다
            다시 업로드해야 하므로 단계를 나눴다.

            | 항목 | 값 |
            | --- | --- |
            | 개수 | 지원 1건당 최대 10개 |
            | 개당 용량 | 최대 100MB |
            | 허용 형식 | pdf / jpg, jpeg, png, webp / zip / mp4 |

            MIME 타입과 확장자가 모두 일치해야 통과한다. 본인이 작성한 공고에는 올릴 수 없다.

            업로드만 하고 지원하지 않으면 파일은 어느 지원에도 연결되지 않은 채 남고 조회되지 않는다.
            """
    )
    @PostMapping(path = "/application-files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<RecruitmentApplicationFileResponse> uploadApplicationFile(
        @AuthenticationPrincipal Long currentUserId,
        @PathVariable Long recruitmentId,
        @RequestPart("file") MultipartFile file
    ) {
        RecruitmentApplicationFileResponse response = recruitmentApplicationFileService.uploadApplicationFile(
            currentUserId,
            recruitmentId,
            file
        );

        return ApiResponse.success(CommonSuccessCode.CREATED, response);
    }

    @Operation(
        summary = "지원 첨부 파일 다운로드",
        description = "공고 작성자와 지원 본인만 받을 수 있다. 그 외에는 403 이다."
    )
    @ApiErrorCodes("APPLICATION403")
    @GetMapping("/applications/{applicationId}/files/{fileId}/download")
    public ResponseEntity<InputStreamResource> downloadApplicationFile(
        @AuthenticationPrincipal Long currentUserId,
        @PathVariable Long recruitmentId,
        @PathVariable Long applicationId,
        @PathVariable Long fileId
    ) {
        RecruitmentApplicationFileDownloadResponse response =
            recruitmentApplicationFileService.downloadApplicationFile(
                currentUserId,
                recruitmentId,
                applicationId,
                fileId
            );

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(response.getContentType()))
            .contentLength(response.getFileSize())
            .headers(headers -> headers.setContentDisposition(
                ContentDisposition.attachment()
                    .filename(response.getFileName(), StandardCharsets.UTF_8)
                    .build()
            ))
            .body(new InputStreamResource(response.getInputStream()));
    }
}
