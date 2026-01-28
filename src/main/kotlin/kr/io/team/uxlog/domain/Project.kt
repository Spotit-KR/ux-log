package kr.io.team.uxlog.domain

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "project")
class Project(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val name: String,

    val description: String? = null,

    @Column(nullable = false, columnDefinition = "bigint default 0")
    val waitingOffset: Long = 0,

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
