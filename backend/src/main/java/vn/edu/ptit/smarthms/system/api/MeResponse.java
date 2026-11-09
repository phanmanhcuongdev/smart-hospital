package vn.edu.ptit.smarthms.system.api;

import java.util.List;

public record MeResponse(
        String username,
        List<String> roles,
        List<String> permissions
) {}
