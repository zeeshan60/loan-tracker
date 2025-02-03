package com.zeenom.loan_tracker.events

import com.zeenom.loan_tracker.common.Command
import com.zeenom.loan_tracker.security.LoginEventHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class CommandEventService(
    private val eventHandlers: EventHandlers,
    private val eventDao: EventDao
) : Command {
    override suspend fun execute(eventDto: EventDto): Unit = coroutineScope {
        val saveEventJob = async(Dispatchers.IO) { eventDao.saveEvent(eventDto) }
        val executeJob = async(Dispatchers.IO) { eventHandlers.getHandler(eventDto.event).execute(eventDto) }
        saveEventJob.await()
        executeJob.await()
    }
}

@Service
class EventHandlers(val loginEventHandler: LoginEventHandler) {
    private val logger = LoggerFactory.getLogger(EventHandlers::class.java)
    fun getHandler(eventType: EventType): Command {
        when (eventType) {
            EventType.LOGIN -> return loginEventHandler
            else -> let {
                logger.error("Invalid event type: $eventType")
                throw IllegalArgumentException("Invalid event type: $eventType")
            }
        }
    }
}
