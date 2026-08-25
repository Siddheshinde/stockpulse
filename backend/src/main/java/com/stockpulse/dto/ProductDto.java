package com.stockpulse.dto;

import com.stockpulse.domain.Category;
import com.stockpulse.domain.Product;
import com.stockpulse.domain.ProductStatus;

import java.math.BigDecimal;

public class ProductDto {
    private String id;
    private String sku;
    private String name;
    private Category category;
    private BigDecimal currentPrice;
    private Integer stockLevel;
    private Integer reorderThreshold;
    private Integer demandVelocity;
    private ProductStatus status;

    public ProductDto(Product product) {
        this.id = product.getId();
        this.sku = product.getSku();
        this.name = product.getName();
        this.category = product.getCategory();
        this.currentPrice = product.getCurrentPrice();
        this.stockLevel = product.getStockLevel();
        this.reorderThreshold = product.getReorderThreshold();
        this.demandVelocity = product.getDemandVelocity();
        this.status = product.getStatus();
    }

    public String getId() { return id; }
    public String getSku() { return sku; }
    public String getName() { return name; }
    public Category getCategory() { return category; }
    public BigDecimal getCurrentPrice() { return currentPrice; }
    public Integer getStockLevel() { return stockLevel; }
    public Integer getReorderThreshold() { return reorderThreshold; }
    public Integer getDemandVelocity() { return demandVelocity; }
    public ProductStatus getStatus() { return status; }
}
