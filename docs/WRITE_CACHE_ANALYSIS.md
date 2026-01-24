# 쓰기 캐시 구현 방식 분석

트래킹 API의 쓰기 성능을 향상시키기 위한 버퍼링/캐시 구현 방식을 분석합니다.

## 환경 정보

- 서버에 Valkey 9.0.1 (Redis 호환) 설치됨
- Spring Boot 3.x + Kotlin
- PostgreSQL

---

## 구현 방식 비교

### 1. 인메모리 버퍼 (ConcurrentLinkedQueue + @Scheduled)

순수 Java/Kotlin으로 구현하는 가장 단순한 방식.

```kotlin
@Service
class TrackingService {
    private val buffer = ConcurrentLinkedQueue<PageView>()

    fun track(...) {
        buffer.add(pageView)
    }

    @Scheduled(fixedRate = 5000)
    fun flush() {
        val batch = mutableListOf<PageView>()
        while (buffer.poll()?.also { batch.add(it) } != null) {}
        if (batch.isNotEmpty()) repository.saveAll(batch)
    }
}
```

| 장점 | 단점 |
|------|------|
| 추가 의존성 없음 | 서버 재시작 시 데이터 유실 |
| 구현 매우 단순 | 멀티 인스턴스 환경에서 문제 |
| 오버헤드 최소 | 메모리 사용량 증가 가능 |
| 지연 시간 최소 | 버퍼 크기 관리 필요 |

**적합한 경우:** 단일 인스턴스, 데이터 유실 허용 가능

---

### 2. Valkey/Redis 버퍼 (LPUSH + RPOP)

Valkey List를 버퍼(큐)로 사용하여 배치 처리.

#### 동작 원리

```
[API 요청] → [LPUSH] → [Valkey List] → [RPOP] → [배치 INSERT] → [PostgreSQL]
                            ↑
                      메모리에 임시 저장
                      (서버 재시작해도 유지)
```

**핵심 개념:** Valkey List는 양방향 연결 리스트로, 왼쪽에서 넣고(LPUSH) 오른쪽에서 빼면(RPOP) FIFO 큐가 됩니다.

#### 사용되는 Valkey 명령어

| 명령어 | 용도 | 시간복잡도 | 설명 |
|--------|------|------------|------|
| `LPUSH key value` | 쓰기 | O(1) | 리스트 왼쪽에 데이터 추가 |
| `RPOP key` | 읽기 | O(1) | 리스트 오른쪽에서 데이터 추출 (제거됨) |
| `LRANGE key 0 99` | 배치 읽기 | O(N) | 여러 개 한번에 조회 (제거 안됨) |
| `LTRIM key 100 -1` | 배치 삭제 | O(N) | 조회한 데이터 삭제 |
| `LLEN key` | 모니터링 | O(1) | 현재 버퍼에 쌓인 개수 |

#### 데이터 흐름 상세

```
1. 트래킹 요청 도착 (10:00:00.000)
   POST /api/track { projectId: 1, channel: "google", ... }

2. JSON 직렬화 후 LPUSH (10:00:00.001)
   > LPUSH tracking:buffer '{"projectId":1,"channel":"google",...}'
   (integer) 1

3. 추가 요청들 도착 (10:00:00.002 ~ 10:00:04.999)
   > LPUSH tracking:buffer '{"projectId":1,"channel":"naver",...}'
   > LPUSH tracking:buffer '{"projectId":2,"channel":"direct",...}'
   ...
   버퍼 상태: [새로운것] ← ... ← [오래된것]

4. 스케줄러 실행 (10:00:05.000, 5초마다)
   > LRANGE tracking:buffer -100 -1  # 오래된 100개 조회
   > LTRIM tracking:buffer 0 -101    # 조회한 100개 삭제

5. PostgreSQL 배치 INSERT
   INSERT INTO page_view (project_id, channel, ...) VALUES
     (1, 'google', ...),
     (1, 'naver', ...),
     (2, 'direct', ...);
```

