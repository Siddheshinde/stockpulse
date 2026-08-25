package com.stockpulse.controller;

import com.stockpulse.domain.Category;
import com.stockpulse.domain.ProductStatus;
import com.stockpulse.dto.CreateProductRequest;
import com.stockpulse.dto.ProductDto;
import com.stockpulse.dto.SuggestionDto;
import com.stockpulse.dto.UpdateStockRequest;
import com.stockpulse.service.ProductService;
import com.stockpulse.service.SuggestionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;
    private final SuggestionService suggestionService;

    public ProductController(ProductService productService, SuggestionService suggestionService) {
        this.productService = productService;
        this.suggestionService = suggestionService;
    }

    @PostMapping
    public ResponseEntity<ProductDto> createProduct(@Valid @RequestBody CreateProductRequest req) {
        ProductDto created = productService.createProduct(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public ResponseEntity<List<ProductDto>> getProducts(
            @RequestParam(required = false) ProductStatus status,
            @RequestParam(required = false) Category category) {
        return ResponseEntity.ok(productService.getProducts(status, category));
    }

    @PatchMapping("/{id}/stock")
    public ResponseEntity<ProductDto> updateStock(
            @PathVariable String id,
            @Valid @RequestBody UpdateStockRequest req) {
        return ResponseEntity.ok(productService.updateStock(id, req.getStockLevel()));
    }

    @PostMapping("/{id}/orders")
    public ResponseEntity<ProductDto> simulateOrder(@PathVariable String id) {
        return ResponseEntity.ok(productService.simulateOrder(id));
    }

    @PostMapping("/{id}/suggest-pricing")
    public ResponseEntity<SuggestionDto> suggestPricing(@PathVariable String id) {
        return ResponseEntity.ok(suggestionService.suggestPricing(id));
    }

    @PostMapping("/{id}/suggest-reorder")
    public ResponseEntity<SuggestionDto> suggestReorder(@PathVariable String id) {
        return ResponseEntity.ok(suggestionService.suggestReorder(id));
    }
}
