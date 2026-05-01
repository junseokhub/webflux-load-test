package com.mvc.load.product;

import com.mvc.load.common.exception.BusinessException;
import com.mvc.load.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProductRepository productRepository;

    @Transactional
    public Product createProduct(Long userId, String name, Integer price, Integer stock) {
        Product product = Product.builder()
                .name(name)
                .price(price)
                .stock(stock)
                .build();

        return productRepository.save(product);
    }

    @Transactional(readOnly = true)
    public List<Product> findAllProducts() {
        return productRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Product findById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
    }
}
