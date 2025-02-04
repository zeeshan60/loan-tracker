package com.zeenom.loan_tracker.friends

import com.fasterxml.jackson.databind.ObjectMapper
import com.zeenom.loan_tracker.common.Command
import com.zeenom.loan_tracker.events.EventDto
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class AddFriendEventHandler(
    private val friendsDao: FriendsDao,
    private val objectMapper: ObjectMapper
) : Command {
    private val logger = LoggerFactory.getLogger(AddFriendEventHandler::class.java)
    override suspend fun execute(eventDto: EventDto) {
        if (eventDto.payload is CreateFriendDto) {
            friendsDao.saveFriend(eventDto.userId, eventDto.payload)
            logger.info("Friend added")
        } else {
            logger.error("Invalid payload: {}", objectMapper.writeValueAsString(eventDto.payload))
            throw IllegalArgumentException("Invalid add friend event payload")
        }
    }
}