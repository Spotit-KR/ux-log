package kr.io.team.uxlog.dto

data class ProjectRequest(
    val name: String,
    val description: String? = null,
    val waitingOffset: Long = 0
)
