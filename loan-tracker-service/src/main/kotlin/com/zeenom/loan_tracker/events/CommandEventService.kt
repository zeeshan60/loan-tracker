package com.zeenom.loan_tracker.events

import com.zeenom.loan_tracker.common.Command
import com.zeenom.loan_tracker.friends.AddFriendEventHandler
import com.zeenom.loan_tracker.security.LoginEventHandler
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class CommandEventService(
    private val eventHandlers: EventHandlers,
    private val eventDao: EventDao
) : Command {
    override suspend fun execute(eventDto: EventDto): Unit = coroutineScope {
        val saveEventJob = async { eventDao.saveEvent(eventDto) }
        val executeJob = async { eventHandlers.getHandler(eventDto.event).execute(eventDto) }
        awaitAll(saveEventJob, executeJob)
    }
}

@Service
class EventHandlers(val loginEventHandler: LoginEventHandler, val addFriendEventHandler: AddFriendEventHandler) {
    private val logger = LoggerFactory.getLogger(EventHandlers::class.java)
    fun getHandler(eventType: EventType): Command {
        return when (eventType) {
            EventType.LOGIN -> loginEventHandler
            EventType.ADD_FRIEND -> addFriendEventHandler
            else -> let {
                logger.error("Invalid event type: $eventType")
                throw IllegalArgumentException("Invalid event type: $eventType")
            }
        }
    }
}
