package com.zeenom.loan_tracker.friends

import com.zeenom.loan_tracker.common.Paginated
import org.springframework.stereotype.Service

@Service
class FriendsControllerAdapter {
    fun FriendsWithAllTimeBalancesDto.toResponse() = FriendsResponse(
        friends = this.friends.map { it.toResponse() },
        balance = this.balance.toResponse()
    )

    fun FriendDto.toResponse(): FriendResponse {
        return FriendResponse(
            photoUrl = this.photoUrl,
            name = this.name,
            friendId = this.friendId,
            mainBalance = this.balances.main?.toResponse(),
            otherBalances = this.balances.other.map { it.toResponse() },
            email = this.email,
            phone = this.phoneNumber,
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