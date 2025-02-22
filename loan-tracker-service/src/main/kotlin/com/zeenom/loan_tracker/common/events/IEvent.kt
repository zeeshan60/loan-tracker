package com.zeenom.loan_tracker.common.events

import java.time.Instant
import java.util.*

interface IEvent<MODEL> {
    val userId: String
    val streamId: UUID
    val createdAt: Instant
    val createdBy: String
    val version: Int

    fun toEntity(): Any
    fun applyEvent(existing: MODEL): MODEL
}