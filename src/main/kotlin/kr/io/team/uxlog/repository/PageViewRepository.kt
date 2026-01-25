package kr.io.team.uxlog.repository

import kr.io.team.uxlog.domain.PageView
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.LocalDateTime

interface PageViewRepository : JpaRepository<PageView, Long> {

    fun countByProjectId(projectId: Long): Long

    fun countByProjectIdAndChannel(projectId: Long, channel: String): Long

    @Query("""
        SELECT pv.channel, COUNT(pv)
        FROM PageView pv
        WHERE pv.project.id = :projectId
        GROUP BY pv.channel
    """)
    fun countByProjectIdGroupByChannel(projectId: Long): List<Array<Any>>

    @Query("""
        SELECT CAST(pv.createdAt AS LocalDate), COUNT(pv)
        FROM PageView pv
        WHERE pv.project.id = :projectId
          AND pv.createdAt >= :startDate
        GROUP BY CAST(pv.createdAt AS LocalDate)
        ORDER BY CAST(pv.createdAt AS LocalDate)
    """)
    fun countByProjectIdAndCreatedAtAfterGroupByDate(
        projectId: Long,
        startDate: LocalDateTime
    ): List<Array<Any>>

    // 고유 방문자 수 (visitorId 우선, 없으면 IP+UA 조합으로 fallback)
    @Query("""
        SELECT COUNT(DISTINCT COALESCE(pv.visitorId, CONCAT(pv.ipAddress, ':', pv.userAgent)))
        FROM PageView pv
        WHERE pv.project.id = :projectId
    """)
    fun countUniqueVisitorsByProjectId(projectId: Long): Long

    // 채널별 고유 방문자 수
    @Query("""
        SELECT pv.channel, COUNT(DISTINCT COALESCE(pv.visitorId, CONCAT(pv.ipAddress, ':', pv.userAgent)))
        FROM PageView pv
        WHERE pv.project.id = :projectId
        GROUP BY pv.channel
    """)
    fun countUniqueVisitorsByProjectIdGroupByChannel(projectId: Long): List<Array<Any>>

    // 일별 통계 (PV, UV)
    @Query("""
        SELECT CAST(pv.createdAt AS LocalDate) as date,
               COUNT(pv) as pageViews,
               COUNT(DISTINCT COALESCE(pv.visitorId, CONCAT(pv.ipAddress, ':', pv.userAgent))) as uniqueVisitors
        FROM PageView pv
        WHERE pv.project.id = :projectId
          AND pv.createdAt >= :startDate
        GROUP BY CAST(pv.createdAt AS LocalDate)
        ORDER BY CAST(pv.createdAt AS LocalDate) DESC
    """)
    fun getDailyStatsByProjectId(projectId: Long, startDate: LocalDateTime): List<Array<Any>>

    // postNumber별 통계 (PV, UV)
    @Query("""
        SELECT pv.postNumber,
               COUNT(pv) as pageViews,
               COUNT(DISTINCT COALESCE(pv.visitorId, CONCAT(pv.ipAddress, ':', pv.userAgent))) as uniqueVisitors
        FROM PageView pv
        WHERE pv.project.id = :projectId
          AND pv.postNumber IS NOT NULL
        GROUP BY pv.postNumber
        ORDER BY COUNT(pv) DESC
    """)
    fun getStatsByProjectIdGroupByPostNumber(projectId: Long): List<Array<Any>>

    // 일별 + postNumber별 통계
    @Query("""
        SELECT CAST(pv.createdAt AS LocalDate) as date,
               pv.postNumber,
               COUNT(pv) as pageViews,
               COUNT(DISTINCT COALESCE(pv.visitorId, CONCAT(pv.ipAddress, ':', pv.userAgent))) as uniqueVisitors
        FROM PageView pv
        WHERE pv.project.id = :projectId
          AND pv.createdAt >= :startDate
          AND pv.postNumber IS NOT NULL
        GROUP BY CAST(pv.createdAt AS LocalDate), pv.postNumber
        ORDER BY CAST(pv.createdAt AS LocalDate) DESC, COUNT(pv) DESC
    """)
    fun getDailyStatsByProjectIdGroupByPostNumber(projectId: Long, startDate: LocalDateTime): List<Array<Any>>

    // 특정 postNumber의 일별 통계
    @Query("""
        SELECT CAST(pv.createdAt AS LocalDate) as date,
               COUNT(pv) as pageViews,
               COUNT(DISTINCT COALESCE(pv.visitorId, CONCAT(pv.ipAddress, ':', pv.userAgent))) as uniqueVisitors
        FROM PageView pv
        WHERE pv.project.id = :projectId
          AND pv.postNumber = :postNumber
          AND pv.createdAt >= :startDate
        GROUP BY CAST(pv.createdAt AS LocalDate)
        ORDER BY CAST(pv.createdAt AS LocalDate) DESC
    """)
    fun getDailyStatsByProjectIdAndPostNumber(
        projectId: Long,
        postNumber: String,
        startDate: LocalDateTime
    ): List<Array<Any>>
}
