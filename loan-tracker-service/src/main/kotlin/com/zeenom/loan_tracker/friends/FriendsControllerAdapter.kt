package com.zeenom.loan_tracker.friends

import com.zeenom.loan_tracker.common.Paginated
import org.springframework.stereotype.Service
import java.util.UUID

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

    fun fromRequestToDto(friendRequest: FriendRequest): CreateFriendDto {
        return CreateFriendDto(
            email = friendRequest.email,
            phoneNumber = friendRequest.phoneNumber,
            name = friendRequest.name
        )
    }

    fun fromRequestToDto(friendRequest: FriendRequest, friendId: UUID): UpdateFriendDto {
        return UpdateFriendDto(
            email = friendRequest.email,
            phoneNumber = friendRequest.phoneNumber,
            name = friendRequest.name,
            friendId = friendId
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