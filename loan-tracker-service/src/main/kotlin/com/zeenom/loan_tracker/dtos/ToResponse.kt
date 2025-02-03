package com.zeenom.loan_tracker.dtos

import com.zeenom.loan_tracker.friends.dtos.FriendDto
import com.zeenom.loan_tracker.friends.dtos.FriendResponse
import com.zeenom.loan_tracker.friends.dtos.FriendsDto
import com.zeenom.loan_tracker.friends.dtos.FriendsResponse

fun FriendsDto.toResponse() = FriendsResponse(
    friends = this.friends.map { it.toResponse() }
)

fun FriendDto.toResponse() = FriendResponse(
    photoUrl = this.photoUrl,
    name = this.name,
    loanAmount = this.loanAmount.toResponse()
)

fun LoanAmountDto.toResponse() = LoanAmountResponse(
    amount = this.amount,
    isOwed = this.isOwed
)

fun <T> T.toPaginated(next: String? = null) = Paginated(this, next)