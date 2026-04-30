package com.testing.load.order.domain;

import com.testing.load.common.entity.BaseEntity;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Getter
@NoArgsConstructor
@Table("orders")
public class Order extends BaseEntity {

    @Id
    private Long id;
    private Long userId;
    private Long productId;
    private Long couponIssueId;  // nullable
    private int originalPrice;
    private int finalPrice;
    private OrderStatus status;  // PENDING, CONFIRMED, CANCELLED

    @Builder
    public Order(Long userId, Long productId, Long couponIssueId,
                 int originalPrice, int finalPrice) {
        this.userId = userId;
        this.productId = productId;
        this.couponIssueId = couponIssueId;
        this.originalPrice = originalPrice;
        this.finalPrice = finalPrice;
        this.status = OrderStatus.PENDING;
    }

    public static Order pending(Long userId, Long productId, Long couponIssueId) {
        return Order.builder()
                .userId(userId)
                .productId(productId)
                .couponIssueId(couponIssueId)
                .originalPrice(0)
                .finalPrice(0)
                .build();
    }
}