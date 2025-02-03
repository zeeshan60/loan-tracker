package com.zeenom.loan_tracker.common

import com.zeenom.loan_tracker.events.EventDto

interface Command {
    suspend fun execute(eventDto: EventDto)
}