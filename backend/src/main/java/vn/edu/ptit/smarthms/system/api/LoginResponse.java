package vn.edu.ptit.smarthms.system.api;

import java.time.Instant;
import java.util.List;

public record LoginResponse(
        String token,
        Instant expiresAt,
        String username,
        List<String> roles
) {}
