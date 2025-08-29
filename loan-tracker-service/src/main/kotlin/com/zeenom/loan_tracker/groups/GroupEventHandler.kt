package com.zeenom.loan_tracker.groups

import com.zeenom.loan_tracker.users.SyncableEventHandler
import com.zeenom.loan_tracker.users.SyncableEventRepository
import com.zeenom.loan_tracker.users.SyncableModelRepository
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Service
import java.util.*

@Service
class GroupEventHandler(
    val groupEventRepository: GroupEventRepository,
    val groupModelRepository: GroupModelRepository
) :
    SyncableEventHandler<GroupModel, GroupEventEntity> {

    suspend fun saveEvent(event: GroupEvent) {
        val entity = event.toEntity()
        groupEventRepository.save(entity)
    }

    override fun modelRepository(): SyncableModelRepository<GroupModel> {
        return groupModelRepository
    }

    override fun eventRepository(): SyncableEventRepository<GroupEventEntity> {
        return groupEventRepository
    }

    suspend fun getModelByStreamId(streamId: UUID): GroupModel? {
        return groupModelRepository.findByStreamIdAndDeletedIsFalse(streamId)
    }

    suspend fun getAllModels(userId: UUID): List<GroupModel> {
        return groupModelRepository.findAllByDeletedIsFalse().toList()
            .filter { it.memberIds?.ids?.contains(userId) == true }
    }
}