package com.zeenom.loan_tracker.common

import com.zeenom.loan_tracker.events.EventDto
import com.zeenom.loan_tracker.events.EventPayloadDto

interface Command<T: EventPayloadDto?> {
    suspend fun execute(eventDto: EventDto<T>)
}