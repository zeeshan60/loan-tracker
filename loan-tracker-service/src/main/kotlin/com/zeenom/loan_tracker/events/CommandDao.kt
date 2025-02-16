package com.zeenom.loan_tracker.events

import org.springframework.stereotype.Service

@Service
class CommandDao(
    private val eventRepository: EventRepository,
    private val eventEntityAdapter: EventEntityAdapter,
) {
    suspend fun <T : CommandPayloadDto?> saveEvent(commandDto: CommandDto<T>) {
        eventRepository.save(eventEntityAdapter.fromDto(commandDto))
    }
}


