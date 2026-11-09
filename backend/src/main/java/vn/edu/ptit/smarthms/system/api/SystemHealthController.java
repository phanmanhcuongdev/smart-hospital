package vn.edu.ptit.smarthms.system.api;

import java.time.Instant;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/system")
public class SystemHealthController {
    private final javax.sql.DataSource dataSource;

    public SystemHealthController(javax.sql.DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        try (java.sql.Connection connection = dataSource.getConnection()) {
            boolean available = connection.isValid(2);
            return ResponseEntity.status(available ? 200 : 503).body(Map.of(
                    "status", available ? "UP" : "DOWN", "timestamp", Instant.now().toString()));
        } catch (java.sql.SQLException exception) {
            return ResponseEntity.status(503).body(Map.of("status", "DOWN", "timestamp", Instant.now().toString()));
        }
    }
}
