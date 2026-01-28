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

    @Test
    fun `잘못된 이메일 형식은 예외가 발생한다`() {
        val exception = assertThrows<IllegalArgumentException> {
            emailService.subscribe(projectId = project.id, email = "invalid-email")
        }
        assertEquals("Invalid email format", exception.message)
    }

    @Test
    fun `빈 이메일은 예외가 발생한다`() {
        val exception = assertThrows<IllegalArgumentException> {
            emailService.subscribe(projectId = project.id, email = "")
        }
        assertEquals("Email is required", exception.message)
    }

    @Test
    fun `공백만 있는 이메일은 예외가 발생한다`() {
        val exception = assertThrows<IllegalArgumentException> {
            emailService.subscribe(projectId = project.id, email = "   ")
        }
        assertEquals("Email is required", exception.message)
    }

    @Test
    fun `골뱅이가 없는 이메일은 예외가 발생한다`() {
        assertThrows<IllegalArgumentException> {
            emailService.subscribe(projectId = project.id, email = "userexample.com")
        }
    }

    @Test
    fun `도메인이 없는 이메일은 예외가 발생한다`() {
        assertThrows<IllegalArgumentException> {
            emailService.subscribe(projectId = project.id, email = "user@")
        }
    }

    @Test
    fun `대기자 수를 조회할 수 있다`() {
        emailService.subscribe(projectId = project.id, email = "user1@example.com")
        emailService.subscribe(projectId = project.id, email = "user2@example.com")

        val waitingCount = emailService.getWaitingCount(project.id)

        assertEquals(2, waitingCount)
    }

    @Test
    fun `대기자 수는 waitingOffset과 이메일 수의 합이다`() {
        val projectWithOffset = projectRepository.save(Project(name = "Project with offset", waitingOffset = 78))
        emailService.subscribe(projectId = projectWithOffset.id, email = "user1@example.com")
        emailService.subscribe(projectId = projectWithOffset.id, email = "user2@example.com")

        val waitingCount = emailService.getWaitingCount(projectWithOffset.id)

        assertEquals(80, waitingCount) // 78 + 2
    }

    @Test
    fun `존재하지 않는 프로젝트의 대기자 수를 조회하면 예외가 발생한다`() {
        assertThrows<IllegalArgumentException> {
            emailService.getWaitingCount(9999)
        }
    }
}
