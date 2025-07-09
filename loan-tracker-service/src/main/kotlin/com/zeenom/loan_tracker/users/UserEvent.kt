package com.zeenom.loan_tracker.users

import com.zeenom.loan_tracker.common.events.IEvent
import com.zeenom.loan_tracker.transactions.IEventAble
import kotlinx.coroutines.flow.Flow
import org.springframework.data.annotation.Id
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.relational.core.mapping.Table
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.*

interface IUserEvent : IEvent<UserModel> {
    override fun toEntity(): UserEvent
}

@Table("user_events")
data class UserEvent(
    @Id val id: UUID? = null,
    val uid: String?,
    val streamId: UUID,
    val displayName: String?,
    val phoneNumber: String?,
    val email: String?,
    val photoUrl: String?,
    val emailVerified: Boolean?,
    val currency: String?,
    val createdAt: Instant,
    val createdBy: UUID,
    val version: Int,
    val eventType: UserEventType,
) : IEventAble<UserModel> {
    override fun toEvent(): IEvent<UserModel> {
        return when (eventType) {
            UserEventType.USER_CREATED -> UserCreated(
                userId = uid ?: throw IllegalStateException("User ID is required"),
                displayName = displayName ?: throw IllegalStateException("Display name is required"),
                phoneNumber = phoneNumber,
                email = email,
                photoUrl = photoUrl,
                emailVerified = emailVerified ?: throw IllegalStateException("Email verified is required"),
                createdAt = createdAt,
                version = version,
                streamId = streamId,
                createdBy = createdBy
            )

            UserEventType.CURRENCY_CHANGED -> UserCurrencyChanged(
                currency = currency ?: throw IllegalStateException("Currency is required"),
                createdAt = createdAt,
                version = version,
                streamId = streamId,
                createdBy = createdBy
            )

            UserEventType.PHONE_NUMBER_CHANGED -> UserPhoneNumberChanged(
                phoneNumber = phoneNumber,
                createdAt = createdAt,
                version = version,
                streamId = streamId,
                createdBy = createdBy
            )

            UserEventType.DISPLAY_NAME_CHANGED -> UserDisplayNameChanged(
                displayName = displayName ?: throw IllegalStateException("Display name is required"),
                createdAt = createdAt,
                version = version,
                streamId = streamId,
                createdBy = createdBy
            )

            UserEventType.USER_DELETED -> UserDeleted(
                createdAt = createdAt,
                version = version,
                streamId = streamId,
                createdBy = createdBy
            )
        }
    }
}

@Repository
interface UserModelRepository : CoroutineCrudRepository<UserModel, UUID>, SyncableModelRepository<UserModel> {
    suspend fun findByUidAndDeletedIsFalse(uid: String): UserModel?
    suspend fun findAllByStreamIdInAndDeletedIsFalse(streamIds: List<UUID>): Flow<UserModel>
    suspend fun findAllByEmailInAndDeletedIsFalse(emails: List<String>): Flow<UserModel>
    suspend fun findAllByPhoneNumberInAndDeletedIsFalse(phones: List<String>): Flow<UserModel>
    suspend fun findByEmailAndDeletedIsFalse(email: String): UserModel?
    suspend fun findByPhoneNumberAndDeletedIsFalse(phone: String): UserModel?
    suspend fun findByStreamIdAndDeletedIsFalse(streamId: UUID): UserModel?
    override suspend fun findByStreamId(streamId: UUID): UserModel?

    @Query("select * from user_model order by insert_order desc limit 1")
    override suspend fun findFirstSortByIdDescending(): UserModel?
}

@Table("user_model")
data class UserModel(
    @Id()
    val id: UUID?,
    override val streamId: UUID,
    val uid: String,
    val displayName: String,
    val phoneNumber: String?,
    val currency: String?,
    val email: String?,
    val photoUrl: String?,
    val emailVerified: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
    override val version: Int,
    override val deleted: Boolean,
    val insertOrder: Long? = null,
) : SyncableModel

