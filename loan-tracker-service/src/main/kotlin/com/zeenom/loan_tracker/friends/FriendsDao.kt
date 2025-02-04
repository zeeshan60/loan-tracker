package com.zeenom.loan_tracker.friends

import com.fasterxml.jackson.databind.ObjectMapper
import com.zeenom.loan_tracker.common.AmountDto
import com.zeenom.loan_tracker.common.SecondInstant
import com.zeenom.loan_tracker.common.r2dbc.toClass
import com.zeenom.loan_tracker.common.r2dbc.toJson
import com.zeenom.loan_tracker.users.UserEntity
import com.zeenom.loan_tracker.users.UserRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
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
        val friendsEntities = friendRepository.findAllFriendsByUid(userId).collectList().awaitSingle()

        friendsEntities.map {
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

    suspend fun saveFriend(uid: String, friendDto: CreateFriendDto): Unit = coroutineScope {

        if (friendDto.email == null && friendDto.phoneNumber == null) {
            throw IllegalArgumentException("At least one of userId, email or phoneNumber must be provided")
        }

        val userDef = async { userRepository.findByUid(uid).awaitSingle() }
        val friendDef = async {
            friendDto.email?.let { userRepository.findByEmail(it).awaitSingleOrNull() }
                ?: friendDto.phoneNumber?.let { userRepository.findByPhoneNumber(it).awaitSingleOrNull() }
        }

        val defs = awaitAll(userDef, friendDef)
        val user = defs[0] as UserEntity
        val friend = defs[1]
        friendRepository.save(
            UserFriendEntity(
                userId = user.id!!,
                friendId = friend?.id,
                friendTotalAmountsDto = null,
                createdAt = secondInstant.now(),
                updatedAt = secondInstant.now(),
                friendDisplayName = friendDto.name,
                friendEmail = friendDto.email,
                friendPhoneNumber = friendDto.phoneNumber
            )
        ).awaitSingle()
    }
}