package com.slatto.global.response.code;

import com.slatto.domain.auth.exception.AuthErrorCode;
import com.slatto.domain.feedback.exception.FeedbackErrorCode;
import com.slatto.domain.project.exception.ProjectErrorCode;
import com.slatto.domain.recruitment.exception.RecruitmentErrorCode;
import com.slatto.domain.schedule.exception.ScheduleErrorCode;
import com.slatto.domain.sharelink.exception.ShareLinkErrorCode;
import com.slatto.domain.user.exception.UserErrorCode;
import com.slatto.domain.video.exception.VideoErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 코드 문자열로 에러 코드를 되찾을 수 있는지 확인한다.
 *
 * <p>레지스트리가 스스로 훑어 모은 목록을 그대로 다시 물어보면 무엇도 검증하지 못한다.
 * 그래서 도메인마다 코드를 하나씩 손으로 적어 대조한다.
 * 새 도메인이 스캔에서 빠지거나 enum 이 사라지면 여기서 먼저 깨진다.
 */
class ErrorCodeRegistryTest {

	private final ErrorCodeRegistry registry = new ErrorCodeRegistry();

	@Test
	@DisplayName("모든 도메인의 에러 코드를 코드 문자열로 찾는다")
	void findsErrorCodeFromEveryDomain() {
		assertThat(registry.find("COMMON500")).isEqualTo(CommonErrorCode.INTERNAL_SERVER_ERROR);
		assertThat(registry.find("AUTH401")).isEqualTo(AuthErrorCode.INVALID_REFRESH_TOKEN);
		assertThat(registry.find("ONBOARDING409")).isEqualTo(UserErrorCode.ONBOARDING_ALREADY_COMPLETED);
		assertThat(registry.find("PROJECT403")).isEqualTo(ProjectErrorCode.PROJECT_ACCESS_DENIED);
		assertThat(registry.find("RECRUITMENT403")).isEqualTo(RecruitmentErrorCode.RECRUITMENT_WRITER_ONLY);
		assertThat(registry.find("SHARELINK410")).isEqualTo(ShareLinkErrorCode.SHARE_LINK_UNAVAILABLE);
		assertThat(registry.find("VIDEO409")).isEqualTo(VideoErrorCode.VIDEO_ALREADY_REGISTERED);
		assertThat(registry.find("FEEDBACK403")).isEqualTo(FeedbackErrorCode.FEEDBACK_WRITER_ONLY);
		assertThat(registry.find("SCHEDULE403")).isEqualTo(ScheduleErrorCode.SCHEDULE_WRITER_ONLY);
	}

	// 성공 코드까지 담으면 @ApiErrorCodes 에 COMMON200 을 적어도 통과한다.
	@Test
	@DisplayName("성공 코드는 담지 않는다")
	void excludesSuccessCodes() {
		assertThat(registry.contains(CommonSuccessCode.OK.getCode())).isFalse();
	}

	@Test
	@DisplayName("없는 코드를 찾으면 무엇이 없는지 알려주고 실패한다")
	void rejectsUnknownCode() {
		assertThatThrownBy(() -> registry.find("PROJECT499"))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("PROJECT499");
	}
}
