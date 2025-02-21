package com.zeenom.loan_tracker.users

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Service

@Service
class UserEventHandler(private val userRepository: UserEventRepository) {

    suspend fun saveEvent(userEvent: UserEvent) {
        try {
            userRepository.save(userEvent)
        } catch (e: DuplicateKeyException) {
            throw IllegalStateException("User with this unique identifier already exist")
        }
    }

    suspend fun findUserById(uid: String): UserDto? {
        return userRepository.findByUid(uid)?.let {
            UserDto(
                uid = it.uid,
                displayName = it.displayName,
                phoneNumber = it.phoneNumber,
                email = it.email,
                emailVerified = it.emailVerified ?: false,
                photoUrl = it.photoUrl
            )
        }
    }

    suspend fun findUsersByUids(uids: List<String>): Flow<UserDto> {
        if (uids.isEmpty()) return emptyFlow()
        return userRepository.findAllByUidIn(uids).map {
            UserDto(
                uid = it.uid,
                displayName = it.displayName,
                phoneNumber = it.phoneNumber,
                email = it.email,
                emailVerified = it.emailVerified ?: false,
                photoUrl = it.photoUrl
            )
        }
    }

    suspend fun findUsersByEmails(emails: List<String>): Flow<UserDto> {
        if (emails.isEmpty()) return emptyFlow()
        return userRepository.findAllByEmailIn(emails).map {
            UserDto(
                uid = it.uid,
                displayName = it.displayName,
                phoneNumber = it.phoneNumber,
                email = it.email,
                emailVerified = it.emailVerified ?: false,
                photoUrl = it.photoUrl
            )
        }
    }

    suspend fun findUsersByPhoneNumbers(phoneNumbers: List<String>): Flow<UserDto> {
        if (phoneNumbers.isEmpty()) return emptyFlow()
        return userRepository.findAllByPhoneNumberIn(phoneNumbers).map {
            UserDto(
                uid = it.uid,
                displayName = it.displayName,
                phoneNumber = it.phoneNumber,
                email = it.email,
                emailVerified = it.emailVerified ?: false,
                photoUrl = it.photoUrl
            )
        }
    }

    suspend fun findUserByEmail(email: String): UserDto? {
        return userRepository.findByEmail(email)?.let {
            UserDto(
                uid = it.uid,
                displayName = it.displayName,
                phoneNumber = it.phoneNumber,
                email = it.email,
                emailVerified = it.emailVerified ?: false,
                photoUrl = it.photoUrl
            )
        }
    }

    suspend fun findUserByPhoneNumber(phoneNumber: String): UserDto? {
        return userRepository.findByPhoneNumber(phoneNumber)?.let {
            UserDto(
                uid = it.uid,
                displayName = it.displayName,
                phoneNumber = it.phoneNumber,
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

