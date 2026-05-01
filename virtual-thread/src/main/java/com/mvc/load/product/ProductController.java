package com.mvc.load.product;

import com.mvc.load.product.dto.ProductRequest;
import com.mvc.load.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/product")
@RequiredArgsConstructor
public class ProductController {

    private final ProjectService projectService;

    @PostMapping
    public ResponseEntity<Product> createProduct(
            @RequestBody ProductRequest request,
            @AuthenticationPrincipal User user
            ) {
        return ResponseEntity.status(201)
                .body(projectService.createProduct(
                        user.getId(), request.name(), request.price(), request.stock()
                ));
    }

    @GetMapping
    public ResponseEntity<List<Product>> getAllProducts() {
        return ResponseEntity.ok(projectService.findAllProducts());
    }
}
