# SLAT-TO Backend Repository

> 슬레이투 백엔드 레포지토리입니다.

## Team Members


## 🛠️ 기술 스택 (Tech Stack)

- Java 21
- Spring Boot 3.5.15
- Gradle - Groovy
- MySQL
- Spring Data JPA
- Spring Web
- Validation
- Lombok
- Swagger / Springdoc OpenAPI

## 🚀 로컬 실행 방법

### 1. IntelliJ에서 실행

1. IntelliJ IDEA에서 프로젝트를 엽니다.
2. Project SDK를 Java 21로 설정합니다.
3. Gradle Sync를 실행합니다.
4. Run Configuration의 Environment variables에 DB 환경변수를 설정합니다.
5. `SlattoApplication` main class를 실행합니다.

### 2. 필수 환경변수

DB 접속 대상은 아직 로컬 MySQL 또는 RDS 중 확정되지 않았습니다.
현재는 MySQL 사용을 전제로 환경변수만 열어둡니다.

```bash
DB_URL=jdbc:mysql://<host>:3306/<database>?serverTimezone=Asia/Seoul&characterEncoding=UTF-8
DB_USER=<username>
DB_PASSWORD=<password>
JPA_DDL_AUTO=validate
```

환경변수 예시는 `.env.example`을 참고합니다. Spring Boot는 `.env` 파일을 자동으로 읽지 않으므로 IntelliJ Run Configuration 또는 shell 환경변수로 값을 주입합니다.

### 3. Swagger

애플리케이션 실행 후 아래 주소에서 Swagger UI를 확인할 수 있습니다.

```txt
http://localhost:8080/swagger-ui
```

## 🧩 초기 패키지 방향

```txt
com.slatto
├── global
│   └── config
└── domain
```

공통 응답 형식, 예외 처리 방식, Security/Auth 구조는 팀 합의 후 추가합니다.

### 🌐 Git-flow 전략 (Git-flow Strategy)

- **`main`**: 최종적으로 사용자에게 배포되는 가장 안정적인 버전 브랜치
- **`develop`**: 다음 출시 버전을 개발하는 중심 브랜치. 기능 개발 완료 후 `feature` 브랜치들이 병합
- **`feature`**: 기능 개발용 브랜치. `develop`에서 분기하여 작업

### 📌 브랜치 규칙 및 네이밍 (Branch Rules & Naming)

1. 모든 기능 개발은 **feature** 브랜치에서 시작
2. 작업 시작 전, 항상 최신 `develop` 내용 받아오기 (`git pull origin develop`)
3. 작업 완료 후, `develop`으로 Pull Request(PR) 생성
4. PR에 Reviewer(멘션) 지정
5. 리뷰어가 PR을 보고 코드 확인 후 머지 진행

**브랜치 이름 형식:**  
feature/이슈번호-기능명

- 예시: `feature/1-login`

### 🎯 커밋 컨벤션 (Commit Convention)

- **주의 사항:**
- `type`은 소문자만 사용 (feat, fix, refactor, docs, style, test, chore)
- `subject`는 **모두 현재형 동사**

#### 📋 타입 목록

| type                | 설명                                  |
| :------------------ | :------------------------------------ |
| `start`             | 새로운 프로젝트를 시작할 때           |
| `feat`              | 새로운 기능을 추가할 때               |
| `fix`               | 버그를 수정할 때                      |
| `design`            | CSS 등 사용자 UI 디자인을 변경할 때   |
| `refactor`          | 기능 변경 없이 코드를 리팩토링할 때   |
| `settings`          | 설정 파일을 변경할 때                 |
| `comment`           | 필요한 주석을 추가하거나 변경할 때    |
| `dependency/Plugin` | 의존성/플러그인을 추가할 때           |
| `docs`              | README.md 등 문서를 수정할 때         |
| `merge`             | 브랜치를 병합할 때                    |
| `deploy`            | 빌드 및 배포 관련 작업을 할 때        |
| `rename`            | 파일 혹은 폴더명을 수정하거나 옮길 때 |
| `remove`            | 파일을 삭제하는 작업만 수행했을 때    |
| `revert`            | 이전 버전으로 롤백할 때               |

```bash
#### ✨ 예시
feat: 로그인 기능 추가
fix: 로그인 버그 수정
refactor: 로그인 로직 리팩토링
```
