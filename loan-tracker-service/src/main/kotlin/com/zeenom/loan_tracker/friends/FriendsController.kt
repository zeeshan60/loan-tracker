package com.zeenom.loan_tracker.friends

import com.zeenom.loan_tracker.common.Paginated
import com.zeenom.loan_tracker.common.PaginationDto
import com.zeenom.loan_tracker.events.CommandDto
import com.zeenom.loan_tracker.events.CommandType
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import jakarta.validation.Valid
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.propertyeditors.StringTrimmerEditor
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.WebDataBinder
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/v1/friends")
class FriendsController(
    val friendsQuery: FriendsQuery,
    val friendsAdapter: FriendsControllerAdapter,
    private val createFriendCommand: CreateFriendCommand,
    private val updateFriendCommand: UpdateFriendCommand,
    private val deleteFriendCommand: DeleteFriendCommand,
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
        @Valid @RequestBody friendRequest: FriendRequest,
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

    @Operation(summary = "Update friend", description = "Update a friend's details")
    @PutMapping("/{friendId}")
    suspend fun updateFriend(
        @PathVariable friendId: UUID,
        @Valid @RequestBody friendRequest: UpdateFriendRequest,
        @AuthenticationPrincipal userId: String,
    ): FriendResponse {
        logger.info("Updating friend $friendId for user $userId")
        updateFriendCommand.execute(
            CommandDto(
                commandType = CommandType.UPDATE_FRIEND,
                payload = friendsAdapter.fromRequestToDto(friendRequest, friendId),
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

    @Operation(summary = "Delete friend by id", description = "Delete a friend by their ID")
    @DeleteMapping("/{friendId}")
    suspend fun deleteFriend(
        @PathVariable friendId: UUID,
        @AuthenticationPrincipal userId: String,
    ): String {
        logger.info("Deleting friend $friendId for user $userId")
        deleteFriendCommand.execute(
            CommandDto(
                commandType = CommandType.DELETE_FRIEND,
                payload = DeleteFriendDto(friendId = friendId),
                userId = userId
            )
        )
        return "Friend with ID $friendId deleted successfully"
    }

}
