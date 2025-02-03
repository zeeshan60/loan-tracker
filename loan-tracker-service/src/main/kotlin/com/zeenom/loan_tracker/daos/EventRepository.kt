package com.zeenom.loan_tracker.daos

import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Mono

@Repository
interface EventRepository : ReactiveCrudRepository<EventEntity, String> {
    fun findEventEntityByEventId(eventId: String): Mono<EventEntity>
    fun deleteAllByEventId(eventId: String): Mono<Void>
}