package vn.edu.ptit.smarthms.common.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;

/**
 * Mô tả: Cấu hình thứ bậc vai trò (Role Hierarchy) cho Spring Security, cho phép tài khoản Admin kế thừa quyền của các vai trò khác
 * Người thực hiện: Đoàn Thị Hồng Nhung
 * Ngày thực hiện: 12/11/2026
 */
@Configuration
public class RoleHierarchyConfiguration {

    /**
     * Mô tả: Thiết lập cây kế thừa quyền giữa vai trò quản trị viên (ROLE_ADMIN) và các vai trò nghiệp vụ khác
     * Người thực hiện: Đoàn Thị Hồng Nhung
     * Ngày thực hiện: 12/11/2026
     */
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
