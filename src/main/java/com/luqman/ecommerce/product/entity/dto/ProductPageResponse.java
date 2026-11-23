// ProductPageResponse.java
package com.luqman.ecommerce.product.entity.dto;

import java.util.List;

import com.luqman.ecommerce.product.entity.dto.ProductResponse;
import org.springframework.data.domain.Page;

public record ProductPageResponse(
        List<ProductResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean last
) {
    public static ProductPageResponse from(Page<ProductResponse> pageData) {
        return new ProductPageResponse(
                pageData.getContent(),
                pageData.getNumber(),
                pageData.getSize(),
                pageData.getTotalElements(),
                pageData.getTotalPages(),
                pageData.isLast()
        );
    }
}