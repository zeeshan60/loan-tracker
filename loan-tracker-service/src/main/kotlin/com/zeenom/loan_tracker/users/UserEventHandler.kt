package com.zeenom.loan_tracker.users

import com.zeenom.loan_tracker.common.events.IEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import org.springframework.stereotype.Service

@Service
class UserEventHandler(private val userRepository: UserEventRepository) {

    suspend fun addEvent(event: IEvent<UserModel>) {
        val entity = event.toEntity()
        if (entity is UserEvent) {
            userRepository.save(entity)
        } else
            throw IllegalStateException("Invalid event type ${entity.javaClass}")
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

    suspend fun findUsersByUids(uids: List<String>): List<UserDto> {
        if (uids.isEmpty()) return emptyList()
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

    suspend fun findUsersByEmails(emails: List<String>): List<UserDto> {
        if (emails.isEmpty()) return emptyList()
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

    suspend fun findUsersByPhoneNumbers(phoneNumbers: List<String>): List<UserDto> {
        if (phoneNumbers.isEmpty()) return emptyList()
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

