package kr.io.team.uxlog.dto

data class EmailRequest(
    val projectId: Long,
    val email: String,
    val channel: String? = null,
    val postNumber: String? = null
)
