package com.zeenom.loan_tracker.friends

import com.fasterxml.jackson.databind.ObjectMapper
import com.zeenom.loan_tracker.common.AmountDto
import com.zeenom.loan_tracker.common.SecondInstant
import com.zeenom.loan_tracker.common.r2dbc.toClass
import com.zeenom.loan_tracker.users.UserEntity
import com.zeenom.loan_tracker.users.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

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

    @Transactional
    suspend fun saveFriend(uid: String, friendDto: CreateFriendDto) {

        if (friendDto.email == null && friendDto.phoneNumber == null) {
            throw IllegalArgumentException("At least one of userId, email or phoneNumber must be provided")
        }

        val user = userRepository.findByUid(uid).awaitSingleOrNull() ?: throw IllegalArgumentException("User not found")
        val friendId = friendDto.email?.let { userRepository.findByEmail(it).awaitSingleOrNull()?.id }
            ?: friendDto.phoneNumber?.let { userRepository.findByPhoneNumber(it).awaitSingleOrNull()?.id }
        addFriendToUser(user = user, friendId = friendId, friendDto = friendDto)
        friendId?.let { addUserToFriend(friendId = it, user = user) }
    }

    private suspend fun addFriendToUser(
        user: UserEntity,
        friendId: UUID?,
        friendDto: CreateFriendDto
    ) {
        friendRepository.save(
            UserFriendEntity(
                userId = user.id ?: throw IllegalArgumentException("User not found"),
                friendId = friendId,
                friendTotalAmountsDto = null,
                createdAt = secondInstant.now(),
                updatedAt = secondInstant.now(),
                friendDisplayName = friendDto.name,
                friendEmail = friendDto.email,
                friendPhoneNumber = friendDto.phoneNumber
            )
        ).awaitSingle()
    }

    private suspend fun addUserToFriend(friendId: UUID, user: UserEntity) {
        val existing = friendRepository.findByUserIdAndFriendId(
            friendId,
            user.id ?: throw IllegalArgumentException("User Not found")
        ).awaitSingleOrNull()
        if (existing == null) {
            friendRepository.save(
                UserFriendEntity(
                    userId = friendId,
                    friendId = user.id,
                    friendTotalAmountsDto = null,
                    createdAt = secondInstant.now(),
                    updatedAt = secondInstant.now(),
                    friendDisplayName = user.displayName,
                    friendEmail = user.email,
                    friendPhoneNumber = user.phoneNumber
                )
            ).awaitSingle()
        }
    }

    @Transactional
    suspend fun makeMyOwnersMyFriends(uid: String) = withContext(Dispatchers.IO) {
        val userEntity = userRepository.findByUid(uid).awaitSingleOrNull()
            ?: throw IllegalArgumentException("User not found")

        updateMyIdInMyOwnersRecord(userEntity)
        addMyOwnersAsMyFriends(userEntity)
    }

    private suspend fun addMyOwnersAsMyFriends(
        userEntity: UserEntity
    ) {
        val owners = friendRepository.findOwnersByMyEmailOrPhone(userEntity.email, userEntity.phoneNumber)
            .collectList()
            .awaitSingle()
        val newUserFriends = owners.mapNotNull { owner ->
            UserFriendEntity(
                userId = userEntity.id ?: throw IllegalArgumentException("User not found"),
                friendId = owner.id,
                friendEmail = owner.email,
                friendPhoneNumber = owner.phoneNumber,
                friendDisplayName = owner.displayName,
                friendTotalAmountsDto = null,
                createdAt = secondInstant.now(),
                updatedAt = secondInstant.now()
            )
        }
        if (newUserFriends.isEmpty()) return
        friendRepository.saveAll(newUserFriends).collectList().awaitSingle()
    }

    private suspend fun updateMyIdInMyOwnersRecord(userEntity: UserEntity) {
        val userFriendEntities = friendRepository.findAllByEmailOrPhone(userEntity.email, userEntity.phoneNumber)
            .collectList()
            .awaitSingle()

        val updatedFriends = userFriendEntities.mapNotNull {
            if (it.friendId == null && it.id != null) {
                it.copy(friendId = userEntity.id)
            } else null
        }
        if (updatedFriends.isEmpty()) return
        friendRepository.saveAll(updatedFriends).collectList().awaitSingle()
    }
}