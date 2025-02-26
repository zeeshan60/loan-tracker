package com.zeenom.loan_tracker.friends

import java.util.*

data class FriendUserDto(
    val friendUid: String?,
    val friendStreamId: UUID,
    val email: String?,
    val phoneNumber: String?,
    val photoUrl: String?,
    val name: String,
)