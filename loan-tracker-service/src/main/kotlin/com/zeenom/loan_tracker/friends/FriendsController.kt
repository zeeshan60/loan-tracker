package com.zeenom.loan_tracker.friends

import com.zeenom.loan_tracker.common.MessageResponse
import com.zeenom.loan_tracker.common.Paginated
import com.zeenom.loan_tracker.common.PaginationDto
import com.zeenom.loan_tracker.events.CommandDto
import com.zeenom.loan_tracker.events.CommandType
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/friends")
class FriendsController(
    val friendsQuery: FriendsQuery,
    val friendsAdapter: FriendsControllerAdapter,
    private val createFriendCommand: CreateFriendCommand,
    private val friendQuery: FriendQuery,
) {

    val logger: Logger = LoggerFactory.getLogger(FriendsController::class.java)

    @Operation(summary = "Get friends", description = "Retrieve a list of friends with pagination")
    @GetMapping
    suspend fun getFriends(
        @Parameter(description = "Pagination token for the next set of results")
        @RequestParam next: String? = null,
        @AuthenticationPrincipal userId: String,
    ): Paginated<FriendsResponse> {
        logger.info("Getting friends for user $userId")
        return friendsQuery.execute(PaginationDto(input = userId, next = next)).let { result ->
            friendsAdapter.fromDtoToPaginatedResponse(result)
        }
    }

    @Operation(summary = "Add friend", description = "Add a friend")
    @PostMapping("/add")
    suspend fun addFriend(
        @RequestBody friendRequest: CreateFriendRequest,
        @AuthenticationPrincipal userId: String,
    ): FriendResponse {
        logger.info("Adding friend for user $userId")
        createFriendCommand.execute(
            CommandDto(
                commandType = CommandType.ADD_FRIEND,
                payload = friendsAdapter.fromRequestToDto(friendRequest),
                userId = userId
            )
        )
        return friendQuery.execute(
            FriendQueryDto(
                userId = userId,
                friendEmail = friendRequest.email,
                friendPhoneNumber = friendRequest.phoneNumber,
            )
        ).let {
            friendsAdapter.fromDtoToResponse(it)
        }
    }

    @Operation(summary = "Settle up", description = "Settle up with a friend")
    @PostMapping("/settle-up")
    suspend fun settleUp(
        @AuthenticationPrincipal userId: String,
    ): FriendResponse {
        TODO()
    }
}
