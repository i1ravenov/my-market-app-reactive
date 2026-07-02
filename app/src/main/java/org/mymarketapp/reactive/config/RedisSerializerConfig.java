package org.mymarketapp.reactive.config;

import org.mymarketapp.reactive.dto.ItemDto;
import org.mymarketapp.reactive.dto.PageDto;
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.Map;

@Configuration
public class RedisSerializerConfig {

    @Bean
    public RedisCacheManagerBuilderCustomizer cacheCustomizer() {
        Map<String, RedisCacheConfiguration> configs = Map.of(
                "item", cacheConfig(ItemDto.class, Duration.ofMinutes(1)),
                "page", cacheConfig(PageDto.class, Duration.ofMinutes(1))
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
}
