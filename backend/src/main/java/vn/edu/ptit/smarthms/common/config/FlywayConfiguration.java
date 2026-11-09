package vn.edu.ptit.smarthms.common.config;

import jakarta.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import vn.edu.ptit.smarthms.system.repository.UserAccountRepository;

@Configuration
public class FlywayConfiguration {

    @Bean(initMethod = "migrate")
    public Flyway flyway(DataSource dataSource) {
        return Flyway.configure()
                .dataSource(dataSource)
                .schemas("ref", "hr", "sys", "emr")
                .locations("classpath:db/migration")
                .load();
    }

    @Bean
    public static BeanFactoryPostProcessor flywayEntityManagerFactoryDependencyPostProcessor() {
        return beanFactory -> {
            String[] emfBeanNames = beanFactory.getBeanNamesForType(EntityManagerFactory.class, true, false);
            for (String emfBeanName : emfBeanNames) {
                if (beanFactory.containsBeanDefinition(emfBeanName)) {
                    BeanDefinition bd = beanFactory.getBeanDefinition(emfBeanName);
                    bd.setDependsOn("flyway");
                }
            }
            if (beanFactory.containsBeanDefinition("entityManagerFactory")) {
                BeanDefinition bd = beanFactory.getBeanDefinition("entityManagerFactory");
                bd.setDependsOn("flyway");
            }
        };
    }

    @Bean
    CommandLineRunner ensureDemoAccounts(Flyway flyway,
                                         UserAccountRepository userAccountRepository,
                                         PasswordEncoder passwordEncoder) {
        return arguments -> {
            userAccountRepository.findByUsername("admin").ifPresent(admin -> {
                if (!passwordEncoder.matches("Admin@2026!", admin.getPasswordHash())) {
                    admin.setPasswordHash(passwordEncoder.encode("Admin@2026!"));
                    userAccountRepository.save(admin);
                }
            });

            userAccountRepository.findByUsername("doctor1").ifPresent(doctor -> {
                if (!passwordEncoder.matches("Doctor@2026!", doctor.getPasswordHash())) {
                    doctor.setPasswordHash(passwordEncoder.encode("Doctor@2026!"));
                    userAccountRepository.save(doctor);
                }
            });
        };
    }
}
