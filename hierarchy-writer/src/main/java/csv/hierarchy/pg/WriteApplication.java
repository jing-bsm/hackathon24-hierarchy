package csv.hierarchy.pg;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

@EnableRetry
@SpringBootApplication
public class WriteApplication {
    public static void main(String[] args) {
        SpringApplication.run(WriteApplication.class, args);
    }
}
