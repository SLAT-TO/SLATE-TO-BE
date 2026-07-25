package com.slatto.domain.feedback.controller;

import com.slatto.domain.feedback.dto.request.FeedbackDetailRequest.ReplyCreateReqDTO;
import com.slatto.domain.feedback.dto.response.FeedbackDetailResponse.ReplyCreateResDTO;
import com.slatto.domain.feedback.dto.response.FeedbackDetailResponse.ReplyListResDTO;
import com.slatto.domain.feedback.dto.request.FeedbackDetailRequest.ReplyUpdateReqDTO;
import com.slatto.domain.feedback.dto.response.FeedbackDetailResponse.ReplyUpdateResDTO;
import com.slatto.domain.feedback.dto.request.FeedbackDetailRequest.ReplyStatusReqDTO;
import com.slatto.domain.feedback.dto.response.FeedbackDetailResponse.ReplyStatusResDTO;
import com.slatto.domain.feedback.service.FeedbackDetailService;
import com.slatto.global.response.ApiResponse;
import com.slatto.global.response.code.CommonSuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Reply", description = "답글 API")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class FeedbackDetailController {

    private final FeedbackDetailService feedbackDetailService;

    @Operation(summary = "답글 작성")
    @PostMapping("/feedbacks/{feedbackId}/replies")
    public ResponseEntity<ApiResponse<ReplyCreateResDTO>> createReply(
            @PathVariable Long feedbackId,
            @Valid @RequestBody ReplyCreateReqDTO request
    ) {
        ReplyCreateResDTO result = feedbackDetailService.createReply(feedbackId, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(CommonSuccessCode.CREATED, result));
    }

    @Operation(summary = "답글 목록 조회")
    @GetMapping("/feedbacks/{feedbackId}/replies")
    public ResponseEntity<ApiResponse<ReplyListResDTO>> getReplyList(
            @PathVariable Long feedbackId,
            @RequestParam(required = false) Long cursor,
            @RequestParam(required = false) Integer size
    ) {
        ReplyListResDTO result = feedbackDetailService.getReplyList(feedbackId, cursor, size);

        return ResponseEntity
                .ok(ApiResponse.success(CommonSuccessCode.OK, result));
    }

    @Operation(summary = "답글 수정")
    @PatchMapping("/replies/{replyId}")
    public ResponseEntity<ApiResponse<ReplyUpdateResDTO>> updateReply(
            @PathVariable Long replyId,
            @Valid @RequestBody ReplyUpdateReqDTO request
    ) {
        ReplyUpdateResDTO result = feedbackDetailService.updateReply(replyId, request);

        return ResponseEntity
                .ok(ApiResponse.success(CommonSuccessCode.OK, result));
    }

    @Operation(summary = "답글 삭제")
    @DeleteMapping("/replies/{replyId}")
    public ResponseEntity<ApiResponse<Void>> deleteReply(
            @PathVariable Long replyId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long guestId
    ) {
        feedbackDetailService.deleteReply(replyId, userId, guestId);

        return ResponseEntity
                .ok(ApiResponse.success(CommonSuccessCode.OK, null));
    }

    @Operation(summary = "답글 해결 상태 변경")
    @PatchMapping("/replies/{replyId}/status")
    public ResponseEntity<ApiResponse<ReplyStatusResDTO>> changeReplyStatus(
            @PathVariable Long replyId,
            @Valid @RequestBody ReplyStatusReqDTO request
    ) {
        ReplyStatusResDTO result = feedbackDetailService.changeReplyStatus(replyId, request);

        return ResponseEntity
                .ok(ApiResponse.success(CommonSuccessCode.OK, result));
    }
}