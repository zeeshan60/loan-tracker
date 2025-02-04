package com.zeenom.loan_tracker.friends

import com.fasterxml.jackson.databind.ObjectMapper
import com.zeenom.loan_tracker.common.AmountDto
import com.zeenom.loan_tracker.common.SecondInstant
import com.zeenom.loan_tracker.common.r2dbc.fromJson
import com.zeenom.loan_tracker.common.r2dbc.toJson
import com.zeenom.loan_tracker.users.UserEntity
import com.zeenom.loan_tracker.users.UserRepository
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.stereotype.Service

@Service
class FriendsDao(
    private val friendRepository: FriendRepository,
    private val objectMapper: ObjectMapper,
    private val userRepository: UserRepository,
    private val secondInstant: SecondInstant
) {

    suspend fun findAllByUserId(userId: String): FriendsDto {
        return friendRepository.findAllFriendsByUid(userId).map {
            FriendDto(
                userId = it.uid,
                photoUrl = it.photoUrl,
                name = it.displayName,
                email = it.email,
                phoneNumber = it.phoneNumber,
                loanAmount = it.friendTotalAmountsDto?.let {
                    val totalAmounts = it.fromJson(
                        objectMapper = objectMapper,
                        FriendTotalAmountsDto::class.java
                    )
                    totalAmounts.amountsPerCurrency.firstOrNull()?.let { amountDto ->
                        AmountDto(
                            amount = amountDto.amount,
                            currency = amountDto.currency,
                            isOwed = amountDto.isOwed

                        )
                    }
                }
            )
        }.collectList().awaitSingle().let {
            FriendsDto(friends = it)
        }
    }

    suspend fun saveFriend(uid: String, friendDto: FriendDto) {
        val user = userRepository.findByUid(uid).awaitSingle()
        val friend = userRepository.findByUid(friendDto.userId).awaitSingleOrNull() ?: userRepository.save(
            UserEntity(
                uid = friendDto.userId,
                email = friendDto.email,
                phoneNumber = friendDto.phoneNumber,
                displayName = friendDto.name,
                photoUrl = friendDto.photoUrl,
                emailVerified = false,
                createdAt = secondInstant.now(),
                updatedAt = secondInstant.now(),
                lastLoginAt = null
            )
        ).awaitSingle()


        friendRepository.save(
            UserFriendEntity(
                userId = user.id!!,
                friendId = friend?.id!!,
                friendTotalAmountsDto = friendDto.loanAmount?.let {
                    FriendTotalAmountsDto(
                        amountsPerCurrency = listOf(
                            AmountDto(
                                amount = it.amount,
                                currency = it.currency,
                                isOwed = it.isOwed
                            )
                        )
                    ).toJson(objectMapper)
                },
                createdAt = secondInstant.now(),
                updatedAt = secondInstant.now()
            )
        ).awaitSingle()
    }
}