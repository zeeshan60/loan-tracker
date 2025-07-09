package com.zeenom.loan_tracker.users

import com.zeenom.loan_tracker.transactions.IEventAble
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.UUID

interface SyncableModelRepository<T : SyncableModel> {
    suspend fun findFirstSortByIdDescending(): T?
    suspend fun findByStreamId(streamId: UUID): T?
    fun saveAll(models: List<T>): Flow<T>
}

interface SyncableEventRepository<T : IEventAble<*>> {
    suspend fun findAllSinceStreamIdAndVersion(streamId: UUID, version: Int): List<T>
    fun findAll(): Flow<T>
}

interface SyncableModel {
    val streamId: UUID
    val version: Int
    val deleted: Boolean
}

interface SyncableEventHandler<M : SyncableModel, E : IEventAble<M>> {
    val logger: Logger
        get() = LoggerFactory.getLogger(this::class.java)

    fun modelRepository(): SyncableModelRepository<M>
    fun eventRepository(): SyncableEventRepository<E>

    /**
     * Synchronize the user model with the user events.
     * This method is called to ensure that the user model is up-to-date with the latest user events.
     * It can be used to handle any discrepancies between the user model and the user events.
     *
     * NOTE: This method assumes number of events are small since it loads all un-synchronized events into memory.
     *      like less than 1000. Otherwise, please use migration script to form models rather than this method.
     */
    suspend fun synchronize() {
        val modelRepository = modelRepository()
        val eventRepository = eventRepository()
        val latestModel = modelRepository.findFirstSortByIdDescending()
        if (latestModel == null) {
            logger.warn("No user model found, skipping synchronization")
        }
        val eventsAfter = (latestModel?.let {
            eventRepository.findAllSinceStreamIdAndVersion(
                it.streamId,
                it.version
            )
        } ?: eventRepository.findAll().toList())
            .map { it.toEvent() }
        if (eventsAfter.isEmpty()) {
            logger.debug(
                "No new events found after user model with stream id {} and version {}",
                latestModel?.streamId,
                latestModel?.version
            )
            return
        }
        val models = eventsAfter.groupBy { it.streamId }.mapNotNull { entry ->
            val sorted = entry.value.sortedBy { it.version }
            @Suppress("CAST_NEVER_SUCCEEDS")
            sorted.fold(null as M?) { model, event ->
                if (event.version != 0 && model == null) {
                    //This means event already has an existing model in the past so we need to pick up from last known model for this event
                    //Alternatively we can get the entire event stream and apply it to the model but using the model from the repository is more efficient
                    val existing = modelRepository.findByStreamId(event.streamId)
                    event.applyEvent(existing)
                } else {
                    event.applyEvent(model)
                }
            } ?: run {
                logger.warn("No user model found for stream id ${entry.key}, this should not have happened")
                null
            }
        }
        modelRepository.saveAll(models).toList()
    }
}