package com.zeenom.loan_tracker.events

import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.stereotype.Service

@Service
class EventDao(
    private val eventRepository: EventRepository,
    private val eventEntityAdapter: EventEntityAdapter
) {
    suspend fun saveEvent(eventDto: EventDto): String {
        eventRepository.save(eventEntityAdapter.fromDto(eventDto)).awaitSingle()
        return "Event saved"
    }

    suspend fun findEventByTransactionId(eventId: String): EventDto? {
        return eventRepository.findEventEntityByEventId(eventId).awaitSingleOrNull()
            ?.let { eventEntityAdapter.toDto(it) }
    }

    suspend fun deleteEventsByTransactionId(eventId: String): String {
        eventRepository.deleteAllByEventId(eventId).awaitSingleOrNull()
        return "Event $eventId deleted"
    }
}
