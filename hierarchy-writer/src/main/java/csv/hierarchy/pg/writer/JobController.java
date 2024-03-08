package csv.hierarchy.pg.writer;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.batch.core.*;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.beans.BeanUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.LongStream;

@RestController
@RequestMapping("/jobs")
@RequiredArgsConstructor
public class JobController {
    private final JobExplorer jobExplorer;
    private final JobOperator jobOperator;

    @GetMapping()
    public List<String> list() {
        return jobExplorer.getJobNames();
    }

    @SneakyThrows
    @GetMapping("/{name}/count")
    public Long count(@PathVariable String name) {
        return jobExplorer.getJobInstanceCount(name);
    }

    @SneakyThrows
    @GetMapping("/{name}/executions")
    public List<JobInstance> count(@PathVariable String name, @RequestParam int start, @RequestParam int count) {
        return jobExplorer.findJobInstancesByJobName(name, start, count);
    }

    @SneakyThrows
    @GetMapping("/executions/{jobInstanceId}")
    public List<ExecutionLite> executions(@PathVariable Long jobInstanceId) {
        var jobInstance = jobExplorer.getJobInstance(jobInstanceId);
        if (null == jobInstance) {
            return Collections.emptyList();
        }
        var executions = jobExplorer.getJobExecutions(jobInstance);
        return executions.stream().map(this::from).toList();
    }

    @SneakyThrows
    @GetMapping("/stop/executions/{jobInstanceId}")
    public void reset(@PathVariable Long jobInstanceId) {
        jobOperator.stop(jobInstanceId);
    }

    @GetMapping("/restart/executions/{jobInstanceId}")
    public ResponseEntity<String> restart(@PathVariable Long jobInstanceId) {
        JobExecution jobExecution = jobExplorer.getJobExecution(jobInstanceId);
        ExitStatus exitStatus = Objects.requireNonNull(jobExecution).getExitStatus();
        if (exitStatus != ExitStatus.EXECUTING && exitStatus != ExitStatus.COMPLETED) {
            try {
                jobOperator.restart(jobInstanceId);
                return ResponseEntity.ok().build();
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(e.getMessage());
            }
        } else {
            return ResponseEntity.badRequest().body(exitStatus.getExitCode());
        }
    }

    @SneakyThrows
    @GetMapping("/{name}/executions/uncompleted")
    public List<ExecutionLite> uncompleted(@PathVariable String name) {
        long count = jobExplorer.getJobInstanceCount(name);
        return LongStream.range(Math.max(1L, count - 50), count).mapToObj(id -> {
                    JobInstance jobInstance = jobExplorer.getJobInstance(id);
                    List<JobExecution> executions = jobExplorer.getJobExecutions(jobInstance);
                    return executions.get(0);
                }).filter(execution -> execution.getStatus() != BatchStatus.COMPLETED)
                .map(this::from)
                .toList();
    }

    private ExecutionLite from(JobExecution execution) {
        ExecutionLite lite = new ExecutionLite();
        BeanUtils.copyProperties(execution, lite);
        return lite;
    }

    @Data
    static class ExecutionLite {
        private JobParameters jobParameters;
        private JobInstance jobInstance;
        private volatile BatchStatus status;
        private volatile LocalDateTime startTime;
        private volatile LocalDateTime createTime;
        private volatile LocalDateTime endTime;
        private volatile LocalDateTime lastUpdated;
    }
}
