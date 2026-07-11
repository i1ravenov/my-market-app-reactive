package org.mymarketapp.reactive.config;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import org.mymarketapp.reactive.dto.ItemDto;
import org.mymarketapp.reactive.dto.PageDto;
import org.springframework.boot.autoconfigure.cache.CacheProperties;
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Configuration
@EnableCaching
public class RedisSerializerConfig {

    @Bean
    public RedisCacheManagerBuilderCustomizer cacheCustomizer(CacheProperties cacheProperties) {
        Duration ttl = cacheProperties.getRedis().getTimeToLive();
        TypeFactory typeFactory = TypeFactory.createDefaultInstance();
        JavaType listItemDto = typeFactory.constructCollectionType(List.class, ItemDto.class);
        Map<String, RedisCacheConfiguration> configs = Map.of(
                "item", cacheConfig(ItemDto.class, ttl),
                "page", cacheConfig(PageDto.class, ttl),
                "items", cacheConfig(listItemDto, ttl)
        );

        return builder -> builder.withInitialCacheConfigurations(configs);
    }

    private <T> RedisCacheConfiguration cacheConfig(Class<T> type, Duration ttl) {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(ttl)
                .disableCachingNullValues()
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new Jackson2JsonRedisSerializer<>(type)));
    }

    private RedisCacheConfiguration cacheConfig(JavaType type, Duration ttl) {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(ttl)
                .disableCachingNullValues()
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new Jackson2JsonRedisSerializer<>(type)));
    }


}
