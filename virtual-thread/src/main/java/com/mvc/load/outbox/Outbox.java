package com.mvc.load.outbox;

import com.mvc.load.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
@Table(name = "outbox")
public class Outbox extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "aggregate_id")
    private String aggregateId;

    @Column(name = "event_type")
    private String eventType;

    @Column(name = "payload")
    private String payload;

    @Column(name = "status")
    private String status;

    @Column(name = "retry_count")
    private int retryCount;

    @Column(name = "failed_reason")
    private String failedReason;

    @Builder
    public Outbox(Long aggregateId, String eventType, String payload) {
        this.aggregateId = aggregateId.toString();
        this.eventType = eventType;
        this.payload = payload;
        this.status = "PENDING";
        this.retryCount = 0;
    }

    public void markProcessed() {
        this.status = "PROCESSED";
    }

    public void markProcessing() {
        this.status = "PROCESSING";
    }

    public void markFailed(String reason) {
        this.status = "FAILED";
        this.failedReason = reason;
        this.retryCount++;
    }
}
