package com.slatto.domain.notification.service;

import org.springframework.stereotype.Component;

@Component
public class ActivityMessageFactory {

    public String projectMemberJoined(String actorName) {
        return actorName + "님이 프로젝트에 합류했어요";
    }

    public String projectStatusChanged(String actorName, String previousStatus, String changedStatus) {
        return actorName + "님이 프로젝트 단계를 '" + previousStatus + "'에서 '" + changedStatus + "'으로 변경했어요";
    }

    public String projectUpdated(String actorName) {
        return actorName + "님이 프로젝트 정보를 수정했어요";
    }

    public String scheduleCreated(String actorName, String scheduleTitle) {
        return actorName + "님이 [" + scheduleTitle + "] 일정을 등록했어요";
    }

    public String scheduleUpdated(String actorName, String scheduleTitle) {
        return actorName + "님이 [" + scheduleTitle + "] 일정을 수정했어요";
    }

    public String noticeCreated(String actorName) {
        return actorName + "님이 새 공지를 등록했어요";
    }

    public String fileUploaded(String actorName, String fileName) {
        return actorName + "님이 [" + fileName + "] 파일을 등록했어요";
    }

    public String videoFeedbackCommented(String actorName, String videoTitle) {
        return actorName + "님이 [" + videoTitle + "]에 피드백을 남겼어요";
    }
}
