package com.zeenom.loan_tracker.users

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

    suspend fun updateUser(userDto: UserDto) {

        val existing = userEventHandler.findUserModelByUid(userDto.uid)
            ?: throw IllegalArgumentException("User with this unique identifier does not exist")

        if (existing.email != userDto.email) {
            throw IllegalArgumentException("User email cannot be changed")
        }

        if (existing.currency == userDto.currency) {
            return
        }
        userEventHandler.addEvent(
            UserCurrencyChanged(
                userId = userDto.uid,
                currency = userDto.currency,
                createdAt = Instant.now(),
                streamId = UUID.randomUUID(),
                version = existing.version + 1,
                createdBy = userDto.uid
            )
        )
    }

    suspend fun findUserById(uid: String): UserDto? {
        return userEventHandler.findUserById(uid)
    }
}