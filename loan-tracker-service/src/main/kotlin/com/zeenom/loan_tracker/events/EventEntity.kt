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

    fun fromDto(eventDto: EventDto<*>, id: UUID? = null, createdAt: Instant? = null) =
        EventEntity(
            id = id,
            event = eventDto.event,
            userId = eventDto.userId,
            createdAt = createdAt ?: secondInstant.now(),
            payload = eventDto.payload?.toJson(objectMapper)
        )

    fun toDto(entity: EventEntity) = EventDto(
        event = entity.event,
        payload = entity.payload?.toClass(objectMapper, EventPayloadDto::class.java),
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
