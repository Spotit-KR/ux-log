# UX-Log 구현 계획

## 개요

랜딩페이지의 유입 채널, 링크 클릭 수, 이메일 입력 전환율을 수집하기 위한 백엔드 API 시스템

## 기술 스택

- Spring Boot 4.0.2
- Kotlin 2.3.0
- JPA + PostgreSQL
- Thymeleaf (관리자 페이지)
- JUnit 5 (테스트)

---

## 1. 도메인 모델

### Project (프로젝트/랜딩페이지)

| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long | PK |
| name | String | 프로젝트명 |
| description | String? | 설명 |
| createdAt | LocalDateTime | 생성일시 |

### PageView (페이지 방문 기록)

| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long | PK |
| project | Project | FK - 프로젝트 |
| channel | String | 유입 채널 (thread, instagram 등) |
| postNumber | String? | 게시물 번호 |
| ipAddress | String? | 방문자 IP |
| userAgent | String? | 브라우저 정보 |
| createdAt | LocalDateTime | 방문 일시 |

### EmailSubscription (이메일 구독)

| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long | PK |
| project | Project | FK - 프로젝트 |
| channel | String? | 유입 채널 |
| postNumber | String? | 게시물 번호 |
| email | String | 이메일 주소 |
| createdAt | LocalDateTime | 등록 일시 |

---

## 2. API 설계

### 트래킹 API

#### GET /api/track
페이지 방문 기록

**Query Parameters:**
- `projectId` (required): 프로젝트 ID
- `channel` (required): 유입 채널
- `postNumber` (optional): 게시물 번호

**Response:** 204 No Content

**사용 예시:**
```
GET /api/track?projectId=1&channel=thread&postNumber=42
```

### 이메일 API

#### POST /api/email
이메일 수집

**Request Body:**
```json
{
  "projectId": 1,
  "channel": "thread",
  "postNumber": "42",
  "email": "user@example.com"
}
```

**Response:** 201 Created

### 관리자 API

#### GET /api/admin/projects
프로젝트 목록 조회

#### POST /api/admin/projects
프로젝트 생성

#### GET /api/admin/projects/{id}/stats
프로젝트 통계 조회

**Response:**
```json
{
  "totalPageViews": 1500,
  "totalEmails": 45,
  "conversionRate": 3.0,
  "channelStats": [
    {"channel": "thread", "pageViews": 800, "emails": 30},
    {"channel": "instagram", "pageViews": 700, "emails": 15}
  ]
}
```

#### GET /api/admin/projects/{id}/emails
이메일 목록 조회 (CSV 내보내기 지원)

---

## 3. 패키지 구조

```
kr.io.team.uxlog
├── domain/
│   ├── Project.kt
│   ├── PageView.kt
│   └── EmailSubscription.kt
├── repository/
│   ├── ProjectRepository.kt
│   ├── PageViewRepository.kt
│   └── EmailSubscriptionRepository.kt
├── service/
│   ├── TrackingService.kt
│   ├── EmailService.kt
│   └── StatisticsService.kt
├── controller/
│   ├── TrackingController.kt
│   ├── EmailController.kt
│   └── AdminController.kt
├── dto/
│   ├── EmailRequest.kt
│   ├── ProjectRequest.kt
│   └── StatisticsResponse.kt
└── config/
    └── WebConfig.kt (CORS 설정)
```

---

## 4. 관리자 페이지 (Thymeleaf)

### 페이지 구성

| 경로 | 설명 |
|------|------|
| /admin | 대시보드 (전체 요약) |
| /admin/projects | 프로젝트 목록 |
| /admin/projects/{id} | 프로젝트 상세 통계 |
| /admin/projects/{id}/emails | 이메일 목록 |

### 주요 기능

1. **대시보드**
   - 전체 프로젝트 수
   - 총 방문 수 / 총 이메일 수
   - 최근 7일 추이

2. **프로젝트 상세**
   - 채널별 방문 수 표
   - 이메일 전환율
   - 일별 방문 추이 차트

3. **이메일 관리**
   - 이메일 목록 테이블
   - CSV 내보내기 버튼

---

## 5. 개발 순서 (TDD)

### Phase 1: 기반 구축
1. [ ] Entity 클래스 생성
2. [ ] Repository 인터페이스 생성
3. [ ] Repository 테스트 작성 및 통과

### Phase 2: 핵심 서비스
4. [ ] TrackingService 테스트 작성
5. [ ] TrackingService 구현
6. [ ] EmailService 테스트 작성
7. [ ] EmailService 구현
8. [ ] StatisticsService 테스트 작성
9. [ ] StatisticsService 구현

### Phase 3: API 계층
10. [ ] TrackingController 테스트 작성
11. [ ] TrackingController 구현
12. [ ] EmailController 테스트 작성
13. [ ] EmailController 구현
14. [ ] AdminController 구현

### Phase 4: 관리자 UI
15. [ ] Thymeleaf 레이아웃 구성
16. [ ] 대시보드 페이지
17. [ ] 프로젝트 상세 페이지
18. [ ] 이메일 목록 페이지

### Phase 5: 설정 및 마무리
19. [ ] CORS 설정
20. [ ] application.properties 구성
21. [ ] 통합 테스트

---

## 6. 추가 고려사항

### CORS 설정
- 모든 외부 도메인에서 `/api/track`, `/api/email` 호출 허용

### 중복 방문 정책
- 현재는 모든 방문을 기록 (추후 IP + 시간 기반 중복 제거 가능)

### 인덱스
- `page_view`: (project_id, channel, created_at)
- `email_subscription`: (project_id, created_at)

### 보안
- 관리자 페이지는 추후 인증 추가 가능 (현재는 미구현)