#### 한 번에 한 쓰기? 배치 쓰기?

**쓰기 (LPUSH):** 요청당 1회 실행 (개별 쓰기)
- 각 트래킹 요청마다 즉시 LPUSH 호출
- Valkey는 인메모리라 매우 빠름 (~0.1ms)
- 네트워크 왕복 1회만 발생

**읽기 (배치 처리):** 스케줄러가 주기적으로 한꺼번에 처리
- 5초마다 쌓인 데이터를 모아서 처리
- PostgreSQL INSERT는 배치로 1회만 실행

```
시간축:
0s      1s      2s      3s      4s      5s      6s
|-------|-------|-------|-------|-------|-------|
 ↓↓↓↓↓↓  ↓↓↓↓↓↓  ↓↓↓↓↓↓  ↓↓↓↓↓↓  ↓↓↓↓↓↓  ↓↓↓↓↓↓
 LPUSH들 (각각 개별 실행)              │
                                      ▼
                              LRANGE + LTRIM
                              배치 INSERT (1회)
```

#### 구현 코드 상세

```kotlin
@Service
class TrackingService(
    private val redisTemplate: StringRedisTemplate,
    private val objectMapper: ObjectMapper
) {
    private val bufferKey = "tracking:buffer"

    // API 호출 시 - 개별 LPUSH (매우 빠름)
    fun track(projectId: Long, channel: String, postNumber: String?,
              ipAddress: String?, userAgent: String?) {
        val data = TrackingData(projectId, channel, postNumber,
                                ipAddress, userAgent, Instant.now())
        val json = objectMapper.writeValueAsString(data)

        redisTemplate.opsForList().leftPush(bufferKey, json)
        // 여기서 반환 - DB 접근 없음, ~1ms 이내
    }
}

@Component
class TrackingBatchProcessor(
    private val redisTemplate: StringRedisTemplate,
    private val pageViewRepository: PageViewRepository,
    private val objectMapper: ObjectMapper
) {
    private val bufferKey = "tracking:buffer"
    private val batchSize = 100

    // 5초마다 실행 - 배치 처리
    @Scheduled(fixedRate = 5000)
    fun flush() {
        val ops = redisTemplate.opsForList()

        while (true) {
            // 1. 오래된 것부터 최대 100개 조회
            val items = ops.range(bufferKey, -batchSize.toLong(), -1)
                        ?: break
            if (items.isEmpty()) break

            // 2. 역직렬화
            val pageViews = items.mapNotNull { json ->
                try {
                    val data = objectMapper.readValue(json, TrackingData::class.java)
                    PageView(
                        project = Project(id = data.projectId),
                        channel = data.channel,
                        postNumber = data.postNumber,
                        ipAddress = data.ipAddress,
                        userAgent = data.userAgent,
                        createdAt = data.createdAt
                    )
                } catch (e: Exception) {
                    null // 파싱 실패 시 스킵
                }
            }

            // 3. 배치 INSERT
            if (pageViews.isNotEmpty()) {
                pageViewRepository.saveAll(pageViews)
            }

            // 4. 처리한 데이터 삭제
            ops.trim(bufferKey, 0, -(items.size + 1).toLong())

            // 5. 처리할 게 batchSize보다 적으면 종료
            if (items.size < batchSize) break
        }
    }
}

data class TrackingData(
    val projectId: Long,
    val channel: String,
    val postNumber: String?,
    val ipAddress: String?,
    val userAgent: String?,
    val createdAt: Instant
)
```

#### 장애 상황 대응

| 상황 | 동작 | 데이터 유실 |
|------|------|------------|
| 애플리케이션 재시작 | Valkey에 데이터 유지, 재시작 후 처리 | 없음 |
| Valkey 재시작 | 메모리 데이터 손실 (RDB/AOF로 복구 가능) | 설정에 따라 다름 |
| PostgreSQL 장애 | 버퍼에 계속 쌓임, DB 복구 후 처리 | 없음 |
| 배치 처리 중 오류 | 트랜잭션 롤백, 다음 주기에 재시도 | 없음 |