data class UserCreated(
    val displayName: String,
    val phoneNumber: String?,
    val email: String?,
    val photoUrl: String?,
    val emailVerified: Boolean,
    val userId: String,
    override val createdAt: Instant,
    override val version: Int,
    override val streamId: UUID,
    override val createdBy: UUID,
) : IUserEvent {
    override fun toEntity(): UserEvent {
        return UserEvent(
            uid = userId,
            streamId = streamId,
            displayName = displayName,
            phoneNumber = phoneNumber,
            email = email,
            photoUrl = photoUrl,
            emailVerified = emailVerified,
            currency = null,
            createdAt = createdAt,
            createdBy = createdBy,
            version = version,
            eventType = UserEventType.USER_CREATED
        )
    }

    override fun applyEvent(existing: UserModel?): UserModel {
        return UserModel(
            id = null,
            uid = userId,
            streamId = streamId,
            displayName = displayName,
            phoneNumber = phoneNumber,
            email = email,
            photoUrl = photoUrl,
            currency = null,
            emailVerified = emailVerified,
            createdAt = createdAt,
            updatedAt = createdAt,
            version = version,
            deleted = false,
        )
    }
}

data class UserCurrencyChanged(
    val currency: String?,
    override val createdAt: Instant,
    override val version: Int,
    override val streamId: UUID,
    override val createdBy: UUID,
) : IUserEvent {
    override fun toEntity(): UserEvent {
        return UserEvent(
            uid = null,
            streamId = streamId,
            displayName = null,
            phoneNumber = null,
            email = null,
            photoUrl = null,
            emailVerified = null,
            currency = currency,
            createdAt = createdAt,
            createdBy = createdBy,
            version = version,
            eventType = UserEventType.CURRENCY_CHANGED
        )
    }

    override fun applyEvent(existing: UserModel?): UserModel {
        requireNotNull(existing) { "User must exist" }
        return existing.copy(
            currency = currency,
            updatedAt = createdAt,
            version = version
        )
    }
}

data class UserPhoneNumberChanged(
    val phoneNumber: String?,
    override val createdAt: Instant,
    override val version: Int,
    override val streamId: UUID,
    override val createdBy: UUID,
) : IUserEvent {
    override fun toEntity(): UserEvent {
        return UserEvent(
            uid = null,
            streamId = streamId,
            displayName = null,
            phoneNumber = phoneNumber,
            email = null,
            photoUrl = null,
            emailVerified = null,
            currency = null,
            createdAt = createdAt,
            createdBy = createdBy,
            version = version,
            eventType = UserEventType.PHONE_NUMBER_CHANGED
        )
    }

    override fun applyEvent(existing: UserModel?): UserModel {
        requireNotNull(existing) { "User must exist" }
        return existing.copy(
            phoneNumber = phoneNumber,
            updatedAt = createdAt,
            version = version
        )
    }
}

data class UserDeleted(
    override val createdAt: Instant,
    override val version: Int,
    override val streamId: UUID,
    override val createdBy: UUID,
) : IUserEvent {
    override fun toEntity(): UserEvent {
        return UserEvent(
            uid = null,
            streamId = streamId,
            displayName = null,
            phoneNumber = null,
            email = null,
            photoUrl = null,
            emailVerified = null,
            currency = null,
            createdAt = createdAt,
            createdBy = createdBy,
            version = version,
            eventType = UserEventType.USER_DELETED
        )
    }

    override fun applyEvent(existing: UserModel?): UserModel {
        requireNotNull(existing) { "User must exist" }
        return existing.copy(
            streamId = streamId,
            uid = existing.uid,
            displayName = "",
            phoneNumber = null,
            email = null,
            photoUrl = null,
            currency = null,
            emailVerified = false,
            createdAt = createdAt,
            updatedAt = createdAt,
            version = version,
            deleted = true
        )
    }
}

data class UserDisplayNameChanged(
    val displayName: String,
    override val createdAt: Instant,
    override val version: Int,
    override val streamId: UUID,
    override val createdBy: UUID,
) : IUserEvent {
    override fun toEntity(): UserEvent {
        return UserEvent(
            uid = null,
            streamId = streamId,
            displayName = displayName,
            phoneNumber = null,
            email = null,
            photoUrl = null,
            emailVerified = null,
            currency = null,
            createdAt = createdAt,
            createdBy = createdBy,
            version = version,
            eventType = UserEventType.DISPLAY_NAME_CHANGED
        )
    }

    override fun applyEvent(existing: UserModel?): UserModel {
        requireNotNull(existing) { "User must exist" }
        return existing.copy(
            displayName = displayName,
            updatedAt = createdAt,
            version = version
        )
    }
}

enum class UserEventType {
    USER_CREATED,
    USER_DELETED,
    CURRENCY_CHANGED,
    PHONE_NUMBER_CHANGED,
    DISPLAY_NAME_CHANGED,
}