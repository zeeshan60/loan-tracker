package com.zeenom.loan_tracker.services

import com.zeenom.loan_tracker.controllers.FriendResponse
import com.zeenom.loan_tracker.controllers.Paginated

fun FriendsDto.toResponse() = com.zeenom.loan_tracker.controllers.FriendsResponse(
    friends = this.friends.map { it.toResponse() }
)

fun FriendDto.toResponse() = FriendResponse(
    photoUrl = this.photoUrl,
    name = this.name,
    loanAmount = this.loanAmount.toResponse()
)

fun LoanAmountDto.toResponse() = com.zeenom.loan_tracker.controllers.LoanAmountResponse(
    amount = this.amount,
    isOwed = this.isOwed
)

fun <T> T.toPaginated(next: String? = null) = Paginated(this, next)