| 장점 | 단점 |
|------|------|
| 서버 재시작해도 데이터 유지 | Redis 의존성 추가 |
| 멀티 인스턴스 환경 지원 | 네트워크 오버헤드 (약간) |
| Valkey 이미 설치됨 | 직렬화/역직렬화 비용 |
| 버퍼 모니터링 용이 (LLEN) | Valkey 장애 시 쓰기 불가 |
| O(1) 쓰기 성능 | |

**적합한 경우:** 멀티 인스턴스, 데이터 유실 최소화 필요

---

### 3. Valkey/Redis Pub/Sub

이벤트 기반으로 구독자가 배치 처리.

```kotlin
// 발행
redisTemplate.convertAndSend("tracking:events", pageViewJson)

// 구독 (별도 리스너)
@RedisListener("tracking:events")
fun onTrackingEvent(message: String) {
    buffer.add(deserialize(message))
}
```

| 장점 | 단점 |
|------|------|
| 이벤트 기반 아키텍처 | 구독자 없으면 메시지 유실 |
| 실시간 처리 가능 | 메시지 지속성 없음 |
| 느슨한 결합 | 복잡도 증가 |

**적합한 경우:** 이벤트 기반 아키텍처가 이미 있는 경우

---

### 4. Valkey/Redis Stream

Redis 5.0+ Stream을 사용한 메시지 큐 방식.

```kotlin
// 쓰기
redisTemplate.opsForStream().add("tracking:stream", mapOf("data" to json))

// 소비 (Consumer Group)
@Scheduled(fixedRate = 5000)
fun consume() {
    val messages = redisTemplate.opsForStream()
        .read(Consumer.from("group", "consumer"),
              StreamOffset.create("tracking:stream", ReadOffset.lastConsumed()))
    // 배치 처리 후 ACK
}
```

| 장점 | 단점 |
|------|------|
| 메시지 지속성 | 구현 복잡도 높음 |
| Consumer Group으로 분산 처리 | 오버엔지니어링 가능성 |
| 메시지 재처리 가능 (ACK) | 학습 곡선 |
| 정확히 한 번 처리 보장 | |

**적합한 경우:** 대규모 트래픽, 메시지 유실 절대 불가

---

### 5. Spring Data Redis + Cache Abstraction

Spring의 캐시 추상화 사용 (읽기 캐시에 더 적합).

| 장점 | 단점 |
|------|------|
| Spring 표준 방식 | 쓰기 버퍼링에는 부적합 |
| 애노테이션 기반 | 커스터마이징 제한적 |

**적합한 경우:** 읽기 캐시가 필요한 경우

---

## 권장 방식

### 이 프로젝트에 가장 적합: **Valkey List 버퍼 (방식 2)**

**이유:**
1. **Valkey 이미 설치됨** - 추가 인프라 불필요
2. **적절한 복잡도** - 인메모리보다 안정적, Stream보다 단순
3. **데이터 유실 최소화** - 서버 재시작해도 버퍼 유지
4. **확장성** - 향후 멀티 인스턴스 배포 가능
5. **모니터링 용이** - `LLEN`으로 버퍼 크기 확인

### 필요한 의존성

```kotlin
// build.gradle.kts
implementation("org.springframework.boot:spring-boot-starter-data-redis")
```

### 예상 성능 향상

| 항목 | Before | After |
|------|--------|-------|
| DB INSERT/초 | 100회 (개별) | 1회 (배치 100건) |
| DB 연결 사용 | 100회 | 1회 |
| 응답 시간 | ~50ms | ~5ms |

---

## 구현 계획

1. `spring-boot-starter-data-redis` 의존성 추가
2. Redis 연결 설정 (`application.properties`)
3. `TrackingService` 수정 - Valkey List에 LPUSH
4. `TrackingBatchProcessor` 생성 - 주기적으로 배치 INSERT
5. 테스트 및 모니터링 설정
