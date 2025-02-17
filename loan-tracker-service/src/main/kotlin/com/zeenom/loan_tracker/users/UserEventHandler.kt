package com.zeenom.loan_tracker.users

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class UserEventHandler(private val userRepository: UserEventRepository) {
    suspend fun createIfNotExist(userDto: UserDto) {
        findUserById(userDto.uid) ?: createUser(userDto)
    }

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
                emailVerified = it.emailVerified ?: false, //todo make the dto optional
                photoUrl = it.photoUrl
            )
        }
    }

    suspend fun findUsersByUids(uids: List<String>): Flow<UserDto> {
        return userRepository.findAllByUidIn(uids).map {
            UserDto(
                uid = it.uid,
                displayName = it.displayName,
                phoneNumber = it.phoneNumber,
                email = it.email,
                emailVerified = it.emailVerified ?: false, //todo make the dto optional
                photoUrl = it.photoUrl
            )
        }
    }

    suspend fun findUsersByEmails(emails: List<String>): Flow<UserDto> {
        return userRepository.findAllByEmailIn(emails).map {
            UserDto(
                uid = it.uid,
                displayName = it.displayName,
                phoneNumber = it.phoneNumber,
                email = it.email,
                emailVerified = it.emailVerified ?: false, //todo make the dto optional
                photoUrl = it.photoUrl
            )
        }
    }

    suspend fun findUsersByPhoneNumbers(phoneNumbers: List<String>): Flow<UserDto> {
        return userRepository.findAllByPhoneNumberIn(phoneNumbers).map {
            UserDto(
                uid = it.uid,
                displayName = it.displayName,
                phoneNumber = it.phoneNumber,
                email = it.email,
                emailVerified = it.emailVerified ?: false, //todo make the dto optional
                photoUrl = it.photoUrl
            )
        }
    }
}

interface NewEvent {
    val type: NewEventType
}

enum class NewEventType {
    CREATE_USER
}