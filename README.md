# UX-Log

랜딩페이지의 유입 채널, 페이지 방문 수, 이메일 전환율을 수집하고 분석하는 백엔드 API 서비스

## 기술 스택

- Kotlin 2.3.0
- Spring Boot 4.0.2
- JPA + PostgreSQL
- Valkey/Redis (트래킹 버퍼)
- Thymeleaf (관리자 페이지)

## 시작하기

### 1. PostgreSQL 실행

```bash
docker compose up -d
```

### 2. 애플리케이션 실행

```bash
./gradlew bootRun
```

### 3. 관리자 페이지 접속

http://localhost:8080/admin

## 환경 변수

| 변수 | 설명 | 기본값 |
|------|------|--------|
| `DB_URL` | PostgreSQL 접속 URL | `jdbc:postgresql://localhost:5432/uxlog` |
| `DB_USERNAME` | DB 사용자명 | `postgres` |
| `DB_PASSWORD` | DB 비밀번호 | `postgres` |
| `ADMIN_USERNAME` | 관리자 계정 | `admin` |
| `ADMIN_PASSWORD` | 관리자 비밀번호 | `admin` |
| `REDIS_HOST` | Redis/Valkey 호스트 | `localhost` |
| `REDIS_PORT` | Redis/Valkey 포트 | `6379` |
| `REDIS_PASSWORD` | Redis/Valkey 비밀번호 | (없음) |

## 인증

- `/api/track`, `/api/email`: 인증 없이 접근 가능 (랜딩페이지에서 호출)
- `/admin/**`, `/api/admin/**`: 로그인 필요

기본 관리자 계정: `admin` / `admin`

운영 환경에서는 반드시 `ADMIN_USERNAME`, `ADMIN_PASSWORD` 환경 변수를 설정하세요.

---

## API 문서

### 트래킹 API

#### 페이지 방문 기록

랜딩페이지 로드 시 호출하여 방문을 기록합니다.

```
GET /api/track
```

**Query Parameters:**

| 파라미터 | 필수 | 설명 |
|----------|------|------|
| `projectId` | O | 프로젝트 ID |
| `channel` | O | 유입 채널 (thread, instagram, twitter 등) |
| `postNumber` | X | 게시물 번호 |

**Example:**

```bash
curl "http://localhost:8080/api/track?projectId=1&channel=thread&postNumber=42"
```

**Response:** `204 No Content`

**내부 동작:**

트래킹 API는 성능 향상을 위해 Valkey/Redis 버퍼를 사용합니다.

1. API 호출 시 데이터를 Valkey List에 LPUSH (즉시 응답, ~1ms)
2. 백그라운드 스케줄러가 5초마다 버퍼에서 데이터를 꺼내 PostgreSQL에 배치 INSERT
3. Valkey 연결 실패 시 자동으로 직접 DB INSERT로 폴백

```
[API 요청] → [Valkey LPUSH] → [배치 스케줄러] → [PostgreSQL]
                  ↓ (실패 시)
            [직접 DB INSERT]
```

> **Note:** 버퍼링으로 인해 통계 반영에 최대 5초의 지연이 발생할 수 있습니다.

---

### 이메일 API

#### 이메일 수집

사용자가 이메일을 입력했을 때 호출합니다.

```
POST /api/email
```

**Request Body:**

```json
{
  "projectId": 1,
  "email": "user@example.com",
  "channel": "thread",
  "postNumber": "42"
}
```

| 필드 | 필수 | 설명 |
|------|------|------|
| `projectId` | O | 프로젝트 ID |
| `email` | O | 이메일 주소 |
| `channel` | X | 유입 채널 |
| `postNumber` | X | 게시물 번호 |

**Example:**

```bash
curl -X POST "http://localhost:8080/api/email" \
  -H "Content-Type: application/json" \
  -d '{"projectId": 1, "email": "user@example.com", "channel": "thread"}'
```

**Response:**

