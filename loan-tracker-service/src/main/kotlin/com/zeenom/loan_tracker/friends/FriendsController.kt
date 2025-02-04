package com.zeenom.loan_tracker.friends

import com.zeenom.loan_tracker.common.MessageResponse
import com.zeenom.loan_tracker.common.Paginated
import com.zeenom.loan_tracker.common.PaginationDto
import com.zeenom.loan_tracker.events.CommandEventService
import com.zeenom.loan_tracker.events.EventDto
import com.zeenom.loan_tracker.events.EventType
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import org.slf4j.LoggerFactory
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1")
class FriendsController(
    val queryFriendsService: QueryFriendsService,
    val eventService: CommandEventService,
    val friendsAdapter: FriendsControllerAdapter
) {

    val logger = LoggerFactory.getLogger(FriendsController::class.java)

    @Operation(summary = "Get friends", description = "Retrieve a list of friends with pagination")
    @GetMapping("/friends")
    suspend fun getFriends(
        @Parameter(description = "Pagination token for the next set of results")
        @RequestParam next: String? = null,
        @AuthenticationPrincipal userId: String
    ): Paginated<FriendsResponse> {
        logger.info("Getting friends for user $userId")
        return queryFriendsService.execute(PaginationDto(next = next)).let { result ->
            friendsAdapter.fromDtoToPaginatedResponse(result)
        }
    }

    @Operation(summary = "Add friend", description = "Add a friend")
    @PostMapping("/friends/add")
    suspend fun addFriend(
        @RequestBody friendRequest: CreateFriendRequest,
        @AuthenticationPrincipal userId: String
    ): MessageResponse {
        logger.info("Adding friend for user $userId")
        eventService.execute(
            EventDto(
                event = EventType.ADD_FRIEND,
                payload = friendsAdapter.fromRequestToDto(friendRequest),
                userId = userId
            )
        )
        return MessageResponse("Friend added successfully")
    }
}
