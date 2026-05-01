package com.mvc.load.order.service;

import com.mvc.load.common.exception.BusinessException;
import com.mvc.load.common.exception.ErrorCode;
import com.mvc.load.order.Order;
import com.mvc.load.order.OrderRepository;
import com.mvc.load.outbox.Outbox;
import com.mvc.load.outbox.OutboxRepository;
import com.mvc.load.product.Product;
import com.mvc.load.product.ProductService;
import com.mvc.load.user.User;
import com.mvc.load.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RedisLuaOrderService implements OrderService{

    private final OrderRepository orderRepository;
    private final ProductService productService;
    private final UserService userService;
    private final RedisTemplate<String, String> redisTemplate;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    private static final String DECREMENT_STOCK_SCRIPT = """
            local stock = tonumber(redis.call('GET', KEYS[1]))
            if stock == nil or stock <= 0 then
                return -1
            end
            redis.call('DECR', KEYS[1])
            return 1
            """;

    @Override
    @Transactional
    public Order createOrder(Long userId, Long productId) {
        String stockKey = "{product:" + productId + "}:stock";

        Long result = redisTemplate.execute(
                new DefaultRedisScript<>(DECREMENT_STOCK_SCRIPT, Long.class),
                List.of(stockKey)
        );

        if (result == null || result == -1L) {
            throw new BusinessException(ErrorCode.PRODUCT_OUT_OF_STOCK);
        }

        User user = userService.findById(userId);
        Product product = productService.findById(productId);

        Order order = orderRepository.save(Order.builder()
                .user(user)
                .product(product)
                .originalPrice(product.getPrice())
                .finalPrice(product.getPrice())
                .build());

        // Outbox 이벤트 저장 (주문과 같은 트랜잭션)
        try {
            String payload = objectMapper.writeValueAsString(
                    new OutboxPayload(productId, order.getId())
            );
            outboxRepository.save(Outbox.builder()
                    .aggregateId(productId)
                    .eventType("STOCK_DECREMENT")
                    .payload(payload)
                    .build());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return order;
    }

    public record OutboxPayload(Long productId, Long orderId) {}
}