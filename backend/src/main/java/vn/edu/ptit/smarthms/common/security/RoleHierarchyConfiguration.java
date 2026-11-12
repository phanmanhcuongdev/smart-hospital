package vn.edu.ptit.smarthms.common.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;

@Configuration
public class RoleHierarchyConfiguration {

    @Bean
    public RoleHierarchy roleHierarchy() {
        return RoleHierarchyImpl.fromHierarchy(String.join("\n",
                "ROLE_ADMIN > ROLE_HR_SPECIALIST",
                "ROLE_ADMIN > ROLE_HR_APPROVER",
                "ROLE_ADMIN > ROLE_DOCTOR",
                "ROLE_ADMIN > ROLE_NURSE",
                "ROLE_ADMIN > ROLE_CLS_TECHNICIAN",
                "ROLE_ADMIN > ROLE_RECORDS_MANAGER",
                "ROLE_ADMIN > ROLE_RECEPTION",
                "ROLE_ADMIN > ROLE_RECEPTIONIST",
                "ROLE_ADMIN > ROLE_PHARMACIST"
        ));
    }

    @Bean
    public MethodSecurityExpressionHandler methodSecurityExpressionHandler(RoleHierarchy roleHierarchy) {
        DefaultMethodSecurityExpressionHandler expressionHandler = new DefaultMethodSecurityExpressionHandler();
        expressionHandler.setRoleHierarchy(roleHierarchy);
        return expressionHandler;
    }
}
