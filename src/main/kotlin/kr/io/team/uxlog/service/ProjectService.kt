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

    fun createProject(name: String, description: String? = null): Project {
        return projectRepository.save(Project(name = name, description = description))
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
}
