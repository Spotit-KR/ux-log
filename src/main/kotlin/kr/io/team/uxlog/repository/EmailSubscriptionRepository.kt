package kr.io.team.uxlog.repository

import kr.io.team.uxlog.domain.EmailSubscription
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

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
}
