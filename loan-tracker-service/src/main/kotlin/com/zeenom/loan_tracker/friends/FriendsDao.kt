package com.zeenom.loan_tracker.friends

import com.fasterxml.jackson.databind.ObjectMapper
import com.zeenom.loan_tracker.common.AmountDto
import com.zeenom.loan_tracker.common.SecondInstant
import com.zeenom.loan_tracker.common.r2dbc.toClass
import com.zeenom.loan_tracker.common.r2dbc.toJson
import com.zeenom.loan_tracker.users.UserRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
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

    suspend fun findAllByUserId(userId: String): FriendsDto = coroutineScope {
        val friendsEntities = async { friendRepository.findAllFriendsByUid(userId).collectList().awaitSingle() }

        friendsEntities.await().map {
            FriendDto(
                photoUrl = it.photoUrl,
                name = it.friendDisplayName,
                email = it.friendEmail,
                phoneNumber = it.friendPhoneNumber,
                loanAmount = it.friendTotalAmountsDto?.let {
                    val totalAmounts = it.toClass(
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
        }.let { FriendsDto(friends = it) }
    }

    suspend fun saveFriend(uid: String, friendDto: FriendDto): Unit = coroutineScope {

        if (friendDto.email == null && friendDto.phoneNumber == null) {
            throw IllegalArgumentException("At least one of userId, email or phoneNumber must be provided")
        }

        val user = async { userRepository.findByUid(uid).awaitSingle() }
        val friend = async {
            friendDto.email?.let { userRepository.findByEmail(it).awaitSingleOrNull() }
                ?: friendDto.phoneNumber?.let { userRepository.findByPhoneNumber(it).awaitSingleOrNull() }
        }

        friendRepository.save(
            UserFriendEntity(
                userId = user.await().id!!,
                friendId = friend.await()?.id,
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
                updatedAt = secondInstant.now(),
                friendDisplayName = friendDto.name,
                friendEmail = friendDto.email,
                friendPhoneNumber = friendDto.phoneNumber
            )
        ).awaitSingle()
    }
}