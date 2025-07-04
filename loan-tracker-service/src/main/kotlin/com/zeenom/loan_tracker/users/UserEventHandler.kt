package com.zeenom.loan_tracker.users

import io.swagger.v3.core.util.Json
import kotlinx.coroutines.flow.toList
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.*

@Service
class UserEventHandler(
    private val userRepository: UserEventRepository,
    private val userModelRepository: UserModelRepository
) {

    val logger = LoggerFactory.getLogger(UserEventHandler::class.java)
    suspend fun addEvent(event: IUserEvent) {
        userRepository.save(event.toEntity())
        val existing = userModelRepository.findByStreamId(event.streamId)
        val updated = event.applyEvent(existing)
        //If deleted event is received, delete the user model
        if (updated.deleted) {
            userModelRepository.deleteById(updated.id!!)
            logger.warn("User with stream id ${updated.streamId} deleted")
        } else {
            userModelRepository.save(updated)
        }
    }

    suspend fun findUserById(uid: String): UserDto? {
        return userModelRepository.findByUid(uid)?.let {
            UserDto(
                uid = it.streamId,
                displayName = it.displayName,
                phoneNumber = it.phoneNumber,
                currency = it.currency,
                email = it.email,
                emailVerified = it.emailVerified,
                photoUrl = it.photoUrl,
                userFBId = it.uid
            )
        }
    }

    suspend fun findByUserId(userId: UUID): UserDto? {
        return userModelRepository.findByStreamId(userId)?.let {
            UserDto(
                uid = it.streamId,
                displayName = it.displayName,
                phoneNumber = it.phoneNumber,
                currency = it.currency,
                email = it.email,
                emailVerified = it.emailVerified,
                photoUrl = it.photoUrl,
                userFBId = it.uid
            )
        }
    }

    suspend fun findModelByUserId(userId: UUID): UserModel? {
        return userModelRepository.findByStreamId(userId)
    }

    suspend fun findUsersByUids(uids: List<UUID>): List<UserDto> {
        if (uids.isEmpty()) return emptyList()
        Json.prettyPrint(uids)
        return userModelRepository.findAllByStreamIdIn(uids).toList().map {
            UserDto(
                uid = it.streamId,
                displayName = it.displayName,
                phoneNumber = it.phoneNumber,
                currency = it.currency,
                email = it.email,
                emailVerified = it.emailVerified == true,
                photoUrl = it.photoUrl,
                userFBId = it.uid
            )
        }
    }

    suspend fun findUsersByEmails(emails: List<String>): List<UserDto> {
        if (emails.isEmpty()) return emptyList()
        return userModelRepository.findAllByEmailIn(emails).toList().map {
            UserDto(
                uid = it.streamId,
                displayName = it.displayName,
                phoneNumber = it.phoneNumber,
                currency = it.currency,
                email = it.email,
                emailVerified = it.emailVerified,
                photoUrl = it.photoUrl,
                userFBId = it.uid
            )
        }
    }

    suspend fun findUsersByPhoneNumbers(phoneNumbers: List<String>): List<UserDto> {
        if (phoneNumbers.isEmpty()) return emptyList()
        return userModelRepository.findAllByPhoneNumberIn(phoneNumbers).toList().map {
            UserDto(
                uid = it.streamId,
                displayName = it.displayName,
                phoneNumber = it.phoneNumber,
                currency = it.currency,
                email = it.email,
                emailVerified = it.emailVerified,
                photoUrl = it.photoUrl,
                userFBId = it.uid
            )
        }
    }

    suspend fun findUserByEmail(email: String): UserDto? {
        return userModelRepository.findByEmail(email)?.let {
            UserDto(
                uid = it.streamId,
                displayName = it.displayName,
                phoneNumber = it.phoneNumber,
                currency = it.currency,
                email = it.email,
                emailVerified = it.emailVerified,
                photoUrl = it.photoUrl,
                userFBId = it.uid
            )
        }
    }

    suspend fun findUserByPhoneNumber(phoneNumber: String): UserDto? {
        return userModelRepository.findByPhoneNumber(phoneNumber)?.let {
            UserDto(
                uid = it.streamId,
                displayName = it.displayName,
                phoneNumber = it.phoneNumber,
                currency = it.currency,
                email = it.email,
                emailVerified = it.emailVerified,
                photoUrl = it.photoUrl,
                userFBId = it.uid
            )
        }
    }

    suspend fun findUserByEmailOrPhoneNumber(email: String?, phoneNumber: String?): UserDto? {
        return email?.let { findUserByEmail(email) } ?: phoneNumber?.let { findUserByPhoneNumber(phoneNumber) }
    }
}

