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
}
