package com.zeenom.loan_tracker.events

import com.fasterxml.jackson.databind.ObjectMapper
import com.zeenom.loan_tracker.common.SecondInstant
import com.zeenom.loan_tracker.common.r2dbc.fromJson
import com.zeenom.loan_tracker.common.r2dbc.toJson
import io.r2dbc.postgresql.codec.Json
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class EventEntityAdapter(
    private val objectMapper: ObjectMapper,
    private val secondInstant: SecondInstant
) {

    fun fromDto(eventDto: EventDto, id: String? = null, createdAt: Instant? = null) =
        EventEntity(
            id = id,
            event = eventDto.event,
            eventId = eventDto.eventId,
            userId = eventDto.userId,
            createdAt = createdAt ?: secondInstant.now(),
            payload = eventDto.payload?.toJson(objectMapper),
            source = eventDto.source
        )

    fun toDto(entity: EventEntity) = EventDto(
        eventId = entity.eventId,
        event = entity.event,
        payload = entity.payload?.fromJson(objectMapper, EventPayloadDto::class.java),
        userId = entity.userId,
        source = entity.source,
    )
}

@Table("events")
data class EventEntity(
    @Id val id: String?,
    val event: EventType,
    val eventId: String,
    val userId: String,
    val createdAt: Instant,
    val payload: Json?,
    val source: EventSource
)

enum class EventSource {
    DIRECT, INDIRECT
}
