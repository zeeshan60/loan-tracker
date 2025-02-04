package com.zeenom.loan_tracker.events

import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface EventRepository : ReactiveCrudRepository<EventEntity, String>