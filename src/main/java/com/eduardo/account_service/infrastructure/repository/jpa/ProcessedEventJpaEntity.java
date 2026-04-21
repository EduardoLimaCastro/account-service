package com.eduardo.account_service.infrastructure.repository.jpa;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "processed_events")
public class ProcessedEventJpaEntity {

    @EmbeddedId
    private ProcessedEventId id;

    @Column(name = "processed_at", nullable = false)
    private LocalDateTime processedAt;

    protected ProcessedEventJpaEntity() {}

    public ProcessedEventJpaEntity(String eventId, String topic) {
        this.id = new ProcessedEventId(eventId, topic);
        this.processedAt = LocalDateTime.now();
    }

    @Embeddable
    public static class ProcessedEventId implements java.io.Serializable {

        @Column(name = "event_id")
        private String eventId;

        @Column(name = "topic")
        private String topic;

        protected ProcessedEventId() {}

        public ProcessedEventId(String eventId, String topic) {
            this.eventId = eventId;
            this.topic = topic;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ProcessedEventId other)) return false;
            return java.util.Objects.equals(eventId, other.eventId)
                    && java.util.Objects.equals(topic, other.topic);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(eventId, topic);
        }
    }
}
