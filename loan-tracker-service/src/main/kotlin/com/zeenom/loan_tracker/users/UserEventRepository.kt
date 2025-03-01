package com.zeenom.loan_tracker.users

import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface UserEventRepository : CoroutineCrudRepository<UserEvent, UUID> {

    suspend fun findByUid(uid: String): UserModel? {
        val eventStream = findUserEventStream(uid)
        return userModel(eventStream)
    }

    @Query("SELECT * FROM user_events WHERE uid = :userId")
    suspend fun findUserEventStream(userId: String): List<UserEvent>

    suspend fun findAllByUidIn(uids: List<String>): List<UserModel> {
        val eventStreams = findUserEventStreamByUidIn(uids)
        return userModels(eventStreams)
    }

    @Query("SELECT * FROM user_events WHERE uid IN (:uids)")
    suspend fun findUserEventStreamByUidIn(uids: List<String>): List<UserEvent>

    suspend fun findByEmail(email: String): UserModel? {
        return userModel(findUserEventStreamByEmail(email))
    }

    @Query("SELECT * FROM user_events WHERE email = :email")
    suspend fun findUserEventStreamByEmail(email: String): List<UserEvent>

    suspend fun findByPhoneNumber(phoneNumber: String): UserModel? {
        val eventStream = findUserEventStreamByPhoneNumber(phoneNumber)
        return userModel(eventStream)
    }

    @Query("SELECT * FROM user_events WHERE phone_number = :phoneNumber")
    suspend fun findUserEventStreamByPhoneNumber(phoneNumber: String): List<UserEvent>

    suspend fun findAllByEmailIn(emails: List<String>): List<UserModel> {
        val eventStreams = findUserEventStreamByEmailsIn(emails)
        return userModels(eventStreams)
    }

    @Query("SELECT * FROM user_events WHERE email IN (:emails)")
    suspend fun findUserEventStreamByEmailsIn(emails: List<String>): List<UserEvent>

    suspend fun findAllByPhoneNumberIn(phoneNumbers: List<String>): List<UserModel> {
        val eventStreams = findUserEventStreamByPhoneNumberIn(phoneNumbers)
        return userModels(eventStreams)
    }

    @Query("SELECT * FROM user_events WHERE phone_number IN (:phoneNumbers)")
    suspend fun findUserEventStreamByPhoneNumberIn(phoneNumbers: List<String>): List<UserEvent>

}

fun userModels(eventStreams: List<UserEvent>) =
    eventStreams.groupBy { it.streamId }.map { (_, events) ->
        userModel(events) ?: throw IllegalStateException("User model not found")
    }

fun userModel(eventStream: List<UserEvent>) =
    eventStream.map { it.toEvent() }.sortedByDescending { it.version }
        .fold(null as UserModel?) { model, userEvent ->
            userEvent.applyEvent(model)
        }
