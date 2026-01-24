package kr.io.team.uxlog.repository

import kr.io.team.uxlog.domain.EmailSubscription
import kr.io.team.uxlog.domain.PageView
import kr.io.team.uxlog.domain.Project
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@SpringBootTest
@Transactional
class RepositoryTest {

    @Autowired
    lateinit var projectRepository: ProjectRepository

    @Autowired
    lateinit var pageViewRepository: PageViewRepository

    @Autowired
    lateinit var emailSubscriptionRepository: EmailSubscriptionRepository

    private lateinit var project: Project

    @BeforeEach
    fun setUp() {
        project = projectRepository.save(Project(name = "Test Project", description = "Test Description"))
    }

    @Test
    fun `프로젝트를 저장하고 조회할 수 있다`() {
        val found = projectRepository.findById(project.id)
        assertTrue(found.isPresent)
        assertEquals("Test Project", found.get().name)
    }

    @Test
    fun `페이지 뷰를 저장하고 프로젝트별로 카운트할 수 있다`() {
        pageViewRepository.save(PageView(project = project, channel = "thread", postNumber = "1"))
        pageViewRepository.save(PageView(project = project, channel = "thread", postNumber = "2"))
        pageViewRepository.save(PageView(project = project, channel = "instagram"))

        val totalCount = pageViewRepository.countByProjectId(project.id)
        assertEquals(3, totalCount)

        val threadCount = pageViewRepository.countByProjectIdAndChannel(project.id, "thread")
        assertEquals(2, threadCount)
    }

    @Test
    fun `페이지 뷰를 채널별로 그룹화하여 카운트할 수 있다`() {
        pageViewRepository.save(PageView(project = project, channel = "thread"))
        pageViewRepository.save(PageView(project = project, channel = "thread"))
        pageViewRepository.save(PageView(project = project, channel = "instagram"))

        val stats = pageViewRepository.countByProjectIdGroupByChannel(project.id)
        assertEquals(2, stats.size)

        val channelMap = stats.associate { it[0] as String to it[1] as Long }
        assertEquals(2L, channelMap["thread"])
        assertEquals(1L, channelMap["instagram"])
    }

    @Test
    fun `이메일 구독을 저장하고 프로젝트별로 조회할 수 있다`() {
        emailSubscriptionRepository.save(
            EmailSubscription(project = project, channel = "thread", email = "user1@example.com")
        )
        emailSubscriptionRepository.save(
            EmailSubscription(project = project, channel = "instagram", email = "user2@example.com")
        )

        val emails = emailSubscriptionRepository.findByProjectId(project.id)
        assertEquals(2, emails.size)

        val count = emailSubscriptionRepository.countByProjectId(project.id)
        assertEquals(2, count)
    }

    @Test
    fun `이메일 중복 여부를 확인할 수 있다`() {
        emailSubscriptionRepository.save(
            EmailSubscription(project = project, email = "existing@example.com")
        )

        assertTrue(emailSubscriptionRepository.existsByProjectIdAndEmail(project.id, "existing@example.com"))
        assertFalse(emailSubscriptionRepository.existsByProjectIdAndEmail(project.id, "new@example.com"))
    }

    @Test
    fun `이메일 구독을 채널별로 그룹화하여 카운트할 수 있다`() {
        emailSubscriptionRepository.save(EmailSubscription(project = project, channel = "thread", email = "a@test.com"))
        emailSubscriptionRepository.save(EmailSubscription(project = project, channel = "thread", email = "b@test.com"))
        emailSubscriptionRepository.save(EmailSubscription(project = project, channel = "instagram", email = "c@test.com"))

        val stats = emailSubscriptionRepository.countByProjectIdGroupByChannel(project.id)
        assertEquals(2, stats.size)

        val channelMap = stats.associate { it[0] as String to it[1] as Long }
        assertEquals(2L, channelMap["thread"])
        assertEquals(1L, channelMap["instagram"])
    }
}
