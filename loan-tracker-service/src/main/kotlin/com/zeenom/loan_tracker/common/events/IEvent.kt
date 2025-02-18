package com.zeenom.loan_tracker.common.events

import java.util.*

interface IEvent {
    val streamId: UUID
    val version: Int
}