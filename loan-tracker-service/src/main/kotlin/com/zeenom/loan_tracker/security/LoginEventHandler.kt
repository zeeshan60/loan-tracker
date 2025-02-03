package com.zeenom.loan_tracker.security

import com.fasterxml.jackson.databind.ObjectMapper
import com.zeenom.loan_tracker.common.Command
import com.zeenom.loan_tracker.events.EventDto
import com.zeenom.loan_tracker.users.UserDao
import com.zeenom.loan_tracker.users.UserDto
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class LoginEventHandler(
    private val userDao: UserDao,
    private val objectMapper: ObjectMapper
) : Command {
    private val logger = LoggerFactory.getLogger(LoginEventHandler::class.java)
    override suspend fun execute(eventDto: EventDto) {
        if (eventDto.payload is UserDto) {
            userDao.loginUser(eventDto.payload)
            logger.info("User saved")
        } else {
            logger.error("Invalid payload: {}", objectMapper.writeValueAsString(eventDto.payload))
            throw IllegalArgumentException("Invalid login user event payload")
        }
    }

}