package com.zeenom.loan_tracker.users

import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Service

@Service
class UserEventHandler(private val userRepository: UserEventRepository, private val userModelRepository: UserModelRepository) {

    suspend fun addEvent(event: IUserEvent) {
        userRepository.save(event.toEntity())
        val existing = userModelRepository.findByStreamId(event.streamId)
        userModelRepository.save(event.applyEvent(existing))
    }

    suspend fun findUserModelByUid(uid: String): UserModel? {
        return userModelRepository.findByUid(uid)
    }

    suspend fun findUserById(uid: String): UserDto? {
        return userModelRepository.findByUid(uid)?.let {
            UserDto(
                uid = it.uid,
                displayName = it.displayName,
                phoneNumber = it.phoneNumber,
                currency = it.currency,
                email = it.email,
                emailVerified = it.emailVerified ?: false,
                photoUrl = it.photoUrl
            )
        }
    }

    suspend fun findUsersByUids(uids: List<String>): List<UserDto> {
        if (uids.isEmpty()) return emptyList()
        return userModelRepository.findAllByUidIn(uids).toList().map {
            UserDto(
                uid = it.uid,
                displayName = it.displayName,
                phoneNumber = it.phoneNumber,
                currency = it.currency,
                email = it.email,
                emailVerified = it.emailVerified == true,
                photoUrl = it.photoUrl
            )
        }
    }

    suspend fun findUsersByEmails(emails: List<String>): List<UserDto> {
        if (emails.isEmpty()) return emptyList()
        return userModelRepository.findAllByEmailIn(emails).toList().map {
            UserDto(
                uid = it.uid,
                displayName = it.displayName,
                phoneNumber = it.phoneNumber,
                currency = it.currency,
                email = it.email,
                emailVerified = it.emailVerified ?: false,
                photoUrl = it.photoUrl
            )
        }
    }

    suspend fun findUsersByPhoneNumbers(phoneNumbers: List<String>): List<UserDto> {
        if (phoneNumbers.isEmpty()) return emptyList()
        return userModelRepository.findAllByPhoneNumberIn(phoneNumbers).toList().map {
            UserDto(
                uid = it.uid,
                displayName = it.displayName,
                phoneNumber = it.phoneNumber,
                currency = it.currency,
                email = it.email,
                emailVerified = it.emailVerified ?: false,
                photoUrl = it.photoUrl
            )
        }
    }

    suspend fun findUserByEmail(email: String): UserDto? {
        return userModelRepository.findByEmail(email)?.let {
            UserDto(
                uid = it.uid,
                displayName = it.displayName,
                phoneNumber = it.phoneNumber,
                currency = it.currency,
                email = it.email,
                emailVerified = it.emailVerified ?: false,
                photoUrl = it.photoUrl
            )
        }
    }

    suspend fun findUserByPhoneNumber(phoneNumber: String): UserDto? {
        return userModelRepository.findByPhoneNumber(phoneNumber)?.let {
            UserDto(
                uid = it.uid,
                displayName = it.displayName,
                phoneNumber = it.phoneNumber,
                currency = it.currency,
                email = it.email,
                emailVerified = it.emailVerified ?: false,
                photoUrl = it.photoUrl
            )
        }
    }

    suspend fun findUserByEmailOrPhoneNumber(email: String?, phoneNumber: String?): UserDto? {
        return email?.let { findUserByEmail(email) } ?: phoneNumber?.let { findUserByPhoneNumber(phoneNumber) }
    }
}

