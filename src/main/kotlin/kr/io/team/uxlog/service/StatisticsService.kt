package kr.io.team.uxlog.service

import kr.io.team.uxlog.dto.ChannelStatistics
import kr.io.team.uxlog.dto.ProjectStatistics
import kr.io.team.uxlog.repository.EmailSubscriptionRepository
import kr.io.team.uxlog.repository.PageViewRepository
import kr.io.team.uxlog.repository.ProjectRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

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
        val totalEmails = emailSubscriptionRepository.countByProjectId(projectId)

        val pageViewsByChannel: Map<String, Long> = pageViewRepository.countByProjectIdGroupByChannel(projectId)
            .associate { (it[0] as String) to (it[1] as Long) }

        val emailsByChannel: Map<String, Long> = emailSubscriptionRepository.countByProjectIdGroupByChannel(projectId)
            .associate { ((it[0] as String?) ?: "direct") to (it[1] as Long) }

        val allChannels = pageViewsByChannel.keys + emailsByChannel.keys

        val channelStats = allChannels.map { channel ->
            val views = pageViewsByChannel[channel] ?: 0L
            val emails = emailsByChannel[channel] ?: 0L
            ChannelStatistics(
                channel = channel,
                pageViews = views,
                emails = emails,
                conversionRate = calculateConversionRate(views, emails)
            )
        }

        return ProjectStatistics(
            projectId = project.id,
            projectName = project.name,
            totalPageViews = totalPageViews,
            totalEmails = totalEmails,
            conversionRate = calculateConversionRate(totalPageViews, totalEmails),
            channelStats = channelStats
        )
    }

    private fun calculateConversionRate(pageViews: Long, emails: Long): Double {
        return if (pageViews > 0) {
            (emails.toDouble() / pageViews * 100)
        } else {
            0.0
        }
    }
}
