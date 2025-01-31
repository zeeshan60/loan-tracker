package com.zeenom.loan_tracker.dtos

import com.zeenom.loan_tracker.controllers.Paginated

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