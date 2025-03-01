package com.zeenom.loan_tracker.users

import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.*

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

        userEventHandler.findUserById(userDto.uid)?.let {
            throw IllegalStateException("User with this unique identifier already exist")
        }

        userEventHandler.addEvent(
            UserCreated(
                id = null,
                displayName = userDto.displayName,
                phoneNumber = userDto.phoneNumber,
                email = userDto.email,
                photoUrl = userDto.photoUrl,
                emailVerified = userDto.emailVerified,
                userId = userDto.uid,
                createdAt = Instant.now(),
                streamId = UUID.randomUUID(),
                version = 1,
                createdBy = userDto.uid
            )
        )
    }

    suspend fun findUserById(uid: String): UserDto? {
        return userEventHandler.findUserById(uid)
    }
}