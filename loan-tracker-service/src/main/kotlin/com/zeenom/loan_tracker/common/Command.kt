package com.zeenom.loan_tracker.common

import com.zeenom.loan_tracker.events.CommandDto
import com.zeenom.loan_tracker.events.CommandPayloadDto

interface Command<T : CommandPayloadDto?> {
    suspend fun execute(commandDto: CommandDto<T>)
}