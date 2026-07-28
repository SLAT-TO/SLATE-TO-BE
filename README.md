<!-- @format -->

# SLATE-TO Backend

<div align="center">

<!-- TODO: 배너 이미지를 docs/assets/readme-banner.png 경로에 추가한 뒤 아래 주석을 해제합니다. -->
<!-- <img width="100%" alt="SLATE-TO Backend Banner" src="./docs/assets/readme-banner.png" /> -->
<img width="1920" height="1080" alt="Frame 2147229304" src="https://github.com/user-attachments/assets/fbe510e1-01c9-4b1a-b400-38a5297a1efa" />

<h3>영상 제작 협업을 위한 프로젝트 관리 및 피드백 플랫폼</h3>

</div>

---

## 프로젝트 소개

SLATE-TO는 영상 제작자와 클라이언트가 하나의 프로젝트 공간에서 작업물을 공유하고, 피드백을 남기며, 일정과 파일을 관리할 수 있도록 돕는 협업 서비스입니다.

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

| 도메인   | 기능                                                                       |
| -------- | -------------------------------------------------------------------------- |
| 인증     | Google OAuth 로그인, JWT 기반 인증, 토큰 재발급                            |
| 프로젝트 | 프로젝트 생성/수정/삭제, 목록/상세 조회, 프로젝트 고정                     |
| 멤버     | 초대 링크 생성/조회/수락, 멤버 목록/상세 조회, 역할 수정, 멤버 삭제/나가기 |
| 영상     | YouTube URL 기반 영상 등록, 영상 목록/상세 조회, 진행 상태 관리            |
| 피드백   | 영상 타임코드 기반 피드백, 답글, 해결 처리                                 |
| 파일     | 프로젝트 파일 업로드/다운로드, 검색, 수정/삭제, 파일 고정                  |
| 공지     | 프로젝트 공지 등록/조회/수정/삭제, 읽음 처리                               |
| 일정     | 개인 일정과 프로젝트 일정 관리                                             |
| 알림     | 프로젝트 활동 기반 알림 조회 및 읽음 처리                                  |

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
</p>

### Infra & Deploy

<p>
  <img src="https://img.shields.io/badge/AWS_EC2-FF9900?style=for-the-badge&logo=amazonec2&logoColor=white" alt="AWS EC2" />
  <img src="https://img.shields.io/badge/GITHUB_ACTIONS-2088FF?style=for-the-badge&logo=githubactions&logoColor=white" alt="GitHub Actions" />
  <img src="https://img.shields.io/badge/GRADLE-02303A?style=for-the-badge&logo=gradle&logoColor=white" alt="Gradle" />
</p>

### API & External

<p>
  <img src="https://img.shields.io/badge/SWAGGER_UI-85EA2D?style=for-the-badge&logo=swagger&logoColor=black" alt="Swagger UI" />
  <img src="https://img.shields.io/badge/GOOGLE_OAUTH-4285F4?style=for-the-badge&logo=google&logoColor=white" alt="Google OAuth" />
  <img src="https://img.shields.io/badge/YOUTUBE_DATA_API-FF0000?style=for-the-badge&logo=youtube&logoColor=white" alt="YouTube Data API" />
</p>

</div>

## 시스템 구성

<img width="1309" height="1201" alt="slat_infra" src="https://github.com/user-attachments/assets/97a66797-34f1-4ce4-96a1-aea0913b504b" />

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
  SLATE-TO Backend
</div>
