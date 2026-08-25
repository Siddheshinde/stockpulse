package com.stockpulse.service;

import com.stockpulse.domain.*;
import com.stockpulse.dto.CreateProductRequest;
import com.stockpulse.dto.ProductDto;
import com.stockpulse.exception.ResourceNotFoundException;
import com.stockpulse.repository.InventorySnapshotRepository;
import com.stockpulse.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final InventorySnapshotRepository snapshotRepository;

    public ProductService(ProductRepository productRepository, InventorySnapshotRepository snapshotRepository) {
        this.productRepository = productRepository;
        this.snapshotRepository = snapshotRepository;
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
        
        // Agentic loop logic is deferred to Phase 6
        return new ProductDto(product);
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
