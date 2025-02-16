package com.zeenom.loan_tracker.friends

import com.fasterxml.jackson.databind.ObjectMapper
import com.zeenom.loan_tracker.common.AmountDto
import com.zeenom.loan_tracker.common.SecondInstant
import com.zeenom.loan_tracker.common.r2dbc.toClass
import com.zeenom.loan_tracker.common.r2dbc.toJson
import com.zeenom.loan_tracker.users.UserEventRepository
import com.zeenom.loan_tracker.users.UserEvent
import io.r2dbc.postgresql.codec.Json
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

interface IFriendsDao {
    suspend fun findAllByUserId(userId: String): FriendsDto

    @Transactional
    suspend fun saveFriend(uid: String, friendDto: CreateFriendDto)

    @Transactional
    suspend fun makeMyOwnersMyFriends(uid: String)
}

@Service
class FriendsDao(
    private val friendRepository: FriendRepository,
    private val objectMapper: ObjectMapper,
    private val userRepository: UserEventRepository,
    private val secondInstant: SecondInstant,
) : IFriendsDao {

    override suspend fun findAllByUserId(userId: String): FriendsDto = coroutineScope {
        val friendsEntities = friendRepository.findAllFriendsByUid(userId)

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
    override suspend fun saveFriend(uid: String, friendDto: CreateFriendDto) {

        if (friendDto.email == null && friendDto.phoneNumber == null) {
            throw IllegalArgumentException("At least one of userId, email or phoneNumber must be provided")
        }

        val user = userRepository.findByUid(uid) ?: throw IllegalArgumentException("User not found")
        if (user.email == friendDto.email || user.phoneNumber == friendDto.phoneNumber) {
            throw IllegalArgumentException("User cannot be friend of oneself")
        }
        val friendId = friendDto.email?.let { userRepository.findByEmail(it)?.id }
            ?: friendDto.phoneNumber?.let { userRepository.findByPhoneNumber(it)?.id }
        addFriendToUser(user = user, friendId = friendId, friendDto = friendDto)
        friendId?.let { addUserToFriend(friendId = it, user = user) }
    }

    private suspend fun addFriendToUser(
        user: UserEvent,
        friendId: UUID?,
        friendDto: CreateFriendDto,
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
        )
    }

    private suspend fun addUserToFriend(friendId: UUID, user: UserEvent) {
        val existing = friendRepository.findByUserIdAndFriendId(
            friendId,
            user.id ?: throw IllegalArgumentException("User Not found")
        )
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
            )
        }
    }

    @Transactional
    override suspend fun makeMyOwnersMyFriends(uid: String) {
        val userEntity = userRepository.findByUid(uid)
            ?: throw IllegalArgumentException("User not found")

        updateMyIdInMyOwnersRecord(userEntity)
        addMyOwnersAsMyFriends(userEntity)
    }

    private suspend fun addMyOwnersAsMyFriends(
        userEntity: UserEvent,
    ) {
        val owners = friendRepository.findOwnersByMyEmailOrPhone(userEntity.email, userEntity.phoneNumber)
        val newUserFriends = owners.map { owner ->
            UserFriendEntity(
                userId = userEntity.id ?: throw IllegalArgumentException("User not found"),
                friendId = owner.ownerId,
                friendEmail = owner.ownerEmail,
                friendPhoneNumber = owner.ownerPhoneNumber,
                friendDisplayName = owner.ownerDisplayName,
                friendTotalAmountsDto = owner.friendTotalAmountsDto?.let { revertAmount(it) },
                createdAt = secondInstant.now(),
                updatedAt = secondInstant.now()
            )
        }
        if (newUserFriends.isEmpty()) return
        friendRepository.saveAll(newUserFriends).toList()
    }

    private fun revertAmount(friendAmountsDtoJson: Json) =
        friendAmountsDtoJson.let {
            val amountDto = it.toClass(
                objectMapper = objectMapper,
                FriendTotalAmountsDto::class.java
            )
            FriendTotalAmountsDto(amountsPerCurrency = amountDto.amountsPerCurrency.map {
                AmountDto(
                    amount = it.amount,
                    currency = it.currency,
                    isOwed = !it.isOwed
                )
            })
        }.toJson(objectMapper = objectMapper)

    private suspend fun updateMyIdInMyOwnersRecord(userEntity: UserEvent) {
        val userFriendEntities =
            friendRepository.findAllByEmailOrPhone(userEntity.email, userEntity.phoneNumber)

        val updatedFriends = userFriendEntities.mapNotNull {
            if (it.friendId == null && it.id != null) {
                it.copy(friendId = userEntity.id)
            } else null
        }
        if (updatedFriends.isEmpty()) return
        friendRepository.saveAll(updatedFriends).toList()
    }
}