package com.slatto.domain.feedback.controller;

import com.slatto.domain.feedback.dto.request.FeedbackDetailRequest.ReplyCreateReqDTO;
import com.slatto.domain.feedback.dto.response.FeedbackDetailResponse.ReplyCreateResDTO;
import com.slatto.domain.feedback.dto.response.FeedbackDetailResponse.ReplyListResDTO;
import com.slatto.domain.feedback.dto.request.FeedbackDetailRequest.ReplyUpdateReqDTO;
import com.slatto.domain.feedback.dto.response.FeedbackDetailResponse.ReplyUpdateResDTO;
import com.slatto.domain.feedback.dto.request.FeedbackDetailRequest.ReplyStatusReqDTO;
import com.slatto.domain.feedback.dto.response.FeedbackDetailResponse.ReplyStatusResDTO;
import com.slatto.domain.feedback.service.FeedbackDetailService;
import com.slatto.domain.feedback.support.GuestHeaderValidator;
import com.slatto.global.config.ApiErrorCodes;
import com.slatto.global.config.OptionalAuthentication;
import com.slatto.global.response.ApiResponse;
import com.slatto.global.response.code.CommonSuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(
        name = "Feedback Reply",
        description = """
                피드백 답글 API. 로그인 사용자와 공유 링크로 들어온 게스트가 함께 쓴다.
                게스트로 호출하려면 ShareLink API 에서 먼저 게스트 등록을 마치고,
                받은 guestId 를 X-Guest-Id 헤더에, sessionToken 을 X-Guest-Token 헤더에 실어 보낸다.
                두 값의 짝이 맞지 않으면 SHARELINK403 으로 막힌다."""
)
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class FeedbackDetailController {

    private final FeedbackDetailService feedbackDetailService;

    @Operation(
            summary = "답글 작성",
            description = """
                    회원은 토큰으로, 게스트는 `X-Guest-Id` 와 `X-Guest-Token` 헤더로 식별한다.
                    **둘 중 하나만 보내면 400** 이다. 회원은 게스트 헤더를 넣지 않는다.

                    회원은 원 피드백이 달린 영상의 프로젝트 활성 멤버여야 하고, 게스트는 그 영상의 공유 링크로 들어온 게스트여야 한다.
                    원 피드백이 삭제됐으면 404 다.

                    답글은 한 단계까지만 달린다. 답글에 다시 답글을 달 수는 없다.

                    작성되면 작성자를 뺀 프로젝트 멤버 전원에게 알림이 가고 최근 활동에 남는다.
                    """
    )
    @OptionalAuthentication
    @ResponseStatus(HttpStatus.CREATED)
    @ApiErrorCodes({"PROJECT403", "SHARELINK403", "SHARELINK410"})
    @PostMapping("/feedbacks/{feedbackId}/replies")
    public ResponseEntity<ApiResponse<ReplyCreateResDTO>> createReply(
            @PathVariable Long feedbackId,
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "게스트로 작성할 때만 보냅니다. 게스트 등록 응답의 guestId 값이며, X-Guest-Token 과 짝이 맞아야 합니다. 로그인 사용자는 생략합니다.", example = "20")
            @RequestHeader(value = "X-Guest-Id", required = false) Long guestId,
            @Parameter(description = "게스트로 작성할 때만 보냅니다. 게스트 등록 응답의 sessionToken 값이며, X-Guest-Id 와 짝이 맞아야 합니다. 로그인 사용자는 생략합니다.")
            @RequestHeader(value = "X-Guest-Token", required = false) String guestToken,
            @Valid @RequestBody ReplyCreateReqDTO request
    ) {
        GuestHeaderValidator.validatePair(guestId, guestToken);
        ReplyCreateResDTO result = feedbackDetailService.createReply(feedbackId, userId, guestId, guestToken, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(CommonSuccessCode.CREATED, result));
    }

    @Operation(
            summary = "답글 목록 조회",
            description = """
                    **익명 조회는 막혀 있다.** 회원 토큰이나 게스트 자격(`X-Guest-Id` + `X-Guest-Token`) 중 하나는 있어야 하고,
                    둘 다 없으면 `SHARELINK403` 이다. 게스트 헤더를 하나만 보내면 400 이다.

                    등록순으로 내려간다. 커서는 직전 응답의 `nextCursor` 를 그대로 넣는다.

                    원 피드백이 삭제됐으면 404 이고, 삭제된 답글은 목록에서 빠진다.
                    """
    )
    @OptionalAuthentication
    @ApiErrorCodes({"PROJECT403", "SHARELINK403", "SHARELINK410"})
    @GetMapping("/feedbacks/{feedbackId}/replies")
    public ResponseEntity<ApiResponse<ReplyListResDTO>> getReplyList(
            @PathVariable Long feedbackId,
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "게스트로 조회할 때만 보냅니다. 게스트 등록 응답의 guestId 값입니다. 로그인 사용자는 생략합니다.", example = "20")
            @RequestHeader(value = "X-Guest-Id", required = false) Long guestId,
            @Parameter(description = "게스트로 조회할 때만 보냅니다. 게스트 등록 응답의 sessionToken 값이며, X-Guest-Id 와 짝이 맞아야 합니다. 로그인 사용자는 생략합니다.")
            @RequestHeader(value = "X-Guest-Token", required = false) String guestToken,
            @Parameter(description = "이전 응답의 nextCursor. 첫 페이지에서는 생략합니다.", example = "31")
            @RequestParam(required = false) Long cursor,
            @Parameter(description = "조회 개수. 생략 시 10, 최대 50입니다.", example = "10")
            @RequestParam(required = false) Integer size
    ) {
        GuestHeaderValidator.validatePair(guestId, guestToken);
        ReplyListResDTO result = feedbackDetailService.getReplyList(feedbackId, userId, guestId, guestToken, cursor, size);

        return ResponseEntity
                .ok(ApiResponse.success(CommonSuccessCode.OK, result));
    }

    @Operation(
            summary = "답글 수정",
            description = """
                    **작성자 본인만 수정할 수 있다.** 남의 답글에 요청하면 `FEEDBACK_REPLY403` 이다.
                    원 피드백을 쓴 사람이라도 남이 단 답글은 고칠 수 없다.

                    게스트 헤더는 `X-Guest-Id` 와 `X-Guest-Token` 을 짝으로 보낸다. 하나만 보내면 400 이다.

                    피드백 수정과 달리 `content` 는 필수다. 답글에는 부분 수정할 다른 항목이 없다.

                    이미 삭제된 답글은 404 다.
                    """
    )
    @OptionalAuthentication
    @ApiErrorCodes({"FEEDBACK_REPLY403", "PROJECT403", "SHARELINK403", "SHARELINK410"})
    @PatchMapping("/replies/{replyId}")
    public ResponseEntity<ApiResponse<ReplyUpdateResDTO>> updateReply(
            @PathVariable Long replyId,
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "게스트로 수정할 때만 보냅니다. 게스트 등록 응답의 guestId 값이며, X-Guest-Token 과 짝이 맞아야 합니다. 로그인 사용자는 생략합니다.", example = "20")
            @RequestHeader(value = "X-Guest-Id", required = false) Long guestId,
            @Parameter(description = "게스트로 수정할 때만 보냅니다. 게스트 등록 응답의 sessionToken 값이며, X-Guest-Id 와 짝이 맞아야 합니다. 로그인 사용자는 생략합니다.")
            @RequestHeader(value = "X-Guest-Token", required = false) String guestToken,
            @Valid @RequestBody ReplyUpdateReqDTO request
    ) {
        GuestHeaderValidator.validatePair(guestId, guestToken);
        ReplyUpdateResDTO result = feedbackDetailService.updateReply(replyId, userId, guestId, guestToken, request);

        return ResponseEntity
                .ok(ApiResponse.success(CommonSuccessCode.OK, result));
    }

    @Operation(
            summary = "답글 삭제",
            description = """
                    **작성자 본인만 삭제할 수 있다.** 남의 답글에 요청하면 `FEEDBACK_REPLY403` 이다.

                    게스트는 `X-Guest-Id` 와 `X-Guest-Token` 을 짝으로 보낸다. 하나만 보내면 400 이다.
                    회원은 둘 다 생략하고 토큰만 보낸다.

                    실제로 행을 지우지 않고 삭제 시각만 남긴다. 이미 삭제된 답글은 404 다.
                    """
    )
    @OptionalAuthentication
    @ApiErrorCodes({"FEEDBACK_REPLY403", "PROJECT403", "SHARELINK403", "SHARELINK410"})
    @DeleteMapping("/replies/{replyId}")
    public ResponseEntity<ApiResponse<Void>> deleteReply(
            @PathVariable Long replyId,
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "게스트로 삭제할 때만 보냅니다. 게스트 등록 응답의 guestId 값입니다. 로그인 사용자는 생략합니다.", example = "20")
            @RequestHeader(value = "X-Guest-Id", required = false) Long guestId,
            @Parameter(description = "게스트로 삭제할 때만 보냅니다. 게스트 등록 응답의 sessionToken 값이며, X-Guest-Id 와 짝이 맞아야 합니다. 로그인 사용자는 생략합니다.")
            @RequestHeader(value = "X-Guest-Token", required = false) String guestToken
    ) {
        GuestHeaderValidator.validatePair(guestId, guestToken);
        feedbackDetailService.deleteReply(replyId, userId, guestId, guestToken);

        return ResponseEntity
                .ok(ApiResponse.success(CommonSuccessCode.OK, null));
    }

    @Operation(
            summary = "답글 해결 상태 변경",
            description = "활성 프로젝트 멤버만 변경할 수 있다. 다른 답글 API 와 달리 게스트는 호출할 수 없다."
    )
    @ApiErrorCodes("PROJECT403")
    @PatchMapping("/replies/{replyId}/status")
    public ResponseEntity<ApiResponse<ReplyStatusResDTO>> changeReplyStatus(
            @PathVariable Long replyId,
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody ReplyStatusReqDTO request
    ) {
        ReplyStatusResDTO result = feedbackDetailService.changeReplyStatus(replyId, userId, request);

        return ResponseEntity
                .ok(ApiResponse.success(CommonSuccessCode.OK, result));
    }
}