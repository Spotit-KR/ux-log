package kr.io.team.uxlog.service

import kr.io.team.uxlog.domain.Project
import kr.io.team.uxlog.repository.ProjectRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class ProjectService(
    private val projectRepository: ProjectRepository
) {

    fun createProject(name: String, description: String? = null, waitingOffset: Long = 0): Project {
        return projectRepository.save(Project(name = name, description = description, waitingOffset = waitingOffset))
    }

    @Transactional(readOnly = true)
    fun getAllProjects(): List<Project> {
        return projectRepository.findAll()
    }

    @Transactional(readOnly = true)
    fun getProject(id: Long): Project {
        return projectRepository.findById(id)
            .orElseThrow { IllegalArgumentException("Project not found: $id") }
    }

    fun updateWaitingOffset(id: Long, waitingOffset: Long): Project {
        val project = getProject(id)
        val updated = Project(
            id = project.id,
            name = project.name,
            description = project.description,
            waitingOffset = waitingOffset,
            createdAt = project.createdAt
        )
        return projectRepository.save(updated)
    }
}
