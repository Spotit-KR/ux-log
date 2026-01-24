package kr.io.team.uxlog

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class UxLogApplication

fun main(args: Array<String>) {
    runApplication<UxLogApplication>(*args)
}
