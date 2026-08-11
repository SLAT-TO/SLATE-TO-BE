<!-- @format -->

# SLAT-TO Backend

<div align="center">

<img width="1920" height="1080" alt="SLAT-TO Backend Banner" src="https://github.com/user-attachments/assets/fbe510e1-01c9-4b1a-b400-38a5297a1efa" />

<h3>영상 제작 협업을 위한 프로젝트 관리 및 피드백 플랫폼</h3>

</div>

---

## 프로젝트 소개

SLAT-TO는 영상 제작자와 클라이언트가 하나의 프로젝트 공간에서 작업물을 공유하고, 피드백을 남기며, 일정과 파일을 관리할 수 있도록 돕는 협업 서비스입니다.

## 팀원 소개

<div align="center">

<table>
  <tr>
    <td align="center">
      <a href="https://github.com/chazy-d">
        <img src="https://github.com/chazy-d.png" width="96" height="96" alt="chazy-d" />
        <br />
        <b>chazy-d</b>
      </a>
      <br />
      Backend
    </td>
    <td align="center">
      <a href="https://github.com/sangwon02">
        <img src="https://github.com/sangwon02.png" width="96" height="96" alt="sangwon02" />
        <br />
        <b>sangwon02</b>
      </a>
      <br />
      Backend
    </td>
    <td align="center">
      <a href="https://github.com/young0206">
        <img src="https://github.com/young0206.png" width="96" height="96" alt="young0206" />
        <br />
        <b>young0206</b>
      </a>
      <br />
      Backend
    </td>
    <td align="center">
      <a href="https://github.com/guingguing">
        <img src="https://github.com/guingguing.png" width="96" height="96" alt="guingguing" />
        <br />
        <b>guingguing</b>
      </a>
      <br />
      Backend
    </td>
    <td align="center">
      <a href="https://github.com/Kohseoyoung">
        <img src="https://github.com/Kohseoyoung.png" width="96" height="96" alt="Kohseoyoung" />
        <br />
        <b>Kohseoyoung</b>
      </a>
      <br />
      Backend
    </td>
  </tr>
</table>

</div>

## 주요 기능

| 도메인 | 기능 |
| --- | --- |
| 인증 | Google OAuth 및 이메일 기반 로그인, JWT 인증, 이메일 인증, 비밀번호 재설정 |
| 회원 | 온보딩, 프로필 조회/수정, 프로필 이미지 업로드, 공개 프로필 조회 |
| 포트폴리오 | 프로젝트 이력 등록/조회/수정/삭제, 영상 링크 기반 썸네일 관리 |
| 프로젝트 | 프로젝트 생성/조회/수정/삭제, 상태 관리, 프로젝트 고정 |
| 멤버 | 초대 링크 기반 프로젝트 참여, 멤버 역할/권한 관리 |
| 영상 | YouTube 영상 등록/검증, 진행 상태 관리, 북마크, 참고 파일 관리 |
| 피드백 | 타임코드 기반 피드백 작성, 답글, 상태 변경 및 해결 처리 |
| 공유 링크 | 외부 게스트용 영상 공유 링크 생성, 게스트 등록, 링크 활성화 관리 |
| 파일/공지 | 프로젝트 파일 업로드/다운로드, 공지 등록/조회, 읽음 처리 |
| 일정 | 개인/프로젝트 일정 관리, 캘린더 조회, 개인 메모 |
| 모집 | 구인구직 공고 등록/조회, 조건별 필터링, 추천 공고, 지원/북마크 |
| 알림 | 알림 조회/읽음 처리, 알림 설정, 최근 활동, 오늘 브리핑 |

## 기술 스택

<div align="center">

### Backend

<p>
  <img src="https://img.shields.io/badge/JAVA_21-007396?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java 21" />
  <img src="https://img.shields.io/badge/SPRING_BOOT_3.5-6DB33F?style=for-the-badge&logo=springboot&logoColor=white" alt="Spring Boot" />
  <img src="https://img.shields.io/badge/SPRING_SECURITY-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white" alt="Spring Security" />
  <img src="https://img.shields.io/badge/JWT-000000?style=for-the-badge&logo=jsonwebtokens&logoColor=white" alt="JWT" />
  <img src="https://img.shields.io/badge/SPRING_DATA_JPA-6DB33F?style=for-the-badge&logo=spring&logoColor=white" alt="Spring Data JPA" />
</p>

### Database & Storage

