package com.slatto.domain.feedback.controller;

import com.slatto.domain.feedback.dto.request.FeedbackDetailRequest.ReplyCreateReqDTO;
import com.slatto.domain.feedback.dto.response.FeedbackDetailResponse.ReplyCreateResDTO;
import com.slatto.domain.feedback.dto.response.FeedbackDetailResponse.ReplyListResDTO;
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
}