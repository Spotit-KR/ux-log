package kr.io.team.uxlog.service

import kr.io.team.uxlog.config.TrackingBufferProperties
import kr.io.team.uxlog.domain.PageView
import kr.io.team.uxlog.dto.TrackingData
import kr.io.team.uxlog.repository.PageViewRepository
import kr.io.team.uxlog.repository.ProjectRepository
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import tools.jackson.databind.ObjectMapper

@Service
class TrackingService(
    private val redisTemplate: StringRedisTemplate,
    private val objectMapper: ObjectMapper,
    private val bufferProperties: TrackingBufferProperties,
    private val projectRepository: ProjectRepository,
    private val pageViewRepository: PageViewRepository
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun trackPageView(
        projectId: Long,
        channel: String,
        postNumber: String? = null,
        visitorId: String? = null,
        ipAddress: String? = null,
        userAgent: String? = null
    ) {
        runCatching {
            TrackingData(projectId, channel, postNumber, visitorId, ipAddress, userAgent)
                .let { objectMapper.writeValueAsString(it) }
                .let { redisTemplate.opsForList().leftPush(bufferProperties.key, it) }
        }.onFailure { e ->
            logger.warn("Failed to push to Redis, falling back to direct DB insert: {}", e.message)
            saveDirectly(projectId, channel, postNumber, visitorId, ipAddress, userAgent)
        }
    }

    private fun saveDirectly(
        projectId: Long,
        channel: String,
        postNumber: String?,
        visitorId: String?,
        ipAddress: String?,
        userAgent: String?
    ) {
        val project = projectRepository.findById(projectId)
            .orElseThrow { IllegalArgumentException("Project not found: $projectId") }

        PageView(
            project = project,
            channel = channel,
            postNumber = postNumber,
            visitorId = visitorId,
            ipAddress = ipAddress,
            userAgent = userAgent
        ).also { pageViewRepository.save(it) }
    }
}
