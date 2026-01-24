package kr.io.team.uxlog.service

import kr.io.team.uxlog.config.TrackingBufferProperties
import kr.io.team.uxlog.domain.Project
import kr.io.team.uxlog.repository.PageViewRepository
import kr.io.team.uxlog.repository.ProjectRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.redis.core.ListOperations
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper
import kotlin.test.assertEquals

@SpringBootTest
@Transactional
class TrackingBatchProcessorTest {

    @Autowired
    lateinit var batchProcessor: TrackingBatchProcessor

    @Autowired
    lateinit var projectRepository: ProjectRepository

    @Autowired
    lateinit var pageViewRepository: PageViewRepository

    @Autowired
    lateinit var bufferProperties: TrackingBufferProperties

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @MockitoBean
    lateinit var redisTemplate: StringRedisTemplate

    @MockitoBean
    lateinit var listOperations: ListOperations<String, String>

    private lateinit var project: Project

    @BeforeEach
    fun setUp() {
        project = projectRepository.save(Project(name = "Test Project"))
        whenever(redisTemplate.opsForList()).thenReturn(listOperations)
    }

    @Test
    fun `버퍼에 데이터가 있으면 배치로 저장한다`() {
        val trackingJson = """
            {"projectId":${project.id},"channel":"thread","postNumber":"42","ipAddress":"127.0.0.1","userAgent":"Mozilla/5.0","createdAt":"2024-01-01T12:00:00"}
        """.trimIndent()

        whenever(listOperations.range(bufferProperties.key, -100, -1))
            .thenReturn(listOf(trackingJson))
            .thenReturn(emptyList())

        batchProcessor.flush()

        val count = pageViewRepository.countByProjectId(project.id)
        assertEquals(1, count)
    }

    @Test
    fun `버퍼가 비어있으면 아무것도 저장하지 않는다`() {
        whenever(listOperations.range(bufferProperties.key, -100, -1))
            .thenReturn(emptyList())

        batchProcessor.flush()

        val count = pageViewRepository.countByProjectId(project.id)
        assertEquals(0, count)
    }
}
