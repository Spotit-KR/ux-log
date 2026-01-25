package kr.io.team.uxlog.controller

import kr.io.team.uxlog.service.EmailService
import kr.io.team.uxlog.service.ProjectService
import kr.io.team.uxlog.service.StatisticsService
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam

@Controller
@RequestMapping("/admin")
class AdminViewController(
    private val projectService: ProjectService,
    private val statisticsService: StatisticsService,
    private val emailService: EmailService
) {

    @GetMapping
    fun dashboard(model: Model): String {
        val projects = projectService.getAllProjects()
        val projectStats = projects.map { project ->
            statisticsService.getProjectStatistics(project.id)
        }

        val totalPageViews = projectStats.sumOf { it.totalPageViews }
        val totalUniqueVisitors = projectStats.sumOf { it.totalUniqueVisitors }
        val totalEmails = projectStats.sumOf { it.totalEmails }

        model.addAttribute("projects", projects)
        model.addAttribute("projectStats", projectStats)
        model.addAttribute("totalPageViews", totalPageViews)
        model.addAttribute("totalUniqueVisitors", totalUniqueVisitors)
        model.addAttribute("totalEmails", totalEmails)
        model.addAttribute("projectCount", projects.size)

        return "admin/dashboard"
    }

    @GetMapping("/projects")
    fun projects(model: Model): String {
        val projects = projectService.getAllProjects()
        model.addAttribute("projects", projects)
        return "admin/projects"
    }

    @GetMapping("/projects/{id}")
    fun projectDetail(
        @PathVariable id: Long,
        @RequestParam(defaultValue = "30") days: Int,
        model: Model
    ): String {
        val project = projectService.getProject(id)
        val stats = statisticsService.getProjectStatistics(id)
        val dailyStats = statisticsService.getDailyStatistics(id, days)
        val postStats = statisticsService.getPostStatistics(id)

        model.addAttribute("project", project)
        model.addAttribute("stats", stats)
        model.addAttribute("dailyStats", dailyStats)
        model.addAttribute("postStats", postStats)
        model.addAttribute("days", days)

        return "admin/project-detail"
    }

    @GetMapping("/projects/{id}/emails")
    fun projectEmails(@PathVariable id: Long, model: Model): String {
        val project = projectService.getProject(id)
        val emails = emailService.getEmailsByProjectId(id)

        model.addAttribute("project", project)
        model.addAttribute("emails", emails)

        return "admin/emails"
    }
}
