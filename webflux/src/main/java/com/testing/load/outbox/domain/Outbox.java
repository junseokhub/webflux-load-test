package com.testing.load.outbox.domain;

import com.testing.load.common.entity.BaseEntity;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Table;

@Getter
@NoArgsConstructor
@Table("outbox")
public class Outbox extends BaseEntity implements Persistable<Long> {

    @Id
    private Long id;
    private Long aggregateId;
    private String eventType;
    private String payload;
    private String status;
    private int retryCount;
    private String failedReason;

    @Builder
    public Outbox(Long aggregateId, String eventType, String payload) {
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.payload = payload;
        this.status = "PENDING";
        this.retryCount = 0;
    }

    @Override
    public boolean isNew() {
        return id == null;
    }

    public void markProcessed() {
        this.status = "PROCESSED";
    }

    public void markFailed(String reason) {
        this.status = "FAILED";
        this.failedReason = reason;
        this.retryCount++;
    }

    public void markProcessing() {
        this.status = "PROCESSING";
    }

    public void incrementRetry() {
        this.retryCount++;
    }
}