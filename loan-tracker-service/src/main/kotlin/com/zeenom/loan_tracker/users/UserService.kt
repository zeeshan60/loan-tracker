package com.zeenom.loan_tracker.users

import com.zeenom.loan_tracker.friends.UserUpdateDto
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.*

@Service
class UserService(
    private val userEventHandler: UserEventHandler
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

    suspend fun updateUser(userDto: UserUpdateDto) {

        var existing = userEventHandler.findUserModelByUid(userDto.uid)
            ?: throw IllegalArgumentException("User with this unique identifier does not exist")


        if (userDto.currency != null && userDto.currency != existing.currency) {
            userEventHandler.addEvent(
                UserCurrencyChanged(
                    userId = userDto.uid,
                    currency = userDto.currency,
                    createdAt = Instant.now(),
                    streamId = existing.streamId,
                    version = existing.version + 1,
                    createdBy = userDto.uid
                ).also {
                    existing = it.applyEvent(existing)
                }
            )
        }

        if (userDto.displayName != null && userDto.displayName != existing.displayName) {
            userEventHandler.addEvent(
                UserDisplayNameChanged(
                    userId = userDto.uid,
                    displayName = userDto.displayName,
                    createdAt = Instant.now(),
                    streamId = existing.streamId,
                    version = existing.version + 1,
                    createdBy = userDto.uid
                ).also {
                    existing = it.applyEvent(existing)
                }
            )
        }

        if (userDto.phoneNumber != null && userDto.phoneNumber != existing.phoneNumber) {
            userEventHandler.addEvent(
                UserPhoneNumberChanged(
                    userId = userDto.uid,
                    phoneNumber = userDto.phoneNumber,
                    createdAt = Instant.now(),
                    streamId = existing.streamId,
                    version = existing.version + 1,
                    createdBy = userDto.uid
                ).also {
                    existing = it.applyEvent(existing)
                }
            )
        }
    }

    suspend fun findUserById(uid: String): UserDto? {
        return userEventHandler.findUserById(uid)
    }

    suspend fun findByUserEmailOrPhoneNumber(email: String?, phoneNumber: String?): UserDto? {
        return email?.let {
            userEventHandler.findUsersByEmails(listOf(it)).firstOrNull()
        } ?: phoneNumber?.let {
            userEventHandler.findUsersByPhoneNumbers(listOf(it)).firstOrNull()
        }
    }

    suspend fun deleteUser(string: String) {
        val existing = userEventHandler.findUserModelByUid(string)
            ?: throw IllegalArgumentException("User with this unique identifier does not exist")

        userEventHandler.addEvent(
            UserDeleted(
                userId = string,
                createdAt = Instant.now(),
                streamId = existing.streamId,
                version = existing.version + 1,
                createdBy = string
            )
        )
    }
}