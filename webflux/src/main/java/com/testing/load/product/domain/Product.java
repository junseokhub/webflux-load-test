package com.testing.load.product.domain;

import com.testing.load.common.entity.BaseEntity;
import com.testing.load.common.exception.BusinessException;
import com.testing.load.common.exception.ErrorCode;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Table;

@Getter
@NoArgsConstructor
@Table("products")
public class Product extends BaseEntity implements Persistable<Long> {

    @Id
    private Long id;
    private String name;
    private int price;
    private int stock;
    private Long createdBy;
    @Version
    private int version;

    @Builder
    public Product(String name, int price, int stock, Long createdBy) {
        this.name = name;
        this.price = price;
        this.stock = stock;
        this.createdBy = createdBy;
    }

    // 낙관적 테스트 용
    @Override
    public boolean isNew() {
        return id == null;
    }

    public void decrementStock() {
        if (this.stock <= 0) {
            throw new BusinessException(ErrorCode.PRODUCT_OUT_OF_STOCK);
        }
        this.stock--;
    }
}