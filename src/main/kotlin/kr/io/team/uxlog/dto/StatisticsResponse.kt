package kr.io.team.uxlog.dto

data class ProjectStatistics(
    val projectId: Long,
    val projectName: String,
    val totalPageViews: Long,
    val totalEmails: Long,
    val conversionRate: Double,
    val channelStats: List<ChannelStatistics>
)

data class ChannelStatistics(
    val channel: String,
    val pageViews: Long,
    val emails: Long,
    val conversionRate: Double
)
