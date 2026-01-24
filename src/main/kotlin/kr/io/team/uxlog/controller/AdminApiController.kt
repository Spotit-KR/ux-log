package kr.io.team.uxlog.controller

import jakarta.servlet.http.HttpServletResponse
import kr.io.team.uxlog.dto.ProjectRequest
import kr.io.team.uxlog.dto.ProjectStatistics
import kr.io.team.uxlog.service.EmailService
import kr.io.team.uxlog.service.ProjectService
import kr.io.team.uxlog.service.StatisticsService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.io.PrintWriter

@RestController
@RequestMapping("/api/admin")
class AdminApiController(
    private val projectService: ProjectService,
    private val statisticsService: StatisticsService,
    private val emailService: EmailService
) {

    @GetMapping("/projects")
    fun getProjects(): ResponseEntity<List<Map<String, Any?>>> {
        val projects = projectService.getAllProjects().map {
            mapOf(
                "id" to it.id,
                "name" to it.name,
                "description" to it.description,
                "createdAt" to it.createdAt
            )
        }
        return ResponseEntity.ok(projects)
    }

    @PostMapping("/projects")
    fun createProject(@RequestBody request: ProjectRequest): ResponseEntity<Map<String, Any?>> {
        val project = projectService.createProject(request.name, request.description)
        return ResponseEntity.status(HttpStatus.CREATED).body(
            mapOf(
                "id" to project.id,
                "name" to project.name,
                "description" to project.description,
                "createdAt" to project.createdAt
            )
        )
    }

    @GetMapping("/projects/{id}/stats")
    fun getProjectStats(@PathVariable id: Long): ResponseEntity<ProjectStatistics> {
        val stats = statisticsService.getProjectStatistics(id)
        return ResponseEntity.ok(stats)
    }

    @GetMapping("/projects/{id}/emails")
    fun getProjectEmails(@PathVariable id: Long): ResponseEntity<List<Map<String, Any?>>> {
        val emails = emailService.getEmailsByProjectId(id).map {
            mapOf(
                "id" to it.id,
                "email" to it.email,
                "channel" to it.channel,
                "postNumber" to it.postNumber,
                "createdAt" to it.createdAt
            )
        }
        return ResponseEntity.ok(emails)
    }

    @GetMapping("/projects/{id}/emails/export")
    fun exportEmails(@PathVariable id: Long, response: HttpServletResponse) {
        response.contentType = "text/csv"
        response.setHeader("Content-Disposition", "attachment; filename=\"emails-project-$id.csv\"")

        val emails = emailService.getEmailsByProjectId(id)
        val writer = PrintWriter(response.outputStream)

        writer.println("email,channel,postNumber,createdAt")
        emails.forEach { email ->
            writer.println("${email.email},${email.channel ?: ""},${email.postNumber ?: ""},${email.createdAt}")
        }

        writer.flush()
    }
}
