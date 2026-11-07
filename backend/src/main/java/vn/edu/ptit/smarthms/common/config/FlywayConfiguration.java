package vn.edu.ptit.smarthms.common.config;

import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FlywayConfiguration {
    @Bean
    CommandLineRunner migrateDatabase(DataSource dataSource) {
        return arguments -> Flyway.configure()
                .dataSource(dataSource)
                .schemas("ref", "hr", "sys", "emr")
                .locations("classpath:db/migration")
                .load()
                .migrate();
    }
}
