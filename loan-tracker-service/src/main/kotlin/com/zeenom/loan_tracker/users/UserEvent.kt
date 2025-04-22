package com.zeenom.loan_tracker.users

import com.zeenom.loan_tracker.common.events.IEvent
import com.zeenom.loan_tracker.transactions.IEventAble
import org.springframework.data.annotation.Id
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
    val uid: String,
    val streamId: UUID,
    val displayName: String?,
    val phoneNumber: String?,
    val email: String?,
    val photoUrl: String?,
    val emailVerified: Boolean?,
    val currency: String?,
    val createdAt: Instant,
    val version: Int,
    val eventType: UserEventType,
) : IEventAble<UserModel> {
    override fun toEvent(): IEvent<UserModel> {
        return when (eventType) {
            UserEventType.USER_CREATED -> UserCreated(
                userId = uid,
                displayName = displayName ?: throw IllegalStateException("Display name is required"),
                phoneNumber = phoneNumber,
                email = email,
                photoUrl = photoUrl,
                emailVerified = emailVerified ?: throw IllegalStateException("Email verified is required"),
                createdAt = createdAt,
                version = version,
                streamId = streamId,
                createdBy = uid
            )

            UserEventType.CURRENCY_CHANGED -> UserCurrencyChanged(
                userId = uid,
                currency = currency ?: throw IllegalStateException("Currency is required"),
                createdAt = createdAt,
                version = version,
                streamId = streamId,
                createdBy = uid
            )

            UserEventType.PHONE_NUMBER_CHANGED -> UserPhoneNumberChanged(
                userId = uid,
                phoneNumber = phoneNumber,
                createdAt = createdAt,
                version = version,
                streamId = streamId,
                createdBy = uid
            )

            UserEventType.DISPLAY_NAME_CHANGED -> UserDisplayNameChanged(
                userId = uid,
                displayName = displayName ?: throw IllegalStateException("Display name is required"),
                createdAt = createdAt,
                version = version,
                streamId = streamId,
                createdBy = uid
            )
        }
    }
}

@Repository
interface UserModelRepository : CoroutineCrudRepository<UserModel, UUID>

@Table("user_model")
data class UserModel(
    @Id
    val streamId: UUID,
    val uid: String,
    val displayName: String,
    val phoneNumber: String?,
    val currency: String?,
    val email: String?,
    val photoUrl: String?,
    val emailVerified: Boolean?,
    val createdAt: Instant,
    val updatedAt: Instant,
    val version: Int,
)

data class UserCreated(
    val displayName: String,
    val phoneNumber: String?,
    val email: String?,
    val photoUrl: String?,
    val emailVerified: Boolean,
    override val userId: String,
    override val createdAt: Instant,
    override val version: Int,
    override val streamId: UUID,
    override val createdBy: String,
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
            version = version,
            eventType = UserEventType.USER_CREATED
        )
    }

    override fun applyEvent(existing: UserModel?): UserModel {
        return UserModel(
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
            version = version
        )
    }
}

data class UserCurrencyChanged(
    val currency: String?,
    override val userId: String,
    override val createdAt: Instant,
    override val version: Int,
    override val streamId: UUID,
    override val createdBy: String,
) : IUserEvent {
    override fun toEntity(): UserEvent {
        return UserEvent(
            uid = userId,
            streamId = streamId,
            displayName = null,
            phoneNumber = null,
            email = null,
            photoUrl = null,
            emailVerified = null,
            currency = currency,
            createdAt = createdAt,
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
    override val userId: String,
    override val createdAt: Instant,
    override val version: Int,
    override val streamId: UUID,
    override val createdBy: String,
) : IUserEvent {
    override fun toEntity(): UserEvent {
        return UserEvent(
            uid = userId,
            streamId = streamId,
            displayName = null,
            phoneNumber = phoneNumber,
            email = null,
            photoUrl = null,
            emailVerified = null,
            currency = null,
            createdAt = createdAt,
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

data class UserDisplayNameChanged(
    val displayName: String,
    override val userId: String,
    override val createdAt: Instant,
    override val version: Int,
    override val streamId: UUID,
    override val createdBy: String,
) : IUserEvent {
    override fun toEntity(): UserEvent {
        return UserEvent(
            uid = userId,
            streamId = streamId,
            displayName = displayName,
            phoneNumber = null,
            email = null,
            photoUrl = null,
            emailVerified = null,
            currency = null,
            createdAt = createdAt,
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
    CURRENCY_CHANGED,
    PHONE_NUMBER_CHANGED,
    DISPLAY_NAME_CHANGED,
}