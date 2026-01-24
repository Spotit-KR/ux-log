package kr.io.team.uxlog.controller

import jakarta.servlet.http.HttpServletRequest
import kr.io.team.uxlog.service.TrackingService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
class TrackingController(
    private val trackingService: TrackingService
) {

    @GetMapping("/track")
    fun track(
        @RequestParam projectId: Long,
        @RequestParam channel: String,
        @RequestParam(required = false) postNumber: String?,
        request: HttpServletRequest
    ): ResponseEntity<Void> {
        trackingService.trackPageView(
            projectId = projectId,
            channel = channel,
            postNumber = postNumber,
            ipAddress = request.remoteAddr,
            userAgent = request.getHeader("User-Agent")
        )
        return ResponseEntity.noContent().build()
    }
}
