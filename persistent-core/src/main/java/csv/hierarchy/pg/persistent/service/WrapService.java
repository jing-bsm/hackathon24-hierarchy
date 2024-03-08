package csv.hierarchy.pg.persistent.service;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.function.Consumer;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
public class WrapService {

    private final ApplicationContext applicationContext;

    @Retryable
    public <T> T retrySupplier(Supplier<T> supplier) {
        return supplier.get();
    }

    public <R> void consume(Class<R> clazz, Consumer<R> consumer) {
        final R bean = applicationContext.getBean(clazz);
        consumer.accept(bean);
    }

    @Async
    public void asyncRun(Runnable runnable) {
        runnable.run();
    }

    @Transactional
    public <T> T transactionSupplier(Supplier<T> supplier) {
        return supplier.get();
    }
}
