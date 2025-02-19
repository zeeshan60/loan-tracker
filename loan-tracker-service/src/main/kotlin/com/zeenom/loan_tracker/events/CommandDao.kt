package com.zeenom.loan_tracker.events

import com.fasterxml.jackson.databind.ObjectMapper
import com.zeenom.loan_tracker.common.r2dbc.toJson
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class CommandDao(
    private val eventRepository: EventRepository,
    private val objectMapper: ObjectMapper,
) {
    suspend fun <T : CommandPayloadDto?> addCommand(commandDto: CommandDto<T>) {
        eventRepository.save(
            CommandEntity(
                userId = commandDto.userId,
                commandType = commandDto.commandType,
                createdAt = Instant.now(),
                payload = commandDto.payload?.toJson(objectMapper = objectMapper)
            )
        )
    }
}


