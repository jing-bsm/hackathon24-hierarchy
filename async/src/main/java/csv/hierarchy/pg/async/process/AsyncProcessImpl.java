package csv.hierarchy.pg.async.process;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import csv.hierarchy.pg.async.domain.AsyncRequestStatus;
import lombok.*;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * A simple service do async process and store the result in redis
 * Used for generating long-running reports.
 */
@Log4j2
@RequiredArgsConstructor
public class AsyncProcessImpl<T> implements AsyncProcess<T> {
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${config.cache.async.prefix}")
    private String cachePrefix;

    @Value("${config.cache.async.ttlMinutes:10}")
    private long ttlMinutes;

    @Override
    public String process(T request, Function<T, ?> function) {
        String eventId = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        AsyncRequestRecord asyncRequestRecord = AsyncRequestRecord.builder()
                .startEpoll(System.currentTimeMillis())
                .status(AsyncRequestStatus.PROGRESSING)
                .build();
        log.info("Async request {}, id {}, {}", request, eventId, asyncRequestRecord);
        save(eventId, asyncRequestRecord);
        CompletableFuture.runAsync(() -> {
            try {
                asyncRequestRecord.setResult(objectMapper.writeValueAsString(function.apply(request)));
                asyncRequestRecord.setStatus(AsyncRequestStatus.COMPLETED);
            } catch (Exception e) {
                log.error("Failed to process {}, {}, due to", eventId, asyncRequestRecord, e);
                asyncRequestRecord.setStatus(AsyncRequestStatus.FAILED);
            } finally {
                asyncRequestRecord.setEndEpoll(System.currentTimeMillis());
            }
            log.info("Async event {} {} in {} ms, request {}", eventId, asyncRequestRecord.status,
                    asyncRequestRecord.endEpoll - asyncRequestRecord.startEpoll, request);
            save(eventId, asyncRequestRecord);
        });
        return eventId;
    }

    public AsyncRequestStatus getStatus(String eventId) {
        return getRecord(eventId).getStatus();
    }

    @Override
    public JsonNode getJsonResult(String eventId) {
        return getResult(eventId, JsonNode.class);
    }

    @SneakyThrows
    public <R> R getResult(String eventId, Class<R> clazz) {
        String result = getRecord(eventId).getResult();
        return result == null ? null : objectMapper.readValue(result, clazz);
    }

    private AsyncRequestRecord getRecord(String eventId) {
        Object request = redisTemplate.opsForValue().get(getRedisKey(eventId));
        if (null == request) {
            throw new ResourceNotFoundException(String.format("Event %s not found", eventId));
        }
        return (AsyncRequestRecord) request;
    }

    private void save(String eventId, AsyncRequestRecord asyncRequestRecord) {
        redisTemplate.opsForValue().set(getRedisKey(eventId), asyncRequestRecord, ttlMinutes, TimeUnit.MINUTES);
    }

    private String getRedisKey(String eventId) {
        return cachePrefix + "::" + eventId;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class AsyncRequestRecord {
        private long startEpoll;
        private long endEpoll;
        private AsyncRequestStatus status;
        private String result;
    }
}
