package com.zeenom.loan_tracker.events

import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.stereotype.Service

@Service
class EventDao(
    private val eventRepository: EventRepository,
    private val eventEntityAdapter: EventEntityAdapter
) {
    suspend fun saveEvent(eventDto: EventDto) {
        eventRepository.save(eventEntityAdapter.fromDto(eventDto)).awaitSingle()
    }
}


