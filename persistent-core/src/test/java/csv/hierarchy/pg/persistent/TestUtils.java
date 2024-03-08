package csv.hierarchy.pg.persistent;

import lombok.experimental.UtilityClass;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.function.Supplier;

@UtilityClass
public class TestUtils {

    public static final PostgreSQLContainer postgreSQLContainer = ((Supplier<PostgreSQLContainer>) () -> {
        PostgreSQLContainer container = new PostgreSQLContainer("postgres:16.1")
                .withDatabaseName("postgres")
                .withUsername("postgres")
                .withPassword("pg");
        container.start();
        return container;
    }).get();

    public void initialDocker() {
        System.setProperty("spring.datasource.url", postgreSQLContainer.getJdbcUrl());
        System.setProperty("spring.datasource.username", postgreSQLContainer.getUsername());
        System.setProperty("spring.datasource.password", postgreSQLContainer.getPassword());
        System.setProperty("spring.flyway.enabled", "true");
        System.err.println("DB running at " + postgreSQLContainer.getJdbcUrl());
    }
}
