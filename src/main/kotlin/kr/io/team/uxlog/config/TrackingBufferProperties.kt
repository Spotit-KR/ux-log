package kr.io.team.uxlog.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "tracking.buffer")
class TrackingBufferProperties {
    var key: String = "tracking:buffer"
    var batchSize: Int = 100
    var flushInterval: Long = 5000
}
