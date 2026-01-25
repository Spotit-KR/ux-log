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

        // 페이지 뷰 데이터 (visitorId로 고유 방문자 구분)
        // thread: 3 PV, 2 UV (visitor1: 2회, visitor2: 1회)
        pageViewRepository.save(PageView(project = project, channel = "thread", visitorId = "visitor1"))
        pageViewRepository.save(PageView(project = project, channel = "thread", visitorId = "visitor1"))
        pageViewRepository.save(PageView(project = project, channel = "thread", visitorId = "visitor2"))
        // instagram: 2 PV, 2 UV (visitor1: 1회, visitor3: 1회)
        pageViewRepository.save(PageView(project = project, channel = "instagram", visitorId = "visitor1"))
        pageViewRepository.save(PageView(project = project, channel = "instagram", visitorId = "visitor3"))

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
        assertEquals(3, stats.totalUniqueVisitors) // visitor1, visitor2, visitor3
        assertEquals(2, stats.totalEmails)
        // conversionRate = emails / uniqueVisitors * 100 = 2/3 * 100 ≈ 66.67
        assertEquals(66.67, stats.conversionRate, 0.01)
    }

    @Test
    fun `채널별 통계를 조회할 수 있다`() {
        val stats = statisticsService.getProjectStatistics(project.id)

        assertEquals(2, stats.channelStats.size)

        val threadStats = stats.channelStats.find { it.channel == "thread" }!!
        assertEquals(3, threadStats.pageViews)
        assertEquals(2, threadStats.uniqueVisitors) // visitor1, visitor2
        assertEquals(1, threadStats.emails)

        val instagramStats = stats.channelStats.find { it.channel == "instagram" }!!
        assertEquals(2, instagramStats.pageViews)
        assertEquals(2, instagramStats.uniqueVisitors) // visitor1, visitor3
        assertEquals(1, instagramStats.emails)
    }
}