```json
// 신규 등록 (201 Created)
{"success": true, "message": "Subscribed"}

// 이미 등록됨 (200 OK)
{"success": true, "message": "Already subscribed"}

// 에러 (400 Bad Request)
{"success": false, "message": "Invalid email format"}
```

**제약사항:**
- 이메일 형식 검증 (유효한 이메일만 허용)
- 빈 문자열 불가
- 프로젝트별 동일 이메일 중복 등록 불가
- 존재하지 않는 프로젝트 ID는 에러

---

### 관리자 API

#### 프로젝트 목록 조회

```
GET /api/admin/projects
```

**Response:**

```json
[
  {
    "id": 1,
    "name": "test1",
    "description": "테스트 프로젝트",
    "createdAt": "2024-01-01T00:00:00"
  }
]
```

#### 프로젝트 생성

```
POST /api/admin/projects
```

**Request Body:**

```json
{
  "name": "my-project",
  "description": "프로젝트 설명"
}
```

#### 프로젝트 통계 조회

```
GET /api/admin/projects/{id}/stats
```

**Response:**

```json
{
  "projectId": 1,
  "projectName": "test1",
  "totalPageViews": 150,
  "totalEmails": 12,
  "conversionRate": 8.0,
  "channelStats": [
    {
      "channel": "thread",
      "pageViews": 100,
      "emails": 8,
      "conversionRate": 8.0
    },
    {
      "channel": "instagram",
      "pageViews": 50,
      "emails": 4,
      "conversionRate": 8.0
    }
  ]
}
```

#### 이메일 목록 조회

```
GET /api/admin/projects/{id}/emails
```

**Response:**

```json
[
  {
    "id": 1,
    "email": "user@example.com",
    "channel": "thread",
    "postNumber": "42",
    "createdAt": "2024-01-01T00:00:00"
  }
]
```

#### 이메일 CSV 내보내기

```
GET /api/admin/projects/{id}/emails/export
```

---

## 랜딩페이지 연동 예시

랜딩페이지 URL에 query parameter를 포함하여 유입 채널을 추적합니다.

**랜딩페이지 URL 예시:**
```
https://your-landing.com?channel=thread&postNumber=42
https://your-landing.com?channel=instagram&postNumber=123
```

이 query parameter들을 읽어서 트래킹 API에 전달합니다.

```html
<script>
  const PROJECT_ID = 1; // 프로젝트 ID
  const API_BASE = 'https://your-uxlog-server.com';

  // URL에서 query parameter 읽기
  const params = new URLSearchParams(window.location.search);
  const channel = params.get('channel') || 'direct';
  const postNumber = params.get('postNumber') || '';

  // 페이지 로드 시 방문 기록
  fetch(`${API_BASE}/api/track?projectId=${PROJECT_ID}&channel=${channel}&postNumber=${postNumber}`);

  // 이메일 폼 제출
  document.getElementById('emailForm').addEventListener('submit', async (e) => {
    e.preventDefault();
    const email = document.getElementById('email').value;

    await fetch(`${API_BASE}/api/email`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        projectId: PROJECT_ID,
        email: email,
        channel: channel,
        postNumber: postNumber
      })
    });

    alert('구독 완료!');
  });
</script>
```

**활용 예시:**
- 스레드 게시물: `https://your-landing.com?channel=thread&postNumber=1`
- 인스타그램: `https://your-landing.com?channel=instagram&postNumber=abc123`
- 트위터: `https://your-landing.com?channel=twitter`
- 직접 유입: `https://your-landing.com` (channel이 없으면 'direct'로 기록)

---

## 관리자 페이지

| 경로 | 설명 |
|------|------|
| `/admin` | 대시보드 (전체 요약) |
| `/admin/projects` | 프로젝트 목록 |
| `/admin/projects/{id}` | 프로젝트 상세 통계 |
| `/admin/projects/{id}/emails` | 이메일 목록 + CSV 내보내기 |
