package com.zeenom.loan_tracker.users

import com.zeenom.loan_tracker.friends.UserUpdateDto
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.*

@Service
class UserService(
    private val userEventHandler: UserEventHandler,
    private val applicationEventPublisher: ApplicationEventPublisher,
) {
    val logger = LoggerFactory.getLogger(UserService::class.java)
    suspend fun createUser(userDto: UserDto) {
        val existing = userDto.email?.let { userEventHandler.findUsersByEmails(listOf(userDto.email)).toList() }
            ?: userDto.phoneNumber?.let {
                userEventHandler.findUsersByPhoneNumbers(listOf(userDto.phoneNumber)).toList()
            } ?: emptyList()
        if (existing.isNotEmpty()) {
            throw IllegalArgumentException("User with email ${userDto.email} or phone number ${userDto.phoneNumber} already exist")
        }

        userEventHandler.findUserById(userDto.userFBId)?.let {
            throw IllegalStateException("User with this unique identifier already exist")
        }

        val streamId = userDto.uid ?: UUID.randomUUID()
        userEventHandler.addEvent(
            UserCreated(
                displayName = userDto.displayName,
                phoneNumber = userDto.phoneNumber,
                email = userDto.email,
                photoUrl = userDto.photoUrl,
                emailVerified = userDto.emailVerified,
                userId = userDto.userFBId,
                createdAt = Instant.now(),
                streamId = streamId,
                version = 1,
                createdBy = streamId
            )
        )
        userEventHandler.synchronize()
    }

    suspend fun updateUser(userDto: UserUpdateDto) {

        var existing = userEventHandler.findModelByUserId(userDto.uid)
            ?: throw IllegalArgumentException("User with this unique identifier does not exist")


        if (userDto.currency != null && userDto.currency != existing.currency) {
            userEventHandler.addEvent(
                UserCurrencyChanged(
                    currency = userDto.currency,
                    createdAt = Instant.now(),
                    streamId = existing.streamId,
                    version = existing.version + 1,
                    createdBy = userDto.uid
                ).also {
                    existing = it.applyEvent(existing)
                })
        }

        if (userDto.displayName != null && userDto.displayName != existing.displayName) {
            userEventHandler.addEvent(
                UserDisplayNameChanged(
                    displayName = userDto.displayName,
                    createdAt = Instant.now(),
                    streamId = existing.streamId,
                    version = existing.version + 1,
                    createdBy = userDto.uid
                ).also {
                    existing = it.applyEvent(existing)
                })
        }

        if (userDto.phoneNumber != null && userDto.phoneNumber != existing.phoneNumber) {
            userEventHandler.addEvent(
                UserPhoneNumberChanged(
                    phoneNumber = userDto.phoneNumber,
                    createdAt = Instant.now(),
                    streamId = existing.streamId,
                    version = existing.version + 1,
                    createdBy = userDto.uid
                ).also {
                    existing = it.applyEvent(existing)
                })
        }
        userEventHandler.synchronize()
    }

    suspend fun findUserById(uid: UUID): UserDto? {
        return userEventHandler.findByUserId(uid)
    }

    suspend fun findUserByFBId(uid: String): UserDto? {
        return userEventHandler.findUserById(uid)
    }

    suspend fun findByUserEmailOrPhoneNumber(email: String?, phoneNumber: String?): UserDto? {
        return email?.let {
            userEventHandler.findUsersByEmails(listOf(it)).firstOrNull()
        } ?: phoneNumber?.let {
            userEventHandler.findUsersByPhoneNumbers(listOf(it)).firstOrNull()
        }
    }

    suspend fun deleteUser(userId: UUID): UserModel {
        logger.info("Deleting user with ID: $userId")
        val existing = userEventHandler.findModelByUserId(userId)
            ?: throw IllegalArgumentException("User with this unique identifier does not exist")

        val event = UserDeleted(
            createdAt = Instant.now(), streamId = existing.streamId, version = existing.version + 1, createdBy = userId
        )
        //trigger application wide userdeleted event
        applicationEventPublisher.publishEvent(event)

        userEventHandler.addEvent(
            event
        )
        userEventHandler.synchronize()
        return event.applyEvent(existing)
    }
}