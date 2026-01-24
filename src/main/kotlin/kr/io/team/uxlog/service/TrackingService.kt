package kr.io.team.uxlog.service

import kr.io.team.uxlog.domain.PageView
import kr.io.team.uxlog.repository.PageViewRepository
import kr.io.team.uxlog.repository.ProjectRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class TrackingService(
    private val projectRepository: ProjectRepository,
    private val pageViewRepository: PageViewRepository
) {

    fun trackPageView(
        projectId: Long,
        channel: String,
        postNumber: String? = null,
        ipAddress: String? = null,
        userAgent: String? = null
    ) {
        val project = projectRepository.findById(projectId)
            .orElseThrow { IllegalArgumentException("Project not found: $projectId") }

        pageViewRepository.save(
            PageView(
                project = project,
                channel = channel,
                postNumber = postNumber,
                ipAddress = ipAddress,
                userAgent = userAgent
            )
        )
    }
}
