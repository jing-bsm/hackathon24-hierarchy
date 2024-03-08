package csv.hierarchy.pg.async.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import csv.hierarchy.pg.async.process.AsyncProcess;
import csv.hierarchy.pg.async.process.AsyncProcessImpl;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

@Configuration
@ComponentScan(basePackageClasses = {AsyncProcess.class})
public class AsyncProcessConfig {
    @Bean("asyncProcess")
    @ConditionalOnMissingBean(name = "asyncProcess")
    @SuppressWarnings("unchecked")
    public <T> AsyncProcess<T> asyncProcess(RedisTemplate<String, Object> redisTemplate, ObjectMapper objectMapper) {
        return new AsyncProcessImpl<>(redisTemplate, objectMapper);
    }

}
