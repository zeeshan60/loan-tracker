package com.zeenom.loan_tracker.users

import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface UserEventRepository : CoroutineCrudRepository<UserEvent, UUID> {
    suspend fun findByUid(uid: String): UserEvent?
    suspend fun findByEmail(email: String): UserEvent?
    suspend fun findByPhoneNumber(phoneNumber: String): UserEvent?
}