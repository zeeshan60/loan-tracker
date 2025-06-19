package com.zeenom.loan_tracker.friends

import java.util.*

data class FriendUserDto(
    val friendUid: UUID?,
    val friendStreamId: UUID,
    val email: String?,
    val phoneNumber: String?,
    val photoUrl: String?,
    val name: String,
    val deleted: Boolean,
)