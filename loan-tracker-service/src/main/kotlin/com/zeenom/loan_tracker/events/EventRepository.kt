package com.zeenom.loan_tracker.events

import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Repository
interface EventRepository : ReactiveCrudRepository<EventEntity, String> {
    fun findEventEntityByEventId(eventId: String): Mono<EventEntity>
    fun deleteAllByEventId(eventId: String): Mono<Void>
    fun deleteAllByEvent(event: EventType): Mono<Void>
    fun findAllByEvent(event: EventType): Flux<EventEntity>
}