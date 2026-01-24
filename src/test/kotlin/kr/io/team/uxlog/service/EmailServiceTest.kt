package kr.io.team.uxlog.service

import kr.io.team.uxlog.domain.Project
import kr.io.team.uxlog.repository.EmailSubscriptionRepository
import kr.io.team.uxlog.repository.ProjectRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@SpringBootTest
@Transactional
class EmailServiceTest {

    @Autowired
    lateinit var emailService: EmailService

    @Autowired
    lateinit var projectRepository: ProjectRepository

    @Autowired
    lateinit var emailSubscriptionRepository: EmailSubscriptionRepository

    private lateinit var project: Project

    @BeforeEach
    fun setUp() {
        project = projectRepository.save(Project(name = "Test Project"))
    }

    @Test
    fun `이메일을 저장할 수 있다`() {
        val result = emailService.subscribe(
            projectId = project.id,
            email = "user@example.com",
            channel = "thread",
            postNumber = "42"
        )

        assertTrue(result)
        val count = emailSubscriptionRepository.countByProjectId(project.id)
        assertEquals(1, count)
    }

    @Test
    fun `존재하지 않는 프로젝트에 이메일을 저장하면 예외가 발생한다`() {
        assertThrows<IllegalArgumentException> {
            emailService.subscribe(
                projectId = 9999,
                email = "user@example.com"
            )
        }
    }

    @Test
    fun `중복된 이메일은 저장되지 않는다`() {
        emailService.subscribe(projectId = project.id, email = "user@example.com")
        val result = emailService.subscribe(projectId = project.id, email = "user@example.com")

        assertFalse(result)
        val count = emailSubscriptionRepository.countByProjectId(project.id)
        assertEquals(1, count)
    }

    @Test
    fun `프로젝트별 이메일 목록을 조회할 수 있다`() {
        emailService.subscribe(projectId = project.id, email = "user1@example.com")
        emailService.subscribe(projectId = project.id, email = "user2@example.com")

        val emails = emailService.getEmailsByProjectId(project.id)
        assertEquals(2, emails.size)
    }
}
