<!-- @format -->

# SLATE-TO Backend

<div align="center">

<img width="1920" height="1080" alt="SLATE-TO Backend Banner" src="https://github.com/user-attachments/assets/27eb1499-d6ce-4620-ae62-bab75b4e6236" />

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
  <img src="https://img.shields.io/badge/MYSQL_8.4-4479A1?style=for-the-badge&logo=mysql&logoColor=white" alt="MySQL" />
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

> **배포 실패 시 동작**
>
> 배포 후 헬스 체크가 실패하면 직전 이미지로 자동 롤백합니다. 다만 최초 배포처럼 직전 이미지 정보를 읽을 수 없으면 롤백하지 못하며, 롤백에 실패한 경우에도 배포는 실패로 기록되고 수동 복구가 필요합니다.
> `concurrency` 설정으로 배포가 동시에 실행되지 않도록 차단합니다.
> Nginx 설정 역시 `nginx -t` 검증에 실패하면 이전 설정으로 되돌립니다.

## 데이터베이스

스키마는 Flyway로 버전 관리하며, 마이그레이션 파일은 `src/main/resources/db/migration`에 있습니다.
운영 환경 반영 전 CI에서 실제 MySQL 컨테이너로 마이그레이션을 검증합니다.

## 협업 방식

### 브랜치 전략

| 브랜치 | 설명 |
| --- | --- |
| `main` | 운영 환경에 배포되는 안정적인 버전입니다. |
| `feature/*` | 기능 개발용 브랜치입니다. 최신 `main`에서 분기하여 작업합니다. |
| `fix/*` | 버그 수정용 브랜치입니다. |
| `refactor/*` | 기능 변경 없는 코드 개선용 브랜치입니다. |
| `docs/*` | 문서 수정용 브랜치입니다. |
| `chore/*` | 빌드, 설정, 기타 작업용 브랜치입니다. |

기능 개발은 `feature`를 사용하고, 나머지 접두사는 아래 커밋 컨벤션의 `type`과 같은 이름을 사용합니다.

### 브랜치 규칙 및 네이밍

- 모든 작업은 `main`에서 분기한 작업 브랜치에서 시작합니다.
- Pull Request를 생성하고 리뷰를 거친 후 `main`에 병합합니다.

```bash
git switch main
git pull origin main
git switch -c feature/1-login
```

브랜치 이름은 아래 형식을 사용합니다.

```text
feature/{이슈번호}-{기능명}
```

예시: `feature/1-login`

---

### 브랜치 전략 변경 이력

프로젝트 초기에는 Dev/Prod 서버를 분리하고 `develop`을 통합 브랜치로 두는 Git Flow 방식으로 시작했으나, 아래 이유로 `main` 중심의 단일 브랜치 전략으로 전환했습니다.

**1. 배포 대상이 하나입니다.**
Git Flow의 `develop`은 여러 릴리스를 모아 한 번에 내보내는 것을 전제로 합니다. 단일 운영 서버에 변경 사항을 지속적으로 반영하는 구조에서는 모아둘 릴리스 단위가 없어, `develop`이 `main`으로 가기 전 대기 지점 이상의 역할을 하지 못했습니다.

**2. 통합 브랜치를 검증할 환경이 없어졌습니다.**
인프라 비용 절감을 위해 Dev 서버 운영을 종료하면서, `develop`에 병합해도 동작을 확인할 수 있는 배포 환경이 사라졌습니다. "운영 반영 전에 통합 상태를 검증한다"는 통합 브랜치의 목적 자체가 성립하지 않게 되었습니다.

**3. 병합 지점이 둘로 나뉘어 리뷰가 분산되었습니다.**
같은 변경에 대해 `feature → develop` PR과 `develop → main` PR이 각각 생성되었고, 후자는 이미 리뷰를 마친 커밋의 모음이라 형식적인 승인이 되기 쉬웠습니다. 병합 지점을 하나로 모아 리뷰가 실제로 이루어지는 위치를 명확히 했습니다.

**4. 검증 조건을 한 곳에 집중할 수 있습니다.**
브랜치 보호와 CI 필수 통과 조건을 `main` 한 곳에 적용해, 운영에 반영되는 모든 코드가 예외 없이 동일한 검증을 거치도록 했습니다. 브랜치가 둘일 때는 긴급 수정을 양쪽에 각각 반영해야 하고, 한쪽을 누락하면 다음 배포에서 수정이 되돌아가는 위험도 있었습니다.

현재는 Prod 서버만 운영하며, 배포 전 검증은 Dev 서버 대신 CI에서 수행합니다. CI는 실제 MySQL 컨테이너로 Flyway 마이그레이션을 검증하고 전체 테스트와 Docker 이미지 빌드까지 확인하며, 이 검증을 통과해야만 운영 배포가 진행됩니다.

---

### 브랜치 보호 규칙

`main`은 GitHub Ruleset(`Protect main`)으로 보호되며, 예외 대상(bypass)은 없습니다.

- `main`에 직접 push할 수 없으며, 모든 변경은 Pull Request를 거칩니다.
- 승인 1건 이상이 있어야 병합할 수 있습니다.
- CI(`Migration, Build and Docker Image Check`) 통과가 병합 조건입니다.
- force push와 브랜치 삭제를 차단합니다.

---

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

---

### Pull Request

- PR은 `main` 브랜치를 기준으로 생성합니다.
- PR 생성 시 Reviewer를 지정합니다.
- 작업 내용, 주요 변경 파일, 검증 결과를 함께 작성합니다.
- 기능 단위가 커질 경우 작은 커밋 단위로 나누어 리뷰 흐름을 명확하게 유지합니다.

## 프로젝트 구조 및 실행

### 패키지 구조

```text
src/main/java/com/slatto
├── domain
│   ├── auth, user, project, video, feedback
│   ├── schedule, notification, recruitment, sharelink, inquiry, common
│   └── controller, service, repository, entity, dto 중심으로 구성
├── global
│   ├── config, security, exception, response
│   ├── storage, health, util
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
