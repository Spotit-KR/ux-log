package kr.io.team.uxlog.service

import kr.io.team.uxlog.domain.EmailSubscription
import kr.io.team.uxlog.repository.EmailSubscriptionRepository
import kr.io.team.uxlog.repository.ProjectRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class EmailService(
    private val projectRepository: ProjectRepository,
    private val emailSubscriptionRepository: EmailSubscriptionRepository
) {

    fun subscribe(
        projectId: Long,
        email: String,
        channel: String? = null,
        postNumber: String? = null
    ): Boolean {
        val project = projectRepository.findById(projectId)
            .orElseThrow { IllegalArgumentException("Project not found: $projectId") }

        if (emailSubscriptionRepository.existsByProjectIdAndEmail(projectId, email)) {
            return false
        }

        emailSubscriptionRepository.save(
            EmailSubscription(
                project = project,
                email = email,
                channel = channel,
                postNumber = postNumber
            )
        )
        return true
    }

    @Transactional(readOnly = true)
    fun getEmailsByProjectId(projectId: Long): List<EmailSubscription> {
        return emailSubscriptionRepository.findByProjectId(projectId)
    }
}
