package kr.io.team.uxlog.domain

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "email_subscription",
    indexes = [
        Index(name = "idx_email_subscription_project", columnList = "project_id"),
        Index(name = "idx_email_subscription_created_at", columnList = "created_at")
    ]
)
class EmailSubscription(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    val project: Project,

    val channel: String? = null,

    val postNumber: String? = null,

    @Column(nullable = false)
    val email: String,

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
