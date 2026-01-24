package kr.io.team.uxlog.repository

import kr.io.team.uxlog.domain.Project
import org.springframework.data.jpa.repository.JpaRepository

interface ProjectRepository : JpaRepository<Project, Long>
