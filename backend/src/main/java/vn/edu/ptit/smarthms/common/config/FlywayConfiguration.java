package vn.edu.ptit.smarthms.common.config;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FlywayConfiguration {

    @Bean
    Flyway flyway(DataSource dataSource) {
        return Flyway.configure()
                .dataSource(dataSource)
                .schemas("ref", "hr", "sys", "emr")
                .locations("classpath:db/migration")
                .load();
    }

    @Bean
    InitializingBean flywayInitializer(Flyway flyway) {
        return flyway::migrate;
    }

    @Bean
    static BeanFactoryPostProcessor entityManagerFactoryDependsOnFlyway() {
        return beanFactory -> {
            if (beanFactory.containsBeanDefinition("entityManagerFactory")) {
                BeanDefinition definition = beanFactory.getBeanDefinition("entityManagerFactory");
                String[] existing = definition.getDependsOn();
                int size = existing == null ? 0 : existing.length;
                String[] dependsOn = new String[size + 1];
                for (int i = 0; i < size; i++) {
                    dependsOn[i] = existing[i];
                }
                dependsOn[size] = "flywayInitializer";
                definition.setDependsOn(dependsOn);
            }
        };
    }
}
