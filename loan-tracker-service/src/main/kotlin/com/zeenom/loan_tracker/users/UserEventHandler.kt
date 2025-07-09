package com.zeenom.loan_tracker.users

import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Service
import java.util.*

@Service
class UserEventHandler(
    private val userRepository: UserEventRepository,
    private val userModelRepository: UserModelRepository
) : SyncableEventHandler<UserModel, UserEvent> {
    override fun modelRepository(): SyncableModelRepository<UserModel> {
        return userModelRepository
    }

    override fun eventRepository(): SyncableEventRepository<UserEvent> {
        return userRepository
    }

    suspend fun addEvent(event: IUserEvent) {
        userRepository.save(event.toEntity())
    }

    suspend fun findUserById(uid: String): UserDto? {
        synchronize()
        return userModelRepository.findByUidAndDeletedIsFalse(uid)?.let {
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

    suspend fun findByUserId(userId: UUID, includeDeleted: Boolean = false): UserDto? {
        synchronize()
        return (if (includeDeleted) userModelRepository.findByStreamId(userId) else userModelRepository.findByStreamIdAndDeletedIsFalse(
            userId
        ))?.let {
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
        synchronize()
        return userModelRepository.findByStreamIdAndDeletedIsFalse(userId)
    }

    suspend fun findUsersByUids(uids: List<UUID>): List<UserDto> {
        if (uids.isEmpty()) return emptyList()
        synchronize()
        return userModelRepository.findAllByStreamIdInAndDeletedIsFalse(uids).toList().map {
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
        synchronize()
        return userModelRepository.findAllByEmailInAndDeletedIsFalse(emails).toList().map {
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

    suspend fun findUsersByPhoneNumbers(phoneNumbers: List<String>, includeDeleted: Boolean = false): List<UserDto> {
        if (phoneNumbers.isEmpty()) return emptyList()
        synchronize()
        return userModelRepository.findAllByPhoneNumberInAndDeletedIsFalse(phoneNumbers).toList().map {
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
        synchronize()
        return userModelRepository.findByEmailAndDeletedIsFalse(email)?.let {
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
        synchronize()
        return userModelRepository.findByPhoneNumberAndDeletedIsFalse(phoneNumber)?.let {
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

