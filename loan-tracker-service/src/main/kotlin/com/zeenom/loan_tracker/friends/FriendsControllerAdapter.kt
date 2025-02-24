package com.zeenom.loan_tracker.friends

import com.zeenom.loan_tracker.common.LoanAmountResponseAdapter
import com.zeenom.loan_tracker.common.Paginated
import com.zeenom.loan_tracker.transactions.AmountResponse
import org.springframework.stereotype.Service

@Service
class FriendsControllerAdapter(val loanAmountResponseAdapter: LoanAmountResponseAdapter) {
    fun FriendsDto.toResponse() = FriendsResponse(
        friends = this.friends.map { it.toResponse() }
    )

    fun FriendDto.toResponse() = FriendResponse(
        photoUrl = this.photoUrl,
        name = this.name,
        loanAmount = this.loanAmount?.let { loanAmountResponseAdapter.fromDto(it) },
        friendId = this.friendId,
        mainBalance = AmountResponse(
            amount = 200.0.toBigDecimal(),
            currency = "SGD",
            isOwed = true
        ),
        otherBalances = listOf(
            AmountResponse(
                amount = 20000.0.toBigDecimal(),
                currency = "PKR",
                isOwed = true
            ),AmountResponse(
                amount = 200.0.toBigDecimal(),
                currency = "USD",
                isOwed = false
            )
        )
    )

    fun fromRequestToDto(createFriendRequest: CreateFriendRequest): CreateFriendDto {
        return CreateFriendDto(
            email = createFriendRequest.email,
            phoneNumber = createFriendRequest.phoneNumber,
            name = createFriendRequest.name
        )
    }

    fun fromDtoToPaginatedResponse(friendsDto: FriendsDto): Paginated<FriendsResponse> {
        return Paginated(
            data = friendsDto.toResponse(),
            next = friendsDto.next
        )
    }
}