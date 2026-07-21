package com.slatto.domain.schedule.dto;

import com.slatto.domain.project.enums.Permission;
import com.slatto.domain.user.enums.RoleName;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ScheduleParticipantCandidateResponse {

    private List<Candidate> candidates;

    @Getter
    @Builder
    public static class Candidate {

        private Long userId;

        private String nickname;

        private String profileImageUrl;

        private Permission permission;

        private List<RoleName> jobRole;
    }
}
