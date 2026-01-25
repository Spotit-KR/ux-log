package kr.io.team.uxlog.repository

import kr.io.team.uxlog.domain.EmailSubscription
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.LocalDateTime

interface EmailSubscriptionRepository : JpaRepository<EmailSubscription, Long> {

    fun findByProjectId(projectId: Long): List<EmailSubscription>

    fun countByProjectId(projectId: Long): Long

    fun countByProjectIdAndChannel(projectId: Long, channel: String): Long

    @Query("""
        SELECT es.channel, COUNT(es)
        FROM EmailSubscription es
        WHERE es.project.id = :projectId
        GROUP BY es.channel
    """)
    fun countByProjectIdGroupByChannel(projectId: Long): List<Array<Any>>

    fun existsByProjectIdAndEmail(projectId: Long, email: String): Boolean

    // 일별 이메일 구독 수
    @Query("""
        SELECT CAST(es.createdAt AS LocalDate) as date, COUNT(es)
        FROM EmailSubscription es
        WHERE es.project.id = :projectId
          AND es.createdAt >= :startDate
        GROUP BY CAST(es.createdAt AS LocalDate)
        ORDER BY CAST(es.createdAt AS LocalDate) DESC
    """)
    fun getDailyCountByProjectId(projectId: Long, startDate: LocalDateTime): List<Array<Any>>

    // postNumber별 이메일 구독 수
    @Query("""
        SELECT es.postNumber, COUNT(es)
        FROM EmailSubscription es
        WHERE es.project.id = :projectId
          AND es.postNumber IS NOT NULL
        GROUP BY es.postNumber
        ORDER BY COUNT(es) DESC
    """)
    fun countByProjectIdGroupByPostNumber(projectId: Long): List<Array<Any>>

    // 일별 + postNumber별 이메일 구독 수
    @Query("""
        SELECT CAST(es.createdAt AS LocalDate) as date, es.postNumber, COUNT(es)
        FROM EmailSubscription es
        WHERE es.project.id = :projectId
          AND es.createdAt >= :startDate
          AND es.postNumber IS NOT NULL
        GROUP BY CAST(es.createdAt AS LocalDate), es.postNumber
        ORDER BY CAST(es.createdAt AS LocalDate) DESC
    """)
    fun getDailyCountByProjectIdGroupByPostNumber(projectId: Long, startDate: LocalDateTime): List<Array<Any>>
}
