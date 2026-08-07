package com.slatto.domain.recruitment.service;

import com.slatto.domain.recruitment.converter.RecruitmentConverter;
import com.slatto.domain.recruitment.dto.RecruitmentBookmarkResponse;
import com.slatto.domain.recruitment.dto.RecruitmentListResponse;
import com.slatto.domain.recruitment.dto.RecruitmentSummary;
import com.slatto.domain.recruitment.entity.RecruitmentBookmark;
import com.slatto.domain.recruitment.repository.RecruitmentBookmarkRepository;
import com.slatto.domain.recruitment.repository.RecruitmentRepository;
import com.slatto.global.exception.BaseException;
import com.slatto.global.response.code.CommonErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecruitmentBookmarkService {

    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 50;

    private final RecruitmentRepository recruitmentRepository;
    private final RecruitmentBookmarkRepository recruitmentBookmarkRepository;
    private final RecruitmentConverter recruitmentConverter;

    // 마감된 공고와 본인 공고도 관심 등록을 허용한다. 관심은 스크랩이지 참여 의사가 아니다.
    @Transactional
    public RecruitmentBookmarkResponse addBookmark(Long currentUserId, Long recruitmentId) {
        validateRecruitmentExists(recruitmentId);

        // exists 후 save 는 동시 요청에서 유니크 위반으로 500 이 난다. DB 가 멱등성을 보장하게 한다.
        recruitmentBookmarkRepository.insertIgnore(currentUserId, recruitmentId, LocalDateTime.now());

        return recruitmentConverter.toBookmarkResponse(recruitmentId, true);
    }

    // 등록되지 않은 상태에서 호출해도 200 이다. 삭제 행 수 0 을 예외로 만들지 않는다.
    @Transactional
    public RecruitmentBookmarkResponse removeBookmark(Long currentUserId, Long recruitmentId) {
        validateRecruitmentExists(recruitmentId);

        recruitmentBookmarkRepository.deleteByUserIdAndRecruitmentId(currentUserId, recruitmentId);

        return recruitmentConverter.toBookmarkResponse(recruitmentId, false);
    }

    // 이 목록의 isBookmarked 는 정의상 전부 true 라 배치 조회를 하지 않는다.
    public RecruitmentListResponse getMyBookmarks(Long currentUserId, Long cursor, int size) {
        int pageSize = normalizePageSize(size);
        LocalDate today = recruitmentConverter.currentDate();

        List<RecruitmentBookmark> bookmarks = recruitmentBookmarkRepository.findMyBookmarksByCursor(
            currentUserId,
            cursor,
            PageRequest.of(0, pageSize + 1)
        );

        boolean hasNext = bookmarks.size() > pageSize;
        List<RecruitmentBookmark> currentPage = bookmarks.stream()
            .limit(pageSize)
            .toList();

        List<RecruitmentSummary> items = currentPage.stream()
            .map(bookmark -> recruitmentConverter.toSummary(
                bookmark.getRecruitment(),
                currentUserId,
                true,
                today
            ))
            .toList();

        // 커서는 공고 id 가 아니라 북마크 id 다. 공고 id 를 내려보내면 다음 페이지가 엉뚱한 위치에서 시작한다.
        Long nextCursor = hasNext && !currentPage.isEmpty()
            ? currentPage.get(currentPage.size() - 1).getId()
            : null;

        return recruitmentConverter.toListResponse(items, nextCursor, hasNext);
    }

    private int normalizePageSize(int size) {
        if (size <= 0) {
            return DEFAULT_PAGE_SIZE;
        }

        return Math.min(size, MAX_PAGE_SIZE);
    }

    private void validateRecruitmentExists(Long recruitmentId) {
        if (!recruitmentRepository.existsByIdAndDeletedAtIsNull(recruitmentId)) {
            throw new BaseException(CommonErrorCode.NOT_FOUND);
        }
    }
}
