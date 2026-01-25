package kr.io.team.uxlog.domain

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "page_view",
    indexes = [
        Index(name = "idx_page_view_project_channel", columnList = "project_id, channel"),
        Index(name = "idx_page_view_created_at", columnList = "created_at"),
        Index(name = "idx_page_view_visitor_id", columnList = "visitor_id")
    ]
)
class PageView(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    val project: Project,

    @Column(nullable = false)
    val channel: String,

    val postNumber: String? = null,

    @Column(name = "visitor_id")
    val visitorId: String? = null,

    val ipAddress: String? = null,

    @Column(length = 500)
    val userAgent: String? = null,

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
