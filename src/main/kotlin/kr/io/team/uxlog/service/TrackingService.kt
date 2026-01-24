package kr.io.team.uxlog.service

import tools.jackson.databind.ObjectMapper
import kr.io.team.uxlog.config.TrackingBufferProperties
import kr.io.team.uxlog.dto.TrackingData
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service

@Service
class TrackingService(
    private val redisTemplate: StringRedisTemplate,
    private val objectMapper: ObjectMapper,
    private val bufferProperties: TrackingBufferProperties
) {

    fun trackPageView(
        projectId: Long,
        channel: String,
        postNumber: String? = null,
        ipAddress: String? = null,
        userAgent: String? = null
    ) {
        val data = TrackingData(
            projectId = projectId,
            channel = channel,
            postNumber = postNumber,
            ipAddress = ipAddress,
            userAgent = userAgent
        )
        val json = objectMapper.writeValueAsString(data)
        redisTemplate.opsForList().leftPush(bufferProperties.key, json)
    }
}
