package com.zeenom.loan_tracker.friends

import io.r2dbc.postgresql.codec.Json
import org.springframework.data.annotation.Id
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.relational.core.mapping.Table
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
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
    val updatedAt: Instant
)

@Repository
interface FriendRepository : ReactiveCrudRepository<UserFriendEntity, UUID> {
    @Query(
        """
        SELECT u.photo_url, uf.friend_display_name, uf.friend_phone_number, uf.friend_email, uf.friend_total_amounts_dto, uf.updated_at 
        FROM user_friends uf
        LEFT JOIN users u ON uf.friend_id = u.id
        INNER JOIN users owner ON uf.user_id = owner.id
        WHERE owner.uid = :uid
    """
    )
    fun findAllFriendsByUid(uid: String): Flux<FriendSelectEntity>

}

data class FriendSelectEntity(
    val photoUrl: String?,
    val friendDisplayName: String,
    val friendEmail: String?,
    val friendPhoneNumber: String?,
    val friendTotalAmountsDto: Json?,
    val updatedAt: Instant
)