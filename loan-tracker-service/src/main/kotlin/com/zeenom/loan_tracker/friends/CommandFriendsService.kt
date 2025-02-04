package com.zeenom.loan_tracker.friends

import com.zeenom.loan_tracker.common.Command
import com.zeenom.loan_tracker.events.EventDto
import org.springframework.stereotype.Service

@Service
class CommandFriendsService : Command {
    override suspend fun execute(eventDto: EventDto) {

    }
}