<p>
  <img src="https://img.shields.io/badge/MYSQL_8.0-4479A1?style=for-the-badge&logo=mysql&logoColor=white" alt="MySQL" />
  <img src="https://img.shields.io/badge/AWS_S3-569A31?style=for-the-badge&logo=amazons3&logoColor=white" alt="AWS S3" />
  <img src="https://img.shields.io/badge/H2_TEST_DB-09476B?style=for-the-badge&logo=h2database&logoColor=white" alt="H2" />
  <img src="https://img.shields.io/badge/FLYWAY-CC0200?style=for-the-badge&logo=flyway&logoColor=white" alt="Flyway" />
</p>

### Infra & Deploy

<p>
  <img src="https://img.shields.io/badge/AWS_EC2-FF9900?style=for-the-badge&logo=amazonec2&logoColor=white" alt="AWS EC2" />
  <img src="https://img.shields.io/badge/GITHUB_ACTIONS-2088FF?style=for-the-badge&logo=githubactions&logoColor=white" alt="GitHub Actions" />
  <img src="https://img.shields.io/badge/GRADLE-02303A?style=for-the-badge&logo=gradle&logoColor=white" alt="Gradle" />
  <img src="https://img.shields.io/badge/DOCKER-2496ED?style=for-the-badge&logo=docker&logoColor=white" alt="Docker" />
</p>

### API & External

<p>
  <img src="https://img.shields.io/badge/SWAGGER_UI-85EA2D?style=for-the-badge&logo=swagger&logoColor=black" alt="Swagger UI" />
  <img src="https://img.shields.io/badge/GOOGLE_OAUTH-4285F4?style=for-the-badge&logo=google&logoColor=white" alt="Google OAuth" />
  <img src="https://img.shields.io/badge/YOUTUBE_DATA_API-FF0000?style=for-the-badge&logo=youtube&logoColor=white" alt="YouTube Data API" />
</p>

</div>

## 시스템 구성

<img width="1381" height="1139" alt="slat_to_infra" src="https://github.com/user-attachments/assets/106b2f50-cd4d-45f4-a3f6-f9981d427967" />


## 협업 방식

### 브랜치 전략

| 브랜치 | 설명 |
| --- | --- |
| `main` | 최종 사용자에게 배포되는 가장 안정적인 버전입니다. |
| `develop` | 다음 출시 버전을 개발하는 중심 브랜치입니다. 기능 개발이 완료된 `feature` 브랜치가 병합됩니다. |
| `feature/*` | 기능 개발용 브랜치입니다. 최신 `develop`에서 분기하여 작업합니다. |

### 브랜치 규칙 및 네이밍

- 모든 기능 개발은 `feature` 브랜치에서 시작합니다.
- 작업을 시작하기 전에는 `develop` 브랜치의 최신 내용을 반영합니다.
- 작업이 끝나면 `develop`을 대상으로 Pull Request를 생성하고, Reviewer를 지정합니다.
- 리뷰 후 승인된 Pull Request만 `develop`에 병합합니다.

```bash
git switch develop
git pull origin develop
git switch -c feature/1-login
```

브랜치 이름은 아래 형식을 사용합니다.

```text
feature/{이슈번호}-{기능명}
```

예시: `feature/1-login`

### 커밋 컨벤션

```text
<type>: <작업 내용>
```

- `type`은 소문자만 사용합니다.
- `subject`는 현재형 동사로 작성합니다.

| type       | 설명                     |
| ---------- | ------------------------ |
| `feat`     | 새로운 기능 추가         |
| `fix`      | 버그 수정                |
| `refactor` | 기능 변경 없는 코드 개선 |
| `docs`     | 문서 수정                |
| `test`     | 테스트 코드 추가/수정    |
| `chore`    | 빌드, 설정, 기타 작업    |
| `deploy`   | 배포 및 인프라 관련 작업 |

### Pull Request

- PR은 `develop` 브랜치를 기준으로 생성합니다.
- PR 생성 시 Reviewer를 지정합니다.
- 작업 내용, 주요 변경 파일, 검증 결과를 함께 작성합니다.
- 기능 단위가 커질 경우 작은 커밋 단위로 나누어 리뷰 흐름을 명확하게 유지합니다.

## 프로젝트 구조 및 실행

### 패키지 구조

```text
src/main/java/com/slatto
├── domain
│   ├── auth, user, project, video, feedback
│   ├── schedule, notification, recruitment, sharelink, inquiry
│   └── controller, service, repository, entity, dto 중심으로 구성
├── global
│   ├── config, security, exception, response
│   ├── storage, health
│   └── 공통 설정과 횡단 관심사 관리
└── SlattoApplication.java
```

### 로컬 실행 및 검증

Java 21 기준으로 실행합니다.

```bash
./gradlew bootRun
```

전체 테스트와 빌드는 아래 명령으로 확인합니다.

```bash
./gradlew test
./gradlew clean build
```

---

<div align="center">
  SLAT-TO Backend
</div>
