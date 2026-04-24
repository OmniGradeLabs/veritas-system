package com.veritas.omnigrade.infrastructure.cached.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

// No need complicated config since:
// Spring IoC auto get port, host, timeout in config file --> Auto create connectionFactory
// Later when deploy, we just need to change setting in config file, Spring auto dothe other
// --> We just create the template and inject that custom config to connect to our redis host
// Gate: StringRedisConfig<Key, Value> is the service that we read, write and work with redis

@Configuration
public class RedisConfig {
  @Bean
  public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
    return new StringRedisTemplate(connectionFactory);
  }
}
