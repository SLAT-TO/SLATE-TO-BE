package com.slatto.domain.feedback.controller;

import com.slatto.domain.feedback.dto.request.FeedbackRequest.FeedbackCreateReqDTO;
import com.slatto.domain.feedback.dto.request.FeedbackRequest.FeedbackUpdateReqDTO;
import com.slatto.domain.feedback.dto.response.FeedbackResponse.FeedbackCreateResDTO;
import com.slatto.domain.feedback.dto.response.FeedbackResponse.FeedbackUpdateResDTO;
import com.slatto.domain.feedback.dto.response.FeedbackResponse.FeedbackListResDTO;
import com.slatto.domain.feedback.dto.request.FeedbackRequest.FeedbackStatusReqDTO;
import com.slatto.domain.feedback.dto.response.FeedbackResponse.FeedbackStatusResDTO;
import com.slatto.domain.feedback.service.FeedbackService;
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
        name = "Feedback",
        description = """
                피드백 API. 로그인 사용자와 공유 링크로 들어온 게스트가 함께 쓴다.
                게스트로 호출하려면 ShareLink API 에서 먼저 게스트 등록을 마치고,
                받은 guestId 를 X-Guest-Id 헤더에, sessionToken 을 X-Guest-Token 헤더에 실어 보낸다.
                두 값의 짝이 맞지 않으면 SHARELINK403 으로 막힌다."""
)
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;

    @Operation(
            summary = "피드백 작성",
            description = """
                    회원은 토큰으로, 게스트는 `X-Guest-Id` 와 `X-Guest-Token` 헤더로 식별한다.
                    **둘을 함께 보내거나 둘 다 보내지 않으면 400** 이다. 회원은 게스트 헤더를 넣지 않는다.

                    회원은 이 영상이 속한 프로젝트의 활성 멤버여야 하고, 게스트는 자기 공유 링크의 영상에만 남길 수 있다.

                    `startTime` 과 `endTime` 은 영상 재생 지점(초)이다. 둘 다 생략하면 영상 전체에 대한 피드백이 되고,
                    함께 보내면 `startTime` 이 `endTime` 보다 클 수 없다.

                    작성되면 작성자를 뺀 프로젝트 멤버 전원에게 알림이 가고 최근 활동에 남는다.
                    """
    )
    @OptionalAuthentication
    @ResponseStatus(HttpStatus.CREATED)
    @ApiErrorCodes({"PROJECT403", "SHARELINK403", "SHARELINK410"})
    @PostMapping("/videos/{videoId}/feedbacks")
    public ResponseEntity<ApiResponse<FeedbackCreateResDTO>> createFeedback(
            @PathVariable Long videoId,
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "게스트로 작성할 때만 보냅니다. 게스트 등록 응답의 guestId 값이며, X-Guest-Token 과 짝이 맞아야 합니다. 로그인 사용자는 생략합니다.", example = "20")
            @RequestHeader(value = "X-Guest-Id", required = false) Long guestId,
            @Parameter(description = "게스트로 작성할 때만 보냅니다. 게스트 등록 응답의 sessionToken 값이며, X-Guest-Id 와 짝이 맞아야 합니다. 로그인 사용자는 생략합니다.")
            @RequestHeader(value = "X-Guest-Token", required = false) String guestToken,
            @Valid @RequestBody FeedbackCreateReqDTO request
    ) {
        FeedbackCreateResDTO result = feedbackService.createFeedback(videoId, userId, guestId, guestToken, request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(CommonSuccessCode.CREATED, result));
    }

    @Operation(
            summary = "피드백 수정",
            description = """
                    **작성자 본인만 수정할 수 있다.** 남의 피드백에 요청하면 `FEEDBACK403` 이다.
                    같은 프로젝트 멤버여도, 같은 공유 링크의 다른 게스트여도 마찬가지다.

                    전달한 항목만 부분 수정된다. `content`, `startTime`, `endTime` 모두 생략할 수 있다.
                    시간 검증은 수정을 반영한 뒤의 최종 값으로 한다. 한쪽만 보내도 기존 값과 묶여 `startTime` ≤ `endTime` 이어야 한다.

                    이미 삭제된 피드백은 404 다.
                    """
    )
    @OptionalAuthentication
    @ApiErrorCodes({"FEEDBACK403", "PROJECT403", "SHARELINK403", "SHARELINK410"})
    @PatchMapping("/feedbacks/{feedbackId}")
    public ResponseEntity<ApiResponse<FeedbackUpdateResDTO>> updateFeedback(
            @PathVariable Long feedbackId,
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "게스트로 수정할 때만 보냅니다. 게스트 등록 응답의 guestId 값이며, X-Guest-Token 과 짝이 맞아야 합니다. 로그인 사용자는 생략합니다.", example = "20")
            @RequestHeader(value = "X-Guest-Id", required = false) Long guestId,
            @Parameter(description = "게스트로 수정할 때만 보냅니다. 게스트 등록 응답의 sessionToken 값이며, X-Guest-Id 와 짝이 맞아야 합니다. 로그인 사용자는 생략합니다.")
            @RequestHeader(value = "X-Guest-Token", required = false) String guestToken,
            @Valid @RequestBody FeedbackUpdateReqDTO request
    ) {
        FeedbackUpdateResDTO result = feedbackService.updateFeedback(feedbackId, userId, guestId, guestToken, request);
        return ResponseEntity
                .ok(ApiResponse.success(CommonSuccessCode.OK, result));
    }

    @Operation(
            summary = "피드백 삭제",
            description = """
                    **작성자 본인만 삭제할 수 있다.** 남의 피드백에 요청하면 `FEEDBACK403` 이다.

                    게스트는 `X-Guest-Id` 와 `X-Guest-Token` 을 헤더로 함께 보낸다.
                    회원은 둘 다 생략하고 토큰만 보낸다.

                    실제로 행을 지우지 않고 삭제 시각만 남긴다. 목록과 답글 조회에서 함께 빠진다.
                    이미 삭제된 피드백은 404 다.
                    """
    )
    @OptionalAuthentication
    @ApiErrorCodes({"FEEDBACK403", "PROJECT403", "SHARELINK403", "SHARELINK410"})
    @DeleteMapping("/feedbacks/{feedbackId}")
    public ResponseEntity<ApiResponse<Void>> deleteFeedback(
            @PathVariable Long feedbackId,
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "게스트로 삭제할 때만 보냅니다. 게스트 등록 응답의 guestId 값입니다. 로그인 사용자는 생략합니다.", example = "20")
            @RequestHeader(value = "X-Guest-Id", required = false) Long guestId,
            @Parameter(description = "게스트로 삭제할 때만 보냅니다. 게스트 등록 응답의 sessionToken 값이며, X-Guest-Id 와 짝이 맞아야 합니다. 로그인 사용자는 생략합니다.")
            @RequestHeader(value = "X-Guest-Token", required = false) String guestToken
    ) {
        feedbackService.deleteFeedback(feedbackId, userId, guestId, guestToken);
        return ResponseEntity
                .ok(ApiResponse.success(CommonSuccessCode.OK, null));
    }

    @Operation(
            summary = "피드백 목록 조회",
            description = """
                    **익명 조회는 막혀 있다.** 회원 토큰이나 게스트 자격(`X-Guest-Id` + `X-Guest-Token`) 중 하나는 있어야 하고,
                    둘 다 없으면 `SHARELINK403` 이다. 인증이 선택이라는 것은 게스트도 볼 수 있다는 뜻이지 누구나 볼 수 있다는 뜻이 아니다.

                    재생 지점이 있는 피드백이 앞에 오고 그 안에서 `startTime` 오름차순, 같은 지점이면 등록순이다.
                    재생 지점이 없는 피드백은 모두 뒤로 밀린 뒤 등록순으로 붙는다.

                    커서는 직전 응답의 `nextCursor` 를 그대로 넣는다. 형식이 어긋나면 400 이다.
                    각 항목에는 답글 개수가 함께 담기고, 삭제된 피드백은 빠진다.
                    """
    )
    @OptionalAuthentication
    @ApiErrorCodes({"PROJECT403", "SHARELINK403", "SHARELINK410"})
    @GetMapping("/videos/{videoId}/feedbacks")
    public ResponseEntity<ApiResponse<FeedbackListResDTO>> getFeedbackList(
            @PathVariable Long videoId,
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "게스트로 조회할 때만 보냅니다. 게스트 등록 응답의 guestId 값입니다. 로그인 사용자는 생략합니다.", example = "20")
            @RequestHeader(value = "X-Guest-Id", required = false) Long guestId,
            @Parameter(description = "게스트로 조회할 때만 보냅니다. 게스트 등록 응답의 sessionToken 값이며, X-Guest-Id 와 짝이 맞아야 합니다. 로그인 사용자는 생략합니다.")
            @RequestHeader(value = "X-Guest-Token", required = false) String guestToken,
            @Parameter(description = "이전 응답의 nextCursor 를 그대로 넣습니다. {재생지점초}_{피드백ID} 형식이고 재생 지점이 없는 피드백은 앞이 n 입니다. 첫 페이지에서는 생략합니다.", example = "12_57")
            @RequestParam(required = false) String cursor,
            @Parameter(description = "조회 개수. 생략 시 10, 최대 50입니다.", example = "10")
            @RequestParam(required = false) Integer size
    ) {
        FeedbackListResDTO result = feedbackService.getFeedbackList(videoId, userId, guestId, guestToken, cursor, size);
        return ResponseEntity
                .ok(ApiResponse.success(CommonSuccessCode.OK, result));
    }

    @Operation(
            summary = "피드백 해결 상태 변경",
            description = "활성 프로젝트 멤버만 변경할 수 있다. 다른 피드백 API 와 달리 게스트는 호출할 수 없다."
    )
    @ApiErrorCodes("PROJECT403")
    @PatchMapping("/feedbacks/{feedbackId}/status")
    public ResponseEntity<ApiResponse<FeedbackStatusResDTO>> changeFeedbackStatus(
            @PathVariable Long feedbackId,
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody FeedbackStatusReqDTO request
    ) {
        FeedbackStatusResDTO result = feedbackService.changeFeedbackStatus(feedbackId, userId, request);

        return ResponseEntity
                .ok(ApiResponse.success(CommonSuccessCode.OK, result));
    }
}