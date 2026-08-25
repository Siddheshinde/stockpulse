package com.stockpulse.repository;

import com.stockpulse.domain.Category;
import com.stockpulse.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<Product, String> {
    
    @Query("SELECT COALESCE(AVG(p.demandVelocity), 0.0) FROM Product p WHERE p.category = :category")
    Double getAverageVelocityByCategory(@Param("category") Category category);
}
