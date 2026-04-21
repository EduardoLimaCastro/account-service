package com.eduardo.account_service.infrastructure.repository.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedEventJpaRepository
        extends JpaRepository<ProcessedEventJpaEntity, ProcessedEventJpaEntity.ProcessedEventId> {

    boolean existsByIdEventIdAndIdTopic(String eventId, String topic);
}
