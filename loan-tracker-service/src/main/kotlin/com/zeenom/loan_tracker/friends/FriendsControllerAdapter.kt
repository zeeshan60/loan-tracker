package com.zeenom.loan_tracker.friends

import com.zeenom.loan_tracker.common.Paginated
import com.zeenom.loan_tracker.transactions.AmountResponse
import org.springframework.stereotype.Service

@Service
class FriendsControllerAdapter {
    fun FriendsWithAllTimeBalancesDto.toResponse() = FriendsResponse(
        friends = this.friends.map { it.toResponse() },
        balance = this.balance
    )

    fun FriendDto.toResponse(): FriendResponse {
        val mainBalance = this.balances.firstOrNull { it.currency == this.mainCurrency } ?: this.balances.firstOrNull()
        return FriendResponse(
            photoUrl = this.photoUrl,
            name = this.name,
            friendId = this.friendId,
            mainBalance = mainBalance?.let {
                AmountResponse(
                    amount = it.amount,
                    currency = it.currency.currencyCode,
                    isOwed = it.isOwed
                )
            },
            otherBalances = this.balances.filter { it.currency != mainBalance?.currency }.map {
                AmountResponse(
                    amount = it.amount,
                    currency = it.currency.currencyCode,
                    isOwed = it.isOwed
                )
            }
        )
    }

    fun fromRequestToDto(createFriendRequest: CreateFriendRequest): CreateFriendDto {
        return CreateFriendDto(
            email = createFriendRequest.email,
            phoneNumber = createFriendRequest.phoneNumber,
            name = createFriendRequest.name
        )
    }

    fun fromDtoToPaginatedResponse(friendsDto: FriendsWithAllTimeBalancesDto): Paginated<FriendsResponse> {
        return Paginated(
            data = friendsDto.toResponse(),
            next = null
        )
    }

    fun fromDtoToResponse(friendDto: FriendDto): FriendResponse = friendDto.toResponse()
}