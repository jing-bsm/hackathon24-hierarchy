package csv.hierarchy.pg.writer;

import csv.hierarchy.pg.persistent.domain.CsvRowNode;
import csv.hierarchy.pg.persistent.service.NodeService;
import csv.hierarchy.pg.persistent.util.CsvRowUtil;
import csv.hierarchy.pg.persistent.util.FileUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.File;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static csv.hierarchy.pg.writer.Constants.SHARED_PARAM_FILENAME;
import static csv.hierarchy.pg.writer.Constants.SHARED_PARAM_LOOKUP_MAP;

@Configuration
@EnableBatchProcessing
@RequiredArgsConstructor
@Log4j2
public class JobConfig {
    private final NodeService nodeService;
    private final TransactionTemplate transactionTemplate;

    @Bean
    public Job hierarchyIngestion(JobRepository jobRepository, Step csvDownloadStep, Step pgImportStep) {
        return new JobBuilder("hierarchyIngestion", jobRepository)
                .start(csvDownloadStep)
                .next(pgImportStep)
                .build();
    }

    @Bean
    public Step csvDownloadStep(JobRepository jobRepository, PlatformTransactionManager transactionManager, Tasklet downloadCsvTasklet) {
        return new StepBuilder("csvDownloadStep", jobRepository)
                .tasklet(downloadCsvTasklet, transactionManager)
                .build();
    }

    @Bean
    public Step pgImportStep(JobRepository jobRepository, PlatformTransactionManager transactionManager, Tasklet csv2PgTasklet) {
        return new StepBuilder("pgImportStep", jobRepository)
                .tasklet(csv2PgTasklet, transactionManager)
                .build();
    }

    @Bean
    @StepScope
    public Tasklet downloadCsvTasklet(@Value("#{jobParameters[fileName]}") String fileName) {
        return (contribution, chunkContext) -> {
            log.info("downloading {}", fileName);
            String actualFileName = fileName + ".csv";
            // TODO where to get the mapping?
            var lookupMap = Map.of("id", "id",
                    "level", "level",
                    "name", "store_name",
                    "parent_id", "parent_id");
            var list = nodeService.getAndValidate(lookupMap, FileUtil.getFile(actualFileName));
            File file = CsvRowUtil.toTemporallyFile(list);
            file.deleteOnExit();
            Util.getSharedContext(chunkContext).put(SHARED_PARAM_FILENAME, file.getAbsolutePath());
            Util.getSharedContext(chunkContext).put(SHARED_PARAM_LOOKUP_MAP, lookupMap);
            log.info("saved to temporally file {} with {} records, file size {}",
                    file.getAbsoluteFile(), list.size(), file.length());
            return RepeatStatus.FINISHED;
        };
    }

    @Bean
    @StepScope
    public Tasklet csv2PgTasklet(@Value("#{jobParameters[hierarchyId]}") Long hierarchyId,
                                 @Value("#{jobParameters[clientId]}") Long clientId) {
        return (contribution, chunkContext) -> {
            var localFileName = Util.getSharedContext(chunkContext).get(SHARED_PARAM_FILENAME, String.class);
            @SuppressWarnings("unchecked")
            Map<String, String> lookup = Util.getSharedContext(chunkContext).get(SHARED_PARAM_LOOKUP_MAP, Map.class);

            List<CsvRowNode> list = CsvRowUtil.getObjectFromFile(localFileName);
            Long definitionId = transactionTemplate.execute(status ->
                    nodeService.save(clientId, hierarchyId, Instant.now(), lookup, list));
            log.info("saved! client {}, hierarchy {}, definition {}, with {} records", clientId, hierarchyId, definitionId, list.size());
            return RepeatStatus.FINISHED;
        };
    }
}
