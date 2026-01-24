package kr.io.team.uxlog.service

import tools.jackson.databind.ObjectMapper
import jakarta.persistence.EntityManager
import kr.io.team.uxlog.config.TrackingBufferProperties
import kr.io.team.uxlog.domain.PageView
import kr.io.team.uxlog.domain.Project
import kr.io.team.uxlog.dto.TrackingData
import kr.io.team.uxlog.repository.PageViewRepository
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class TrackingBatchProcessor(
    private val redisTemplate: StringRedisTemplate,
    private val pageViewRepository: PageViewRepository,
    private val objectMapper: ObjectMapper,
    private val bufferProperties: TrackingBufferProperties,
    private val entityManager: EntityManager
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedRateString = "\${tracking.buffer.flush-interval:5000}")
    @Transactional
    fun flush() {
        val ops = redisTemplate.opsForList()
        var totalProcessed = 0

        while (true) {
            val items = ops.range(bufferProperties.key, -bufferProperties.batchSize.toLong(), -1)
            if (items.isNullOrEmpty()) break

            val pageViews = items.mapNotNull { json ->
                try {
                    val data = objectMapper.readValue(json, TrackingData::class.java)
                    val projectRef = entityManager.getReference(Project::class.java, data.projectId)
                    PageView(
                        project = projectRef,
                        channel = data.channel,
                        postNumber = data.postNumber,
                        ipAddress = data.ipAddress,
                        userAgent = data.userAgent,
                        createdAt = data.createdAt
                    )
                } catch (e: Exception) {
                    logger.warn("Failed to deserialize tracking data: {}", e.message)
                    null
                }
            }

            if (pageViews.isNotEmpty()) {
                pageViewRepository.saveAll(pageViews)
                totalProcessed += pageViews.size
            }

            ops.trim(bufferProperties.key, 0, -(items.size + 1).toLong())

            if (items.size < bufferProperties.batchSize) break
        }

        if (totalProcessed > 0) {
            logger.info("Flushed {} tracking records to database", totalProcessed)
        }
    }
}
