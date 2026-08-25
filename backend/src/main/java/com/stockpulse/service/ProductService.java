package com.stockpulse.service;

import com.stockpulse.domain.*;
import com.stockpulse.dto.CreateProductRequest;
import com.stockpulse.dto.ProductDto;
import com.stockpulse.exception.ResourceNotFoundException;
import com.stockpulse.repository.InventorySnapshotRepository;
import com.stockpulse.repository.ProductRepository;
import com.stockpulse.event.ProductStateChangedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final InventorySnapshotRepository snapshotRepository;
    private final ApplicationEventPublisher eventPublisher;

    public ProductService(ProductRepository productRepository, 
                          InventorySnapshotRepository snapshotRepository,
                          ApplicationEventPublisher eventPublisher) {
        this.productRepository = productRepository;
        this.snapshotRepository = snapshotRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public ProductDto createProduct(CreateProductRequest req) {
        if (productRepository.existsById(req.getId())) {
            throw new IllegalStateException("Product with ID " + req.getId() + " already exists");
        }
        
        Product product = new Product();
        product.setId(req.getId());
        product.setSku(req.getSku());
        product.setName(req.getName());
        product.setCategory(req.getCategory());
        product.setCurrentPrice(req.getCurrentPrice());
        product.setStockLevel(req.getStockLevel());
        product.setReorderThreshold(req.getReorderThreshold());
        product.setDemandVelocity(req.getDemandVelocity());
        product.setStatus(ProductStatus.ACTIVE);
        
        product = productRepository.save(product);
        createSnapshot(product, TriggerReason.INITIAL);
        
        return new ProductDto(product);
    }

    @Transactional(readOnly = true)
    public List<ProductDto> getProducts(ProductStatus status, Category category) {
        // Since it's a hackathon and no specifications for complex querying were needed,
        // filtering in memory is sufficient for small demo datasets, 
        // or we could use custom queries. We'll use stream filtering for simplicity.
        return productRepository.findAll().stream()
                .filter(p -> status == null || p.getStatus() == status)
                .filter(p -> category == null || p.getCategory() == category)
                .map(ProductDto::new)
                .collect(Collectors.toList());
    }

    @Transactional
    public ProductDto updateStock(String id, Integer newStock) {
        Product product = getProductEntity(id);
        product.setStockLevel(newStock);
        product = productRepository.save(product);
        
        createSnapshot(product, TriggerReason.MANUAL);
        checkAndPublishTriggers(product);
        
        return new ProductDto(product);
    }

    @Transactional
    public ProductDto simulateOrder(String id) {
        Product product = getProductEntity(id);
        
        if (product.getStockLevel() <= 0) {
            throw new IllegalStateException("Product is out of stock");
        }
        
        // Simple deterministic update: decrease stock by 1, increase demand velocity by 1
        product.setStockLevel(product.getStockLevel() - 1);
        product.setDemandVelocity(product.getDemandVelocity() + 1);
        
        if (product.getStockLevel() == 0) {
            product.setStatus(ProductStatus.OUT_OF_STOCK);
        }
        
        product = productRepository.save(product);
        createSnapshot(product, TriggerReason.MANUAL);
        
        checkAndPublishTriggers(product);
        
        return new ProductDto(product);
    }

    private void checkAndPublishTriggers(Product product) {
        if (product.getStockLevel() < product.getReorderThreshold()) {
            eventPublisher.publishEvent(new ProductStateChangedEvent(this, product.getId(), TriggerReason.INVENTORY_LOW));
        }

        Double avgVel = productRepository.getAverageVelocityByCategory(product.getCategory());
        if (avgVel != null && product.getDemandVelocity() > 2 * avgVel) {
            eventPublisher.publishEvent(new ProductStateChangedEvent(this, product.getId(), TriggerReason.DEMAND_SPIKE));
        }
    }

    private Product getProductEntity(String id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
    }

    private void createSnapshot(Product product, TriggerReason reason) {
        InventorySnapshot snapshot = new InventorySnapshot();
        snapshot.setProduct(product);
        snapshot.setTimestamp(Instant.now());
        snapshot.setStockLevel(product.getStockLevel());
        snapshot.setDemandVelocity(product.getDemandVelocity());
        snapshot.setTriggerReason(reason);
        snapshotRepository.save(snapshot);
    }
}
