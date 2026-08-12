package com.slatto.domain.project.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ProjectStatus {

    PREPARING("기획 중"),
    EDITING("편집 중"),
    // 화면 표기는 '촬영 중'이다. 이름만 REVIEWING 으로 남아 있으니 라벨을 바꿀 때 헷갈리지 말 것.
    REVIEWING("촬영 중"),
    COMPLETED("완료");

    // 최근활동 문구를 서버에서 만들기 때문에 사용자에게 보이는 표기를 여기서 관리한다.
    // 화면 라벨과 어긋나면 최근활동에만 다른 단계 이름이 찍힌다.
    private final String label;
}
