package csv.hierarchy.pg.writer;

import csv.hierarchy.pg.persistent.domain.UserScope;
import csv.hierarchy.pg.persistent.entity.User;
import csv.hierarchy.pg.persistent.service.UserService;
import csv.hierarchy.pg.writer.dto.UserAuth;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class Controller {
    private final JobLauncher jobLauncher;

    private final Job job;

    private final UserService userService;

    @SneakyThrows
    @PutMapping("/trigger/{clientId}/{hierarchyId}/{fileName}")
    public ResponseEntity<String> trigger(@PathVariable Long clientId, @PathVariable Long hierarchyId, @PathVariable String fileName) {
        // You can pass any parameters you need to the job
        JobParameters jobParameters = new JobParametersBuilder()
                .addLong("hierarchyId", hierarchyId)
                .addLong("clientId", clientId)
                .addString("fileName", fileName)
                .toJobParameters();
        try {
            JobExecution jobExecution = jobLauncher.run(job, jobParameters);
            return ResponseEntity.ok().body("Job launched with id: " + jobExecution.getId());
        } catch (JobExecutionException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/user/{userId}/{clientId}/{hierarchyId}")
    public ResponseEntity<User> trigger(@PathVariable Long userId, @PathVariable Long clientId, @PathVariable Long hierarchyId,
                                        @RequestBody UserAuth auth) {
        var user = userService.save(userId,clientId, UserScope.fromShort(auth.getScope()), auth.getCascade(), auth.getExplicit());
        return ResponseEntity.ok().body(user);
    }
}
