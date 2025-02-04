package com.zeenom.loan_tracker.friends

import com.zeenom.loan_tracker.common.AmountDto
import org.springframework.stereotype.Service

data class FriendTotalAmountsDto(
    val amountsPerCurrency: List<AmountDto>
)

data class UserFriendDto(
    val userId: String,
    val friendId: String,
    val friendTotalAmountsDto: FriendTotalAmountsDto?
)

@Service
class FriendsDao(private val friendRepository: FriendRepository, private val friendEntityAdapter: FriendEntityAdapter) {
    fun save(friendDto: UserFriendDto) = friendRepository.save(friendEntityAdapter.fromDto(friendDto))

    fun findAllByUserId(userId: String) = friendRepository.findAllByUserId(userId).map { friendEntityAdapter.toDto(it) }

    fun findAllByFriendId(friendId: String) =
        friendRepository.findAllByFriendId(friendId).map { friendEntityAdapter.toDto(it) }
}