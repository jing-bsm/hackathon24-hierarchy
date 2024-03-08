package csv.hierarchy.pg.writer;

import lombok.experimental.UtilityClass;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.item.ExecutionContext;

@UtilityClass
public class Util {

    public ExecutionContext getSharedContext(ChunkContext chunkContext) {
        return chunkContext.getStepContext().getStepExecution().getJobExecution().getExecutionContext();
    }
}
