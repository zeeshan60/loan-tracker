package com.zeenom.loan_tracker.common.migration

import org.springframework.data.annotation.Id
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.relational.core.mapping.Table
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.UUID

@Table(name = "migration_status")
data class MigrationStatus(
    @Id
    val id: UUID? = null,
    val version: Int,
    val createdAt: Instant,
    val finishedAt: Instant? = null,
    val entityVersion: Long = 0
)

@Repository
interface MigrationStatusRepository : CoroutineCrudRepository<MigrationStatus, UUID> {
    suspend fun findByVersion(version: Int): MigrationStatus?

    @Query(
        """
        SELECT * FROM migration_status WHERE finished_at IS NOT NULL ORDER BY version DESC LIMIT 1
        """
    )
    suspend fun findLastFinishedByOrderByVersionDesc(): MigrationStatus?
}