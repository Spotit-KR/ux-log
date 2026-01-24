# UX-Log

랜딩페이지의 유입 채널, 페이지 방문 수, 이메일 전환율을 수집하고 분석하는 백엔드 API 서비스

## 기술 스택

- Kotlin 2.3.0
- Spring Boot 4.0.2
- JPA + PostgreSQL
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
```

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

#### 이메일 CSV 내보내기

```
GET /api/admin/projects/{id}/emails/export
```

---

## 랜딩페이지 연동 예시

```html
<script>
  // 페이지 로드 시 방문 기록
  fetch('https://your-server.com/api/track?projectId=1&channel=thread&postNumber=42');

  // 이메일 폼 제출
  document.getElementById('emailForm').addEventListener('submit', async (e) => {
    e.preventDefault();
    const email = document.getElementById('email').value;

    await fetch('https://your-server.com/api/email', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        projectId: 1,
        email: email,
        channel: 'thread',
        postNumber: '42'
      })
    });

    alert('구독 완료!');
  });
</script>
```

---

## 관리자 페이지

| 경로 | 설명 |
|------|------|
| `/admin` | 대시보드 (전체 요약) |
| `/admin/projects` | 프로젝트 목록 |
| `/admin/projects/{id}` | 프로젝트 상세 통계 |
| `/admin/projects/{id}/emails` | 이메일 목록 + CSV 내보내기 |
