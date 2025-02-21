package com.zeenom.loan_tracker.users

import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class UserService(
    private val userEventHandler: UserEventHandler,
) {

    suspend fun createUser(userDto: UserDto) {
        val existing =
            userDto.email?.let { userEventHandler.findUsersByEmails(listOf(userDto.email)).toList() }
                ?: userDto.phoneNumber?.let {
                    userEventHandler.findUsersByPhoneNumbers(listOf(userDto.phoneNumber)).toList()
                } ?: emptyList()
        if (existing.isNotEmpty()) {
            throw IllegalArgumentException("User with email ${userDto.email} or phone number ${userDto.phoneNumber} already exist")
        }

        userEventHandler.saveEvent(
            UserEvent(
                uid = userDto.uid,
                displayName = userDto.displayName,
                phoneNumber = userDto.phoneNumber,
                email = userDto.email,
                emailVerified = userDto.emailVerified,
                photoUrl = userDto.photoUrl,
                createdAt = Instant.now(),
                version = 1,
                eventType = UserEventType.CREATE_USER
            )
        )
    }

    suspend fun findUserById(uid: String): UserDto? {
        return userEventHandler.findUserById(uid)
    }
}