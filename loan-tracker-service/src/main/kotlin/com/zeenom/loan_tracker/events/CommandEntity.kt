package com.zeenom.loan_tracker.events

import io.r2dbc.postgresql.codec.Json
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant
import java.util.*

@Table("commands")
data class CommandEntity(
    @Id val id: UUID? = null,
    val commandType: CommandType,
    val userId: UUID?, //TODO make it uuid and nullable
    val createdAt: Instant,
    val payload: Json?
)
