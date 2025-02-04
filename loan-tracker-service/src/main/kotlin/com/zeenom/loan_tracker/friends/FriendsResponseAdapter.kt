package com.zeenom.loan_tracker.friends

import com.zeenom.loan_tracker.common.LoanAmountResponseAdapter
import com.zeenom.loan_tracker.common.Paginated
import org.springframework.stereotype.Service

@Service
class FriendsResponseAdapter(val loanAmountResponseAdapter: LoanAmountResponseAdapter) {
    fun FriendsDto.toResponse() = FriendsResponse(
        friends = this.friends.map { it.toResponse() }
    )

    fun FriendDto.toResponse() = FriendResponse(
        photoUrl = this.photoUrl,
        name = this.name,
        loanAmount = this.loanAmount?.let { loanAmountResponseAdapter.fromDto(it) }
    )

    fun fromDto(friendsDto: FriendsDto): Paginated<FriendsResponse> {
        return Paginated(
            data = friendsDto.toResponse(),
            next = friendsDto.next
        )
    }
}