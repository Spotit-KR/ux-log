package kr.io.team.uxlog.dto

import java.time.LocalDateTime

data class TrackingData(
    val projectId: Long,
    val channel: String,
    val postNumber: String?,
    val visitorId: String?,
    val ipAddress: String?,
    val userAgent: String?,
    val createdAt: LocalDateTime = LocalDateTime.now()
)
