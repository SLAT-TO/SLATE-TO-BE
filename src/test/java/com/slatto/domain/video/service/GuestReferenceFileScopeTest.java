package com.slatto.domain.video.service;

import com.slatto.domain.project.entity.Project;
import com.slatto.domain.project.entity.ProjectFile;
import com.slatto.domain.project.service.ProjectAccessValidator;
import com.slatto.domain.video.entity.Video;
import com.slatto.domain.video.entity.VideoReferenceFile;
import com.slatto.domain.video.repository.VideoReferenceFileRepository;
import com.slatto.domain.video.repository.VideoRepository;
import com.slatto.global.exception.BaseException;
import com.slatto.global.response.code.CommonErrorCode;
import com.slatto.global.storage.StorageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * 게스트에게는 projectId 를 내려주지 않으므로, 다운로드 범위가 공유된 영상 하나로
 * 좁혀지는지가 이 경로의 핵심이다.
 */
@ExtendWith(MockitoExtension.class)
class GuestReferenceFileScopeTest {

    private static final Long PROJECT_ID = 3L;
    private static final Long VIDEO_ID = 11L;
    private static final Long REFERENCE_FILE_ID = 7L;

    @Mock private ProjectAccessValidator projectAccessValidator;
    @Mock private VideoRepository videoRepository;
    @Mock private VideoReferenceFileRepository videoReferenceFileRepository;
    @Mock private StorageService storageService;

    @Mock private Video video;
    @Mock private Project project;
    @Mock private VideoReferenceFile referenceFile;
    @Mock private ProjectFile projectFile;

    @InjectMocks private VideoReferenceFileService videoReferenceFileService;

    @Test
    @DisplayName("다운로드는 공유된 영상에 연결된 파일만 찾는다")
    void 다운로드는_공유된_영상에_연결된_파일만_찾는다() {
        givenSharedVideo();
        given(videoReferenceFileRepository.findActiveReferenceFile(PROJECT_ID, VIDEO_ID, REFERENCE_FILE_ID))
                .willReturn(Optional.of(referenceFile));
        given(referenceFile.getProjectFile()).willReturn(projectFile);
        given(projectFile.getFileName()).willReturn("콘티.pdf");
        given(projectFile.getContentType()).willReturn("application/pdf");
        given(projectFile.getFileSize()).willReturn(1024L);
        given(projectFile.getStorageKey()).willReturn("projects/3/conti.pdf");

        var result = videoReferenceFileService.downloadGuestReferenceFile(video, REFERENCE_FILE_ID);

        assertThat(result.getFileName()).isEqualTo("콘티.pdf");
        verify(videoReferenceFileRepository).findActiveReferenceFile(PROJECT_ID, VIDEO_ID, REFERENCE_FILE_ID);
        verify(storageService).download("projects/3/conti.pdf");
    }

    @Test
    @DisplayName("같은 프로젝트라도 그 영상에 연결되지 않은 파일은 404 이고 S3 를 건드리지 않는다")
    void 그_영상에_연결되지_않은_파일은_404_이다() {
        givenSharedVideo();
        given(videoReferenceFileRepository.findActiveReferenceFile(PROJECT_ID, VIDEO_ID, 999L))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> videoReferenceFileService.downloadGuestReferenceFile(video, 999L))
                .isInstanceOfSatisfying(BaseException.class,
                        exception -> assertThat(exception.getErrorCode()).isSameAs(CommonErrorCode.NOT_FOUND));
        verify(storageService, never()).download(anyString());
    }

    @Test
    @DisplayName("게스트 조회는 멤버 권한 검증을 타지 않는다")
    void 게스트_조회는_멤버_권한_검증을_타지_않는다() {
        givenSharedVideo();
        given(videoReferenceFileRepository.findActiveReferenceFilesByCursor(
                org.mockito.ArgumentMatchers.eq(PROJECT_ID),
                org.mockito.ArgumentMatchers.eq(VIDEO_ID),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.any()
        )).willReturn(java.util.List.of());

        var result = videoReferenceFileService.getGuestReferenceFiles(video, null, null, null);

        assertThat(result.items()).isEmpty();
        assertThat(result.hasNext()).isFalse();
        verify(projectAccessValidator, never()).validateProjectAccess(
                org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyLong());
    }

    private void givenSharedVideo() {
        given(video.getProject()).willReturn(project);
        given(project.getId()).willReturn(PROJECT_ID);
        given(video.getId()).willReturn(VIDEO_ID);
    }
}
