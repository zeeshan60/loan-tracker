package com.zeenom.loan_tracker.common.events

import java.util.*

interface ReadModel<T : IEvent> {
    suspend fun read(userId: String, streamId: UUID): T?
    suspend fun apply(existing: T, next: T): T
    suspend fun resolveAll(events: List<T>): List<T> {
        return events.groupBy { it.streamId }
            .map { (_, events) -> events.reduce { current, next -> apply(current, next) } }
    }
}