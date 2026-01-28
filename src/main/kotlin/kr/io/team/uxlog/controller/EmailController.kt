package kr.io.team.uxlog.controller

import kr.io.team.uxlog.dto.EmailRequest
import kr.io.team.uxlog.service.EmailService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api")
class EmailController(
    private val emailService: EmailService
) {

    @PostMapping("/email")
    fun subscribe(@RequestBody request: EmailRequest): ResponseEntity<Map<String, Any>> {
        val isNew = emailService.subscribe(
            projectId = request.projectId,
            email = request.email,
            channel = request.channel,
            postNumber = request.postNumber
        )

        return if (isNew) {
            ResponseEntity.status(HttpStatus.CREATED).body(mapOf("success" to true, "message" to "Subscribed"))
        } else {
            ResponseEntity.ok(mapOf("success" to true, "message" to "Already subscribed"))
        }
    }

    @GetMapping("/projects/{projectId}/waiting-count")
    fun getWaitingCount(@PathVariable projectId: Long): ResponseEntity<Map<String, Long>> {
        val count = emailService.getWaitingCount(projectId)
        return ResponseEntity.ok(mapOf("waitingCount" to count))
    }
}
