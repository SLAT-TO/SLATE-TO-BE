package com.slatto.domain.feedback.controller;

import com.slatto.domain.feedback.dto.request.FeedbackDetailRequest.ReplyCreateReqDTO;
import com.slatto.domain.feedback.dto.response.FeedbackDetailResponse.ReplyCreateResDTO;
import com.slatto.domain.feedback.dto.response.FeedbackDetailResponse.ReplyListResDTO;
import com.slatto.domain.feedback.dto.request.FeedbackDetailRequest.ReplyUpdateReqDTO;
import com.slatto.domain.feedback.dto.response.FeedbackDetailResponse.ReplyUpdateResDTO;
import com.slatto.domain.feedback.dto.request.FeedbackDetailRequest.ReplyStatusReqDTO;
import com.slatto.domain.feedback.dto.response.FeedbackDetailResponse.ReplyStatusResDTO;
import com.slatto.domain.feedback.service.FeedbackDetailService;
import com.slatto.global.config.OptionalAuthentication;
import com.slatto.global.response.ApiResponse;
import com.slatto.global.response.code.CommonSuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Feedback Reply", description = "피드백 답글 API")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class FeedbackDetailController {

    private final FeedbackDetailService feedbackDetailService;

    @Operation(summary = "답글 작성")
    @OptionalAuthentication
    @PostMapping("/feedbacks/{feedbackId}/replies")
    public ResponseEntity<ApiResponse<ReplyCreateResDTO>> createReply(
            @PathVariable Long feedbackId,
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody ReplyCreateReqDTO request
    ) {
        ReplyCreateResDTO result = feedbackDetailService.createReply(feedbackId, userId, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(CommonSuccessCode.CREATED, result));
    }

    @Operation(summary = "답글 목록 조회")
    @OptionalAuthentication
    @GetMapping("/feedbacks/{feedbackId}/replies")
    public ResponseEntity<ApiResponse<ReplyListResDTO>> getReplyList(
            @PathVariable Long feedbackId,
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) Long guestId,
            @RequestParam(required = false) Long cursor,
            @RequestParam(required = false) Integer size
    ) {
        ReplyListResDTO result = feedbackDetailService.getReplyList(feedbackId, userId, guestId, cursor, size);

        return ResponseEntity
                .ok(ApiResponse.success(CommonSuccessCode.OK, result));
    }

    @Operation(summary = "답글 수정")
    @OptionalAuthentication
    @PatchMapping("/replies/{replyId}")
    public ResponseEntity<ApiResponse<ReplyUpdateResDTO>> updateReply(
            @PathVariable Long replyId,
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody ReplyUpdateReqDTO request
    ) {
        ReplyUpdateResDTO result = feedbackDetailService.updateReply(replyId, userId, request);

        return ResponseEntity
                .ok(ApiResponse.success(CommonSuccessCode.OK, result));
    }

    @Operation(summary = "답글 삭제")
    @OptionalAuthentication
    @DeleteMapping("/replies/{replyId}")
    public ResponseEntity<ApiResponse<Void>> deleteReply(
            @PathVariable Long replyId,
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) Long guestId
    ) {
        feedbackDetailService.deleteReply(replyId, userId, guestId);

        return ResponseEntity
                .ok(ApiResponse.success(CommonSuccessCode.OK, null));
    }

    @Operation(
        summary = "답글 해결 상태 변경",
        description = "활성 프로젝트 멤버만 변경할 수 있다. 다른 답글 API 와 달리 게스트는 호출할 수 없다."
    )
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