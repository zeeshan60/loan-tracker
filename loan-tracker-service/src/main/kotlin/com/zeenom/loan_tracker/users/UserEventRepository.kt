package com.zeenom.loan_tracker.users

import kotlinx.coroutines.flow.Flow
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface UserEventRepository : CoroutineCrudRepository<UserEvent, UUID> {
    suspend fun findByUid(uid: String): UserModel? {
        val eventStream = findUserEventStream(uid)
        return eventStream.map { it.toEvent() }.sortedByDescending { it.version }
            .fold(null as UserModel?) { model, userEvent ->
                userEvent.applyEvent(model)
            }
    }
    @Query("SELECT * FROM user_events WHERE uid = :userId")
    suspend fun findUserEventStream(userId: String): List<UserEvent>

    suspend fun findAllByUidIn(uids: List<String>): Flow<UserEvent>
    suspend fun findByEmail(email: String): UserEvent?
    suspend fun findByPhoneNumber(phoneNumber: String): UserEvent?
    suspend fun findAllByEmailIn(emails: List<String>): Flow<UserEvent>
    suspend fun findAllByPhoneNumberIn(phoneNumbers: List<String>): Flow<UserEvent>
}