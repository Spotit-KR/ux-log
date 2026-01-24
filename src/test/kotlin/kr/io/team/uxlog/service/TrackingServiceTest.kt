package kr.io.team.uxlog.service

import kr.io.team.uxlog.domain.Project
import kr.io.team.uxlog.repository.PageViewRepository
import kr.io.team.uxlog.repository.ProjectRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional
import kotlin.test.assertEquals

@SpringBootTest
@Transactional
class TrackingServiceTest {

    @Autowired
    lateinit var trackingService: TrackingService

    @Autowired
    lateinit var projectRepository: ProjectRepository

    @Autowired
    lateinit var pageViewRepository: PageViewRepository

    private lateinit var project: Project

    @BeforeEach
    fun setUp() {
        project = projectRepository.save(Project(name = "Test Project"))
    }

    @Test
    fun `페이지 방문을 기록할 수 있다`() {
        trackingService.trackPageView(
            projectId = project.id,
            channel = "thread",
            postNumber = "42",
            ipAddress = "127.0.0.1",
            userAgent = "Mozilla/5.0"
        )

        val count = pageViewRepository.countByProjectId(project.id)
        assertEquals(1, count)
    }

    @Test
    fun `존재하지 않는 프로젝트에 방문을 기록하면 예외가 발생한다`() {
        assertThrows<IllegalArgumentException> {
            trackingService.trackPageView(
                projectId = 9999,
                channel = "thread"
            )
        }
    }

    @Test
    fun `여러 번 방문을 기록할 수 있다`() {
        repeat(5) {
            trackingService.trackPageView(
                projectId = project.id,
                channel = "thread"
            )
        }

        val count = pageViewRepository.countByProjectId(project.id)
        assertEquals(5, count)
    }
}
