package kr.io.team.uxlog.service

import kr.io.team.uxlog.domain.Project
import kr.io.team.uxlog.repository.ProjectRepository
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional
import kotlin.test.assertEquals

@SpringBootTest
@Transactional
class ProjectServiceTest {

    @Autowired
    lateinit var projectService: ProjectService

    @Autowired
    lateinit var projectRepository: ProjectRepository

    @Test
    fun `프로젝트를 생성할 수 있다`() {
        val project = projectService.createProject("Test Project", "Description")

        assertEquals("Test Project", project.name)
        assertEquals("Description", project.description)
        assertEquals(0, project.waitingOffset)
    }

    @Test
    fun `waitingOffset과 함께 프로젝트를 생성할 수 있다`() {
        val project = projectService.createProject("Test Project", "Description", 78)

        assertEquals("Test Project", project.name)
        assertEquals(78, project.waitingOffset)
    }

    @Test
    fun `프로젝트의 waitingOffset을 수정할 수 있다`() {
        val project = projectRepository.save(Project(name = "Test Project", waitingOffset = 0))

        val updated = projectService.updateWaitingOffset(project.id, 100)

        assertEquals(100, updated.waitingOffset)
        assertEquals(project.name, updated.name)
    }

    @Test
    fun `존재하지 않는 프로젝트의 waitingOffset을 수정하면 예외가 발생한다`() {
        assertThrows<IllegalArgumentException> {
            projectService.updateWaitingOffset(9999, 100)
        }
    }

    @Test
    fun `모든 프로젝트를 조회할 수 있다`() {
        projectRepository.save(Project(name = "Project 1"))
        projectRepository.save(Project(name = "Project 2"))

        val projects = projectService.getAllProjects()

        assertEquals(2, projects.size)
    }

    @Test
    fun `프로젝트를 ID로 조회할 수 있다`() {
        val saved = projectRepository.save(Project(name = "Test Project", waitingOffset = 50))

        val project = projectService.getProject(saved.id)

        assertEquals("Test Project", project.name)
        assertEquals(50, project.waitingOffset)
    }

    @Test
    fun `존재하지 않는 프로젝트를 조회하면 예외가 발생한다`() {
        assertThrows<IllegalArgumentException> {
            projectService.getProject(9999)
        }
    }
}
