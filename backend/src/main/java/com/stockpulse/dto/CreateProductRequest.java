package com.stockpulse.dto;

import com.stockpulse.domain.Category;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public class CreateProductRequest {
    @NotBlank
    private String id;
    
    @NotBlank
    private String sku;
    
    @NotBlank
    private String name;
    
    @NotNull
    private Category category;
    
    @NotNull
    @Positive
    private BigDecimal currentPrice;
    
    @NotNull
    @Min(0)
    private Integer stockLevel;
    
    @NotNull
    @Positive
    private Integer reorderThreshold;
    
    @NotNull
    @Min(0)
    private Integer demandVelocity;

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
    
    public BigDecimal getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(BigDecimal currentPrice) { this.currentPrice = currentPrice; }
    
    public Integer getStockLevel() { return stockLevel; }
    public void setStockLevel(Integer stockLevel) { this.stockLevel = stockLevel; }
    
    public Integer getReorderThreshold() { return reorderThreshold; }
    public void setReorderThreshold(Integer reorderThreshold) { this.reorderThreshold = reorderThreshold; }
    
    public Integer getDemandVelocity() { return demandVelocity; }
    public void setDemandVelocity(Integer demandVelocity) { this.demandVelocity = demandVelocity; }
}
