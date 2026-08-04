package com.slatto.domain.notification.model;

import com.slatto.domain.notification.enums.ActorType;

public record ActivityActor(
	ActorType actorType,
	Long actorUserId,
	Long actorGuestId,
	String displayName
) {

	public ActivityActor {
		if (actorType == null) {
			throw new IllegalArgumentException("최근활동 행위자 타입은 필수입니다.");
		}
		if (displayName == null || displayName.isBlank()) {
			throw new IllegalArgumentException("최근활동 행위자 이름은 필수입니다.");
		}

		validateActorIds(actorType, actorUserId, actorGuestId);
	}

	public static ActivityActor user(Long userId, String displayName) {
		return new ActivityActor(ActorType.USER, userId, null, displayName);
	}

	public static ActivityActor clientReviewer(Long guestId, String displayName) {
		return new ActivityActor(ActorType.CLIENT_REVIEWER, null, guestId, displayName);
	}

	public static ActivityActor system(String displayName) {
		return new ActivityActor(ActorType.SYSTEM, null, null, displayName);
	}

	private static void validateActorIds(ActorType actorType, Long actorUserId, Long actorGuestId) {
		switch (actorType) {
			case USER -> {
				requirePositiveId(actorUserId, "사용자");
				requireNull(actorGuestId, "회원 행위자는 게스트 ID를 가질 수 없습니다.");
			}
			case CLIENT_REVIEWER -> {
				requireNull(actorUserId, "게스트 행위자는 사용자 ID를 가질 수 없습니다.");
				requirePositiveId(actorGuestId, "게스트");
			}
			case SYSTEM -> {
				requireNull(actorUserId, "시스템 행위자는 사용자 ID를 가질 수 없습니다.");
				requireNull(actorGuestId, "시스템 행위자는 게스트 ID를 가질 수 없습니다.");
			}
		}
	}

	private static void requirePositiveId(Long id, String actorName) {
		if (id == null || id <= 0) {
			throw new IllegalArgumentException(actorName + " ID는 양수여야 합니다.");
		}
	}

	private static void requireNull(Long id, String message) {
		if (id != null) {
			throw new IllegalArgumentException(message);
		}
	}
}
