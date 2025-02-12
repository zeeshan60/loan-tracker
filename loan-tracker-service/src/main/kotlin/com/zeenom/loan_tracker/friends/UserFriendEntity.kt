package com.zeenom.loan_tracker.friends

import io.r2dbc.postgresql.codec.Json
import org.springframework.data.annotation.Id
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.relational.core.mapping.Table
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.*

@Table("user_friends")
data class UserFriendEntity(
    @Id val id: UUID? = null,
    val userId: UUID,
    val friendId: UUID?,
    val friendEmail: String?,
    val friendPhoneNumber: String?,
    val friendDisplayName: String,
    val friendTotalAmountsDto: Json?,
    val createdAt: Instant,
    val updatedAt: Instant,
)

@Repository
interface FriendRepository : CoroutineCrudRepository<UserFriendEntity, UUID> {
    @Query(
        """
        SELECT u.photo_url, uf.friend_display_name, uf.friend_phone_number, uf.friend_email, uf.friend_total_amounts_dto, uf.updated_at 
        FROM user_friends uf
        LEFT JOIN users u ON uf.friend_id = u.id
        INNER JOIN users owner ON uf.user_id = owner.id
        WHERE owner.uid = :uid
    """
    )
    suspend fun findAllFriendsByUid(uid: String): List<FriendSelectEntity>

    @Query(
        """
        SELECT * 
        FROM user_friends uf
        WHERE (uf.friend_email = :email AND :email IS NOT NULL) OR (uf.friend_phone_number = :phone AND :phone IS NOT NULL)
    """
    )
    suspend fun findAllByEmailOrPhone(email: String?, phone: String?): List<UserFriendEntity>

    @Query(
        """
        SELECT u.id as owner_id, u.phone_number as owner_phone_number, u.email as owner_email, u.display_name as owner_display_name, uf.friend_total_amounts_dto
        FROM users u
        INNER JOIN user_friends uf ON u.id = uf.user_id
        WHERE (uf.friend_email = :email AND :email IS NOT NULL) OR (uf.friend_phone_number = :phone AND :phone IS NOT NULL)
    """
    )
    suspend fun findOwnersByMyEmailOrPhone(email: String?, phone: String?): List<UserEntityWithFriendAmountEntity>
    suspend fun findByUserIdAndFriendId(id: UUID, friendId: UUID): UserFriendEntity?
}

data class UserEntityWithFriendAmountEntity(
    val ownerId: UUID,
    val ownerPhoneNumber: String?,
    val ownerEmail: String?,
    val ownerDisplayName: String,
    val friendTotalAmountsDto: Json?,
)

data class FriendSelectEntity(
    val photoUrl: String?,
    val friendDisplayName: String,
    val friendEmail: String?,
    val friendPhoneNumber: String?,
    val friendTotalAmountsDto: Json?,
    val updatedAt: Instant,
)