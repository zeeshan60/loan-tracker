package com.zeenom.loan_tracker.users

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class UserEventHandler(private val userRepository: UserEventRepository) {

    suspend fun createUser(userDto: UserDto) {
        try {

            val existing =
                userDto.email?.let { findUsersByEmails(listOf(userDto.email)).toList() } ?: userDto.phoneNumber?.let {
                    findUsersByPhoneNumbers(listOf(userDto.phoneNumber)).toList()
                } ?: emptyList()
            if (existing.isNotEmpty()) {
                throw IllegalArgumentException("User with email ${userDto.email} or phone number ${userDto.phoneNumber} already exist")
            }

            userRepository.save(
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

