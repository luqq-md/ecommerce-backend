package com.luqman.ecommerce.config;

import com.luqman.ecommerce.product.entity.dto.ProductPageResponse;
import com.luqman.ecommerce.product.entity.dto.ProductResponse;
import java.time.Duration;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    public static final String PRODUCT_CACHE = "products";
    public static final String PRODUCT_PAGE_CACHE = "product-pages";

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration baseConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .disableCachingNullValues()
                .serializeKeysWith(SerializationPair.fromSerializer(new StringRedisSerializer()));

        RedisCacheConfiguration productConfig = baseConfig.serializeValuesWith(
                SerializationPair.fromSerializer(new JacksonJsonRedisSerializer<>(ProductResponse.class))
        );

        RedisCacheConfiguration productPageConfig = baseConfig.serializeValuesWith(
                SerializationPair.fromSerializer(new JacksonJsonRedisSerializer<>(ProductPageResponse.class))
        );

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(baseConfig)
                .withInitialCacheConfigurations(Map.of(
                        PRODUCT_CACHE, productConfig,
                        PRODUCT_PAGE_CACHE, productPageConfig
                ))
                .build();
    }
}