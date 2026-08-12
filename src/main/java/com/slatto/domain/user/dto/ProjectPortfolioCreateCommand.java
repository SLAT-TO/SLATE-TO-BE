package com.slatto.domain.user.dto;

import com.slatto.domain.user.entity.Users;
import com.slatto.domain.user.enums.CategoryName;
import com.slatto.domain.user.enums.Kind;
import com.slatto.domain.user.enums.RoleName;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

// 프로젝트가 완료될 때 참여자 포트폴리오를 만들기 위한 입력이다.
// Project 를 그대로 받지 않는 이유는 PortfolioService 가 project 도메인을 참조하면
// ProjectService 와 순환 참조가 생기기 때문이다. 필요한 값만 옮겨 담아 전달한다.
@Getter
@Builder
public class ProjectPortfolioCreateCommand {

    private String title;

    private CategoryName type;

    private Kind kind;

    private String clientName;

    private String description;

    // 프로젝트의 최신 영상 썸네일이다. 영상 없이도 완료할 수 있으므로 비어 있을 수 있다.
    private String thumbnailUrl;

    private LocalDate startDate;

    private LocalDate endDate;

    private List<Participant> participants;

    @Getter
    @Builder
    public static class Participant {

        private Users user;

        // 역할이 지정되지 않은 멤버도 포트폴리오를 받는다. 이때는 빈 목록이다.
        private List<RoleName> roles;
    }
}
