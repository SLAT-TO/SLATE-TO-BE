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
                받은 sessionToken 을 X-Guest-Token 헤더에, guestId 를 파라미터나 본문에 실어 보낸다.
                두 값의 짝이 맞지 않으면 SHARELINK403 으로 막힌다."""
)
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;

    @Operation(summary = "피드백 작성")
    @OptionalAuthentication
    @ResponseStatus(HttpStatus.CREATED)
    @ApiErrorCodes({"PROJECT403", "SHARELINK403", "SHARELINK410"})
    @PostMapping("/videos/{videoId}/feedbacks")
    public ResponseEntity<ApiResponse<FeedbackCreateResDTO>> createFeedback(
            @PathVariable Long videoId,
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "게스트로 작성할 때만 보냅니다. 게스트 등록 응답의 sessionToken 값이며, 본문의 guestId 와 짝이 맞아야 합니다. 로그인 사용자는 생략합니다.")
            @RequestHeader(value = "X-Guest-Token", required = false) String guestToken,
            @Valid @RequestBody FeedbackCreateReqDTO request
    ) {
        FeedbackCreateResDTO result = feedbackService.createFeedback(videoId, userId, guestToken, request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(CommonSuccessCode.CREATED, result));
    }

    @Operation(summary = "피드백 수정")
    @OptionalAuthentication
    @ApiErrorCodes({"FEEDBACK403", "PROJECT403", "SHARELINK403", "SHARELINK410"})
    @PatchMapping("/feedbacks/{feedbackId}")
    public ResponseEntity<ApiResponse<FeedbackUpdateResDTO>> updateFeedback(
            @PathVariable Long feedbackId,
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "게스트로 수정할 때만 보냅니다. 게스트 등록 응답의 sessionToken 값이며, 본문의 guestId 와 짝이 맞아야 합니다. 로그인 사용자는 생략합니다.")
            @RequestHeader(value = "X-Guest-Token", required = false) String guestToken,
            @Valid @RequestBody FeedbackUpdateReqDTO request
    ) {
        FeedbackUpdateResDTO result = feedbackService.updateFeedback(feedbackId, userId, guestToken, request);
        return ResponseEntity
                .ok(ApiResponse.success(CommonSuccessCode.OK, result));
    }

    @Operation(summary = "피드백 삭제")
    @OptionalAuthentication
    @ApiErrorCodes({"FEEDBACK403", "PROJECT403", "SHARELINK403", "SHARELINK410"})
    @DeleteMapping("/feedbacks/{feedbackId}")
    public ResponseEntity<ApiResponse<Void>> deleteFeedback(
            @PathVariable Long feedbackId,
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "게스트로 삭제할 때만 보냅니다. 게스트 등록 응답의 guestId 값입니다. 로그인 사용자는 생략합니다.", example = "20")
            @RequestParam(required = false) Long guestId,
            @Parameter(description = "게스트로 삭제할 때만 보냅니다. 게스트 등록 응답의 sessionToken 값이며, guestId 와 짝이 맞아야 합니다. 로그인 사용자는 생략합니다.")
            @RequestHeader(value = "X-Guest-Token", required = false) String guestToken
    ) {
        feedbackService.deleteFeedback(feedbackId, userId, guestId, guestToken);
        return ResponseEntity
                .ok(ApiResponse.success(CommonSuccessCode.OK, null));
    }

    @Operation(summary = "피드백 목록 조회")
    @OptionalAuthentication
    @ApiErrorCodes({"PROJECT403", "SHARELINK403", "SHARELINK410"})
    @GetMapping("/videos/{videoId}/feedbacks")
    public ResponseEntity<ApiResponse<FeedbackListResDTO>> getFeedbackList(
            @PathVariable Long videoId,
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "게스트로 조회할 때만 보냅니다. 게스트 등록 응답의 guestId 값입니다. 로그인 사용자는 생략합니다.", example = "20")
            @RequestParam(required = false) Long guestId,
            @Parameter(description = "게스트로 조회할 때만 보냅니다. 게스트 등록 응답의 sessionToken 값이며, guestId 와 짝이 맞아야 합니다. 로그인 사용자는 생략합니다.")
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