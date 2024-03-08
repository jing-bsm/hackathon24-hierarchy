package csv.hierarchy.pg.async.process;

import com.fasterxml.jackson.databind.JsonNode;
import csv.hierarchy.pg.async.domain.AsyncRequestStatus;

import java.util.function.Function;

public interface AsyncProcess<T> {
    String process(T request, Function<T, ?> function);

    AsyncRequestStatus getStatus(String eventId);

    JsonNode getJsonResult(String eventId);

    <R> R getResult(String eventId, Class<R> clazz);
}
