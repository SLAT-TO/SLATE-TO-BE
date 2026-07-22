package com.slatto.domain.feedback.controller;

import com.slatto.domain.feedback.dto.request.FeedbackRequest.FeedbackCreateReqDTO;
import com.slatto.domain.feedback.dto.request.FeedbackRequest.FeedbackUpdateReqDTO;
import com.slatto.domain.feedback.dto.response.FeedbackResponse.FeedbackCreateResDTO;
import com.slatto.domain.feedback.dto.response.FeedbackResponse.FeedbackUpdateResDTO;
import com.slatto.domain.feedback.dto.response.FeedbackResponse.FeedbackListResDTO;
import com.slatto.domain.feedback.service.FeedbackService;
import com.slatto.global.response.ApiResponse;
import com.slatto.global.response.code.CommonSuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Feedback", description = "피드백 API")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;

    @Operation(summary = "피드백 작성")
    @PostMapping("/videos/{videoId}/feedbacks")
    public ResponseEntity<ApiResponse<FeedbackCreateResDTO>> createFeedback(
            @PathVariable Long videoId,
            @Valid @RequestBody FeedbackCreateReqDTO request
    ) {
        FeedbackCreateResDTO result = feedbackService.createFeedback(videoId, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(CommonSuccessCode.CREATED, result));
    }

    @Operation(summary = "피드백 수정")
    @PatchMapping("/feedbacks/{feedbackId}")
    public ResponseEntity<ApiResponse<FeedbackUpdateResDTO>> updateFeedback(
            @PathVariable Long feedbackId,
            @Valid @RequestBody FeedbackUpdateReqDTO request
    ) {
        FeedbackUpdateResDTO result = feedbackService.updateFeedback(feedbackId, request);

        return ResponseEntity
                .ok(ApiResponse.success(CommonSuccessCode.OK, result));
    }

    @Operation(summary = "피드백 삭제")
    @DeleteMapping("/feedbacks/{feedbackId}")
    public ResponseEntity<ApiResponse<Void>> deleteFeedback(
            @PathVariable Long feedbackId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long guestId
    ) {
        feedbackService.deleteFeedback(feedbackId, userId, guestId);

        return ResponseEntity
                .ok(ApiResponse.success(CommonSuccessCode.OK, null));
    }

    @Operation(summary = "피드백 목록 조회")
    @GetMapping("/videos/{videoId}/feedbacks")
    public ResponseEntity<ApiResponse<FeedbackListResDTO>> getFeedbackList(
            @PathVariable Long videoId,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) Integer size
    ) {
        FeedbackListResDTO result = feedbackService.getFeedbackList(videoId, cursor, size);

        return ResponseEntity
                .ok(ApiResponse.success(CommonSuccessCode.OK, result));
    }
}