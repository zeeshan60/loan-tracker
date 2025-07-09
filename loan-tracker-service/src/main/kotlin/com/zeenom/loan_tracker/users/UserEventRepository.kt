package com.zeenom.loan_tracker.users

import kotlinx.coroutines.flow.toList
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface UserEventRepository : CoroutineCrudRepository<UserEvent, UUID> {

    @Query("SELECT * FROM user_events WHERE uid = :userId")
    suspend fun findUserEventStream(userId: String): List<UserEvent>

    @Query("SELECT * FROM user_events WHERE uid IN (:uids)")
    suspend fun findUserEventStreamByUidIn(uids: List<String>): List<UserEvent>

    @Query("SELECT * FROM user_events WHERE email = :email")
    suspend fun findUserEventStreamByEmail(email: String): List<UserEvent>

    @Query("SELECT * FROM user_events WHERE phone_number = :phoneNumber")
    suspend fun findUserEventStreamByPhoneNumber(phoneNumber: String): List<UserEvent>

    @Query("SELECT * FROM user_events WHERE email IN (:emails)")
    suspend fun findUserEventStreamByEmailsIn(emails: List<String>): List<UserEvent>

    @Query("SELECT * FROM user_events WHERE phone_number IN (:phoneNumbers)")
    suspend fun findUserEventStreamByPhoneNumberIn(phoneNumbers: List<String>): List<UserEvent>
}

suspend fun UserEventRepository.findByUid(uid: String): UserModel? {
    return userModels(findAll().toList()).find { it.uid == uid }
}

suspend fun UserEventRepository.findAllByUidIn(uids: List<String>): List<UserModel> {
    val eventStreams = findAll().toList()
    return userModels(eventStreams).filter { it.uid in uids }
}

suspend fun UserEventRepository.findByEmail(email: String): UserModel? {
    return userModels(findAll().toList())
        .find { it.email == email }
}

suspend fun UserEventRepository.findByPhoneNumber(phoneNumber: String): UserModel? {
    val eventStream = findAll().toList()
    return userModels(eventStream).find { it.phoneNumber == phoneNumber }
}

suspend fun UserEventRepository.findAllByEmailIn(emails: List<String>): List<UserModel> {
    val eventStreams = findAll().toList()
    return userModels(eventStreams).filter { it.email in emails }
}

suspend fun UserEventRepository.findAllByPhoneNumberIn(phoneNumbers: List<String>): List<UserModel> {
    val eventStreams = findAll().toList()
    return userModels(eventStreams).filter { it.phoneNumber in phoneNumbers }
}

fun userModels(eventStreams: List<UserEvent>) =
    eventStreams.groupBy { it.streamId }.map { (_, events) ->
        userModel(events) ?: throw IllegalStateException("User model not found")
    }

fun userModel(eventStream: List<UserEvent>) =
    eventStream.map { it.toEvent() }.sortedBy { it.version }
        .fold(null as UserModel?) { model, userEvent ->
            userEvent.applyEvent(model)
        }
