package com.zeenom.loan_tracker.events

import com.fasterxml.jackson.databind.ObjectMapper
import com.zeenom.loan_tracker.common.SecondInstant
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

    private fun toJsonB(payload: EventPayloadDto): Json {
        return Json.of(objectMapper.writeValueAsString(payload))
    }

    private fun fromJsonB(json: Json?): EventPayloadDto? {
        return json?.let { objectMapper.readValue(it.asString(), EventPayloadDto::class.java) }
    }

    fun fromDto(eventDto: EventDto, id: String? = null) =
        EventEntity(
            id = id,
            event = eventDto.event,
            eventId = eventDto.eventId,
            userId = eventDto.userId,
            createdAt = secondInstant.now(),
            payload = eventDto.payload?.let { toJsonB(it) }
        )

    fun toDto(entity: EventEntity) = EventDto(
        eventId = entity.eventId,
        event = entity.event,
        payload = entity.payload?.let { fromJsonB(it) },
        userId = entity.userId
    )
}

@Table("events")
data class EventEntity(
    @Id val id: String?,
    val event: EventType,
    val eventId: String,
    val userId: String,
    val createdAt: Instant,
    val payload: Json?
)
