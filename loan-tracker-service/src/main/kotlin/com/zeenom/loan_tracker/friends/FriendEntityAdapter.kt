package com.zeenom.loan_tracker.friends

import com.fasterxml.jackson.databind.ObjectMapper
import com.zeenom.loan_tracker.common.SecondInstant
import com.zeenom.loan_tracker.common.r2dbc.fromJson
import com.zeenom.loan_tracker.common.r2dbc.toJson
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.*

@Component
class FriendEntityAdapter(private val objectMapper: ObjectMapper, private val secondInstant: SecondInstant) {
    fun fromDto(friendDto: UserFriendDto, id: UUID? = null, createdAt: Instant? = null, updatedAt: Instant? = null) =
        UserFriendEntity(
            id = id,
            userId = UUID.fromString(friendDto.userId),
            friendId = UUID.fromString(friendDto.friendId),
            friendTotalAmountsDto = friendDto.friendTotalAmountsDto?.toJson(objectMapper),
            createdAt = secondInstant.now(),
            updatedAt = secondInstant.now()
        )

    fun toDto(entity: UserFriendEntity) = UserFriendDto(
        userId = entity.userId.toString(),
        friendId = entity.friendId.toString(),
        friendTotalAmountsDto = entity.friendTotalAmountsDto?.fromJson(objectMapper, FriendTotalAmountsDto::class.java)
    )
}