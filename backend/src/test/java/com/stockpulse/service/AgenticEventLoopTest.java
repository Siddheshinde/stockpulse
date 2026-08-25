package com.stockpulse.service;

import com.stockpulse.domain.*;
import com.stockpulse.dto.UpdateStockRequest;
import com.stockpulse.repository.PricingSuggestionRepository;
import com.stockpulse.repository.ProductRepository;
import com.stockpulse.repository.ReorderSuggestionRepository;
import com.stockpulse.repository.InventorySnapshotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
public class AgenticEventLoopTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private PricingSuggestionRepository pricingRepo;

    @Autowired
    private ReorderSuggestionRepository reorderRepo;

    @Autowired
    private InventorySnapshotRepository snapshotRepository;

    @BeforeEach
    void setUp() {
        pricingRepo.deleteAll();
        reorderRepo.deleteAll();
        snapshotRepository.deleteAll();
        productRepository.deleteAll();
    }

    private Product createProduct(String id, int stock, int threshold, int demandVelocity) {
        Product p = new Product();
        p.setId(id);
        p.setSku("SKU-" + id);
        p.setName("Test Product " + id);
        p.setCategory(Category.APPAREL);
        p.setCurrentPrice(new BigDecimal("100.00"));
        p.setStockLevel(stock);
        p.setReorderThreshold(threshold);
        p.setDemandVelocity(demandVelocity);
        p.setStatus(ProductStatus.ACTIVE);
        return productRepository.save(p);
    }

    @Test
    void testLowStockEventTriggersSuggestions() {
        // Given stock = 20, threshold = 15
        Product p = createProduct("PRD-LOW", 20, 15, 5);

        // When stock updated to 10 (< 15)
        productService.updateStock(p.getId(), 10);

        // Then INVENTORY_LOW event creates suggestions asynchronously
        waitFor(() -> pricingRepo.findAll().size() >= 1);

        List<PricingSuggestion> pricing = pricingRepo.findAll();
        List<ReorderSuggestion> reorder = reorderRepo.findAll();

        assertThat(pricing).hasSize(1);
        assertThat(reorder).hasSize(1);

        assertEquals(TriggerReason.INVENTORY_LOW, pricing.get(0).getTriggerReason());
        assertEquals(SuggestionStatus.PENDING, pricing.get(0).getStatus());

        assertEquals(TriggerReason.INVENTORY_LOW, reorder.get(0).getTriggerReason());
        assertEquals(SuggestionStatus.PENDING, reorder.get(0).getStatus());
    }

    @Test
    void testDemandSpikeEventTriggersSuggestions() {
        // Add background products to keep the category average low
        createProduct("PRD-BG1", 50, 10, 2);
        createProduct("PRD-BG2", 50, 10, 2);
        createProduct("PRD-BG3", 50, 10, 2);
        
        // Given a product with velocity = 10
        Product p = createProduct("PRD-SPIKE", 50, 10, 10);

        // When order simulated until velocity > 20
        // simulateOrder increments velocity by 1. Needs 11 orders.
        for (int i = 0; i < 11; i++) {
            productService.simulateOrder(p.getId());
        }

        // Then DEMAND_SPIKE event creates suggestions asynchronously
        waitFor(() -> pricingRepo.findAll().stream().anyMatch(s -> s.getTriggerReason() == TriggerReason.DEMAND_SPIKE));

        List<PricingSuggestion> pricing = pricingRepo.findAll();
        
        // Only care about finding the demand spike ones
        boolean hasDemandSpikePricing = pricing.stream()
            .anyMatch(s -> s.getTriggerReason() == TriggerReason.DEMAND_SPIKE);
            
        assertTrue(hasDemandSpikePricing, "Should have DEMAND_SPIKE pricing suggestion");
    }

    @Test
    void testIdempotencyPreventsDuplicates() {
        Product p = createProduct("PRD-IDEMPOTENT", 20, 15, 5);

        // Trigger low stock event first time
        productService.updateStock(p.getId(), 10);

        waitFor(() -> pricingRepo.findAll().size() == 1);

        // Trigger again (stock is still 10, so it publishes event again)
        productService.updateStock(p.getId(), 9);

        // Wait to ensure no duplicates are created
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {}

        // Should still only have 1 pending suggestion of each type for INVENTORY_LOW
        List<PricingSuggestion> pricing = pricingRepo.findAll();
        List<ReorderSuggestion> reorder = reorderRepo.findAll();

        assertThat(pricing).hasSize(1);
        assertThat(reorder).hasSize(1);
    }

    @Test
    void testConcurrentIdempotencyPreventsDuplicates() throws InterruptedException {
        Product p = createProduct("PRD-CONCURRENT", 20, 15, 5);

        // Fire 10 updates rapidly to simulate concurrent requests that would trigger INVENTORY_LOW
        for (int i = 0; i < 10; i++) {
            productService.updateStock(p.getId(), 10);
        }

        // Wait to ensure all background processing finishes
        Thread.sleep(3000);

        // Even with 10 concurrent requests, there should only be 1 pending suggestion of each type
        List<PricingSuggestion> pricing = pricingRepo.findAll();
        List<ReorderSuggestion> reorder = reorderRepo.findAll();

        assertThat(pricing).hasSize(1);
        assertThat(reorder).hasSize(1);
    }

    @Test
    void testNoFalseTrigger() {
        Product p = createProduct("PRD-NORMAL", 50, 15, 5);

        // Ordinary update, stock remains > threshold
        productService.updateStock(p.getId(), 40);

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {}

        assertThat(pricingRepo.findAll()).isEmpty();
        assertThat(reorderRepo.findAll()).isEmpty();
    }

    private void waitFor(java.util.function.Supplier<Boolean> condition) {
        for (int i = 0; i < 50; i++) {
            if (condition.get()) {
                return;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {}
        }
    }
}
