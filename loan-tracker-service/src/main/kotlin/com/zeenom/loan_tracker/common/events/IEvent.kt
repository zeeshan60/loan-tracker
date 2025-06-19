package com.zeenom.loan_tracker.common.events

import com.zeenom.loan_tracker.transactions.IEventAble
import java.time.Instant
import java.util.*

interface IEvent<MODEL> {
    val streamId: UUID
    val createdAt: Instant
    val createdBy: UUID
    val version: Int

    fun toEntity(): IEventAble<MODEL>
    fun applyEvent(existing: MODEL?): MODEL
}