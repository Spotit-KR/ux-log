package kr.io.team.uxlog.service

import kr.io.team.uxlog.domain.EmailSubscription
import kr.io.team.uxlog.domain.PageView
import kr.io.team.uxlog.domain.Project
import kr.io.team.uxlog.repository.EmailSubscriptionRepository
import kr.io.team.uxlog.repository.PageViewRepository
import kr.io.team.uxlog.repository.ProjectRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional
import kotlin.test.assertEquals

@SpringBootTest
@Transactional
class StatisticsServiceTest {

    @Autowired
    lateinit var statisticsService: StatisticsService

    @Autowired
    lateinit var projectRepository: ProjectRepository

    @Autowired
    lateinit var pageViewRepository: PageViewRepository

    @Autowired
    lateinit var emailSubscriptionRepository: EmailSubscriptionRepository

    private lateinit var project: Project

    @BeforeEach
    fun setUp() {
        project = projectRepository.save(Project(name = "Test Project"))

        // 페이지 뷰 데이터
        pageViewRepository.save(PageView(project = project, channel = "thread"))
        pageViewRepository.save(PageView(project = project, channel = "thread"))
        pageViewRepository.save(PageView(project = project, channel = "thread"))
        pageViewRepository.save(PageView(project = project, channel = "instagram"))
        pageViewRepository.save(PageView(project = project, channel = "instagram"))

        // 이메일 데이터
        emailSubscriptionRepository.save(EmailSubscription(project = project, channel = "thread", email = "a@test.com"))
        emailSubscriptionRepository.save(EmailSubscription(project = project, channel = "instagram", email = "b@test.com"))
    }

    @Test
    fun `프로젝트 통계를 조회할 수 있다`() {
        val stats = statisticsService.getProjectStatistics(project.id)

        assertEquals(project.id, stats.projectId)
        assertEquals("Test Project", stats.projectName)
        assertEquals(5, stats.totalPageViews)
        assertEquals(2, stats.totalEmails)
        assertEquals(40.0, stats.conversionRate) // 2/5 * 100
    }

    @Test
    fun `채널별 통계를 조회할 수 있다`() {
        val stats = statisticsService.getProjectStatistics(project.id)

        assertEquals(2, stats.channelStats.size)

        val threadStats = stats.channelStats.find { it.channel == "thread" }!!
        assertEquals(3, threadStats.pageViews)
        assertEquals(1, threadStats.emails)

        val instagramStats = stats.channelStats.find { it.channel == "instagram" }!!
        assertEquals(2, instagramStats.pageViews)
        assertEquals(1, instagramStats.emails)
    }
}
