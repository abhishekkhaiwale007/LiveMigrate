package com.livemigrate.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import java.time.Duration;

@Configuration
public class RedisConfiguration {

    /**
     * Creates a custom JSON serializer that properly handles Java 8 date/time types
     * and ensures proper type information is included in serialized data.
     */
    @Bean
    public GenericJackson2JsonRedisSerializer genericJackson2JsonRedisSerializer() {
        // Create a type validator that allows our model classes to be serialized safely
        PolymorphicTypeValidator typeValidator = BasicPolymorphicTypeValidator.builder()
                .allowIfBaseType(Object.class)
                .build();

        // Create and configure the ObjectMapper with necessary modules and settings
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());  // Add support for Java 8 date/time types
        mapper.activateDefaultTyping(typeValidator, ObjectMapper.DefaultTyping.NON_FINAL);

        // Create the serializer with our configured ObjectMapper
        return new GenericJackson2JsonRedisSerializer(mapper);
    }

    /**
     * Creates and configures the Redis connection factory with appropriate timeouts
     * and connection pooling settings.
     */
    @Bean
    public RedisConnectionFactory redisConnectionFactory(RedisProperties properties) {
        // Create the standalone configuration
        RedisStandaloneConfiguration standaloneConfig = new RedisStandaloneConfiguration();
        standaloneConfig.setHostName(properties.getHost() != null ? properties.getHost() : "localhost");
        standaloneConfig.setPort(properties.getPort() != 0 ? properties.getPort() : 6379);
        standaloneConfig.setDatabase(properties.getDatabase());

        // Set authentication if provided
        if (properties.getUsername() != null) {
            standaloneConfig.setUsername(properties.getUsername());
        }
        if (properties.getPassword() != null && !properties.getPassword().isEmpty()) {
            standaloneConfig.setPassword(properties.getPassword());
        }

        // Configure connection pooling settings
        LettucePoolingClientConfiguration.LettucePoolingClientConfigurationBuilder builder =
                LettucePoolingClientConfiguration.builder();

        // Set command timeout - using a default of 2 seconds if not specified
        Duration timeout = properties.getTimeout() != null ? properties.getTimeout() : Duration.ofSeconds(2);
        builder.commandTimeout(timeout);

        // Configure connection pooling if enabled
        if (properties.getLettuce() != null && properties.getLettuce().getPool() != null) {
            GenericObjectPoolConfig<?> poolConfig = new GenericObjectPoolConfig<>();
            RedisProperties.Pool poolProps = properties.getLettuce().getPool();

            poolConfig.setMaxTotal(poolProps.getMaxActive());
            poolConfig.setMaxIdle(poolProps.getMaxIdle());
            poolConfig.setMinIdle(poolProps.getMinIdle());
            if (poolProps.getMaxWait() != null) {
                poolConfig.setMaxWait(poolProps.getMaxWait());
            } else {
                poolConfig.setMaxWait(Duration.ofMillis(1000));
            }

            builder.poolConfig(poolConfig);
        }

        return new LettuceConnectionFactory(standaloneConfig, builder.build());
    }

    /**
     * Creates and configures the RedisTemplate with appropriate serializers for
     * different types of data.
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(
            RedisConnectionFactory connectionFactory,
            GenericJackson2JsonRedisSerializer jsonSerializer) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Configure serializers
        StringRedisSerializer stringSerializer = new StringRedisSerializer();

        // Set serializers for different types of data
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setValueSerializer(jsonSerializer);  // Use our custom JSON serializer
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();

        return template;
    }
}