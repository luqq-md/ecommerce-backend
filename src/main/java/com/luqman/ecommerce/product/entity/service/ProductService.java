package com.luqman.ecommerce.product.entity.service;

import com.luqman.ecommerce.common.exception.ConflictException;
import com.luqman.ecommerce.common.exception.ResourceNotFoundException;
import com.luqman.ecommerce.config.RedisConfig;
import com.luqman.ecommerce.product.entity.dto.*;
import com.luqman.ecommerce.product.entity.Product;
import com.luqman.ecommerce.product.entity.repository.ProductRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {

    private static final int MAX_PAGE_SIZE = 50;

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    @Cacheable(
            cacheNames = RedisConfig.PRODUCT_PAGE_CACHE,
            key = "(#category == null || #category.isBlank() ? 'all' : #category.trim().toLowerCase()) + ':' + #page + ':' + #size"
    )
    public ProductPageResponse listProducts(String category, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Product> products = (category == null || category.isBlank())
                ? productRepository.findByActiveTrue(pageable)
                : productRepository.findByActiveTrueAndCategoryIgnoreCase(category.trim(), pageable);

        return ProductPageResponse.from(products.map(ProductResponse::from));
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = RedisConfig.PRODUCT_CACHE, key = "#id")
    public ProductResponse getById(Long id) {
        Product product = productRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        return ProductResponse.from(product);
    }

    @Transactional
    @CacheEvict(cacheNames = RedisConfig.PRODUCT_PAGE_CACHE, allEntries = true)
    public ProductResponse create(CreateProductRequest request) {
        String normalizedSku = request.sku().trim().toUpperCase();
        if (productRepository.existsBySku(normalizedSku)) {
            throw new ConflictException("Product with SKU already exists: " + normalizedSku);
        }

        Product product = new Product(
                normalizedSku,
                request.name().trim(),
                request.description(),
                request.category().trim(),
                request.price(),
                request.stock()
        );
        return ProductResponse.from(productRepository.save(product));
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = RedisConfig.PRODUCT_CACHE, key = "#id"),
            @CacheEvict(cacheNames = RedisConfig.PRODUCT_PAGE_CACHE, allEntries = true)
    })
    public ProductResponse update(Long id, UpdateProductRequest request) {
        Product product = productRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        product.setName(request.name().trim());
        product.setDescription(request.description());
        product.setCategory(request.category().trim());
        product.setPrice(request.price());
        product.setStock(request.stock());

        return ProductResponse.from(productRepository.save(product));
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = RedisConfig.PRODUCT_CACHE, key = "#id"),
            @CacheEvict(cacheNames = RedisConfig.PRODUCT_PAGE_CACHE, allEntries = true)
    })
    public void delete(Long id) {
        Product product = productRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        product.setActive(false);
        productRepository.save(product);
    }
}