package com.zeenom.loan_tracker.users

import kotlinx.coroutines.flow.Flow
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface UserEventRepository : CoroutineCrudRepository<UserEvent, UUID> {
    suspend fun findByUid(uid: String): UserEvent?
    suspend fun findAllByUidIn(uids: List<String>): Flow<UserEvent>
    suspend fun findByEmail(email: String): UserEvent?
    suspend fun findByPhoneNumber(phoneNumber: String): UserEvent?
    suspend fun findAllByEmailIn(emails: List<String>): Flow<UserEvent>
    suspend fun findAllByPhoneNumberIn(phoneNumbers: List<String>): Flow<UserEvent>
}