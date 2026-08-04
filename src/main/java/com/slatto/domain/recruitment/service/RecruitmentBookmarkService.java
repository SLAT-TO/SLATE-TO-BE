package com.slatto.domain.recruitment.service;

import com.slatto.domain.recruitment.converter.RecruitmentConverter;
import com.slatto.domain.recruitment.dto.RecruitmentBookmarkResponse;
import com.slatto.domain.recruitment.repository.RecruitmentBookmarkRepository;
import com.slatto.domain.recruitment.repository.RecruitmentRepository;
import com.slatto.global.exception.BaseException;
import com.slatto.global.response.code.CommonErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecruitmentBookmarkService {

    private final RecruitmentRepository recruitmentRepository;
    private final RecruitmentBookmarkRepository recruitmentBookmarkRepository;
    private final RecruitmentConverter recruitmentConverter;

    // 마감된 공고와 본인 공고도 관심 등록을 허용한다. 관심은 스크랩이지 참여 의사가 아니다.
    @Transactional
    public RecruitmentBookmarkResponse addBookmark(Long currentUserId, Long recruitmentId) {
        validateRecruitmentExists(recruitmentId);

        // exists 후 save 는 동시 요청에서 유니크 위반으로 500 이 난다. DB 가 멱등성을 보장하게 한다.
        recruitmentBookmarkRepository.insertIgnore(currentUserId, recruitmentId);

        return recruitmentConverter.toBookmarkResponse(recruitmentId, true);
    }

    // 등록되지 않은 상태에서 호출해도 200 이다. 삭제 행 수 0 을 예외로 만들지 않는다.
    @Transactional
    public RecruitmentBookmarkResponse removeBookmark(Long currentUserId, Long recruitmentId) {
        validateRecruitmentExists(recruitmentId);

        recruitmentBookmarkRepository.deleteByUserIdAndRecruitmentId(currentUserId, recruitmentId);

        return recruitmentConverter.toBookmarkResponse(recruitmentId, false);
    }

    private void validateRecruitmentExists(Long recruitmentId) {
        if (!recruitmentRepository.existsByIdAndDeletedAtIsNull(recruitmentId)) {
            throw new BaseException(CommonErrorCode.NOT_FOUND);
        }
    }
}
