package kr.io.team.uxlog.service

import kr.io.team.uxlog.dto.*
import kr.io.team.uxlog.repository.EmailSubscriptionRepository
import kr.io.team.uxlog.repository.PageViewRepository
import kr.io.team.uxlog.repository.ProjectRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime

@Service
@Transactional(readOnly = true)
class StatisticsService(
    private val projectRepository: ProjectRepository,
    private val pageViewRepository: PageViewRepository,
    private val emailSubscriptionRepository: EmailSubscriptionRepository
) {

    fun getProjectStatistics(projectId: Long): ProjectStatistics {
        val project = projectRepository.findById(projectId)
            .orElseThrow { IllegalArgumentException("Project not found: $projectId") }

        val totalPageViews = pageViewRepository.countByProjectId(projectId)
        val totalUniqueVisitors = pageViewRepository.countUniqueVisitorsByProjectId(projectId)
        val totalEmails = emailSubscriptionRepository.countByProjectId(projectId)

        val pageViewsByChannel: Map<String, Long> = pageViewRepository.countByProjectIdGroupByChannel(projectId)
            .associate { (it[0] as String) to (it[1] as Long) }

        val uniqueVisitorsByChannel: Map<String, Long> = pageViewRepository.countUniqueVisitorsByProjectIdGroupByChannel(projectId)
            .associate { (it[0] as String) to (it[1] as Long) }

        val emailsByChannel: Map<String, Long> = emailSubscriptionRepository.countByProjectIdGroupByChannel(projectId)
            .associate { ((it[0] as String?) ?: "direct") to (it[1] as Long) }

        val allChannels = pageViewsByChannel.keys + emailsByChannel.keys

        val channelStats = allChannels.map { channel ->
            val views = pageViewsByChannel[channel] ?: 0L
            val visitors = uniqueVisitorsByChannel[channel] ?: 0L
            val emails = emailsByChannel[channel] ?: 0L
            ChannelStatistics(
                channel = channel,
                pageViews = views,
                uniqueVisitors = visitors,
                emails = emails,
                conversionRate = calculateConversionRate(visitors, emails)
            )
        }

        return ProjectStatistics(
            projectId = project.id,
            projectName = project.name,
            totalPageViews = totalPageViews,
            totalUniqueVisitors = totalUniqueVisitors,
            totalEmails = totalEmails,
            conversionRate = calculateConversionRate(totalUniqueVisitors, totalEmails),
            channelStats = channelStats
        )
    }

    fun getDailyStatistics(projectId: Long, days: Int = 30): List<DailyStatistics> {
        val startDate = LocalDateTime.now().minusDays(days.toLong())

        val pageViewStats = pageViewRepository.getDailyStatsByProjectId(projectId, startDate)
            .associate { (it[0] as LocalDate) to Pair(it[1] as Long, it[2] as Long) }

        val emailStats = emailSubscriptionRepository.getDailyCountByProjectId(projectId, startDate)
            .associate { (it[0] as LocalDate) to (it[1] as Long) }

        val allDates = pageViewStats.keys + emailStats.keys

        return allDates.map { date ->
            val (pageViews, uniqueVisitors) = pageViewStats[date] ?: Pair(0L, 0L)
            val emails = emailStats[date] ?: 0L
            DailyStatistics(
                date = date,
                pageViews = pageViews,
                uniqueVisitors = uniqueVisitors,
                emails = emails,
                conversionRate = calculateConversionRate(uniqueVisitors, emails)
            )
        }.sortedByDescending { it.date }
    }

    fun getPostStatistics(projectId: Long): List<PostStatistics> {
        val pageViewStats = pageViewRepository.getStatsByProjectIdGroupByPostNumber(projectId)
            .associate { (it[0] as String) to Pair(it[1] as Long, it[2] as Long) }

        val emailStats = emailSubscriptionRepository.countByProjectIdGroupByPostNumber(projectId)
            .associate { (it[0] as String) to (it[1] as Long) }

        val allPostNumbers = pageViewStats.keys + emailStats.keys

        return allPostNumbers.map { postNumber ->
            val (pageViews, uniqueVisitors) = pageViewStats[postNumber] ?: Pair(0L, 0L)
            val emails = emailStats[postNumber] ?: 0L
            PostStatistics(
                postNumber = postNumber,
                pageViews = pageViews,
                uniqueVisitors = uniqueVisitors,
                emails = emails,
                conversionRate = calculateConversionRate(uniqueVisitors, emails)
            )
        }.sortedByDescending { it.pageViews }
    }

    fun getDetailedStatistics(projectId: Long, days: Int = 30): DetailedProjectStatistics {
        return DetailedProjectStatistics(
            summary = getProjectStatistics(projectId),
            dailyStats = getDailyStatistics(projectId, days),
            postStats = getPostStatistics(projectId)
        )
    }

    fun getDailyPostStatistics(projectId: Long, days: Int = 30): List<DailyPostStatistics> {
        val startDate = LocalDateTime.now().minusDays(days.toLong())

        val pageViewStats = pageViewRepository.getDailyStatsByProjectIdGroupByPostNumber(projectId, startDate)
            .map {
                Triple(
                    it[0] as LocalDate,
                    it[1] as String,
                    Pair(it[2] as Long, it[3] as Long)
                )
            }
            .associateBy { Pair(it.first, it.second) }
            .mapValues { it.value.third }

        val emailStats = emailSubscriptionRepository.getDailyCountByProjectIdGroupByPostNumber(projectId, startDate)
            .associate { Pair(it[0] as LocalDate, it[1] as String) to (it[2] as Long) }

        val allKeys = pageViewStats.keys + emailStats.keys

        return allKeys.map { (date, postNumber) ->
            val (pageViews, uniqueVisitors) = pageViewStats[Pair(date, postNumber)] ?: Pair(0L, 0L)
            val emails = emailStats[Pair(date, postNumber)] ?: 0L
            DailyPostStatistics(
                date = date,
                postNumber = postNumber,
                pageViews = pageViews,
                uniqueVisitors = uniqueVisitors,
                emails = emails,
                conversionRate = calculateConversionRate(uniqueVisitors, emails)
            )
        }.sortedWith(compareByDescending<DailyPostStatistics> { it.date }.thenByDescending { it.pageViews })
    }

    private fun calculateConversionRate(visitors: Long, emails: Long): Double {
        return if (visitors > 0) {
            (emails.toDouble() / visitors * 100)
        } else {
            0.0
        }
    }
}
