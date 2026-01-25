package kr.io.team.uxlog.dto

import java.time.LocalDate

data class ProjectStatistics(
    val projectId: Long,
    val projectName: String,
    val totalPageViews: Long,
    val totalUniqueVisitors: Long,
    val totalEmails: Long,
    val conversionRate: Double,
    val channelStats: List<ChannelStatistics>
)

data class ChannelStatistics(
    val channel: String,
    val pageViews: Long,
    val uniqueVisitors: Long,
    val emails: Long,
    val conversionRate: Double
)

// 일별 통계
data class DailyStatistics(
    val date: LocalDate,
    val pageViews: Long,
    val uniqueVisitors: Long,
    val emails: Long,
    val conversionRate: Double
)

// postNumber별 통계
data class PostStatistics(
    val postNumber: String,
    val pageViews: Long,
    val uniqueVisitors: Long,
    val emails: Long,
    val conversionRate: Double
)

// 일별 + postNumber별 통계
data class DailyPostStatistics(
    val date: LocalDate,
    val postNumber: String,
    val pageViews: Long,
    val uniqueVisitors: Long,
    val emails: Long,
    val conversionRate: Double
)

// 확장된 통계 응답 (일별, postNumber별 포함)
data class DetailedProjectStatistics(
    val summary: ProjectStatistics,
    val dailyStats: List<DailyStatistics>,
    val postStats: List<PostStatistics>
)
