package com.zeenom.loan_tracker.events

import com.fasterxml.jackson.databind.ObjectMapper
import com.zeenom.loan_tracker.common.SecondInstant
import com.zeenom.loan_tracker.common.r2dbc.toClass
import com.zeenom.loan_tracker.common.r2dbc.toJson
import io.r2dbc.postgresql.codec.Json
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.*

@Component
class EventEntityAdapter(
    private val objectMapper: ObjectMapper,
    private val secondInstant: SecondInstant
) {

    fun fromDto(commandDto: CommandDto<*>, id: UUID? = null, createdAt: Instant? = null) =
        EventEntity(
            id = id,
            event = commandDto.event,
            userId = commandDto.userId,
            createdAt = createdAt ?: secondInstant.now(),
            payload = commandDto.payload?.toJson(objectMapper)
        )

    fun toDto(entity: EventEntity) = CommandDto(
        event = entity.event,
        payload = entity.payload?.toClass(objectMapper, CommandPayloadDto::class.java),
        userId = entity.userId,
    )
}

@Table("events")
data class EventEntity(
    @Id val id: UUID?,
    val event: EventType,
    val userId: String,
    val createdAt: Instant,
    val payload: Json?
)
