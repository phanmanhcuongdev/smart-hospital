package vn.edu.ptit.smarthms.common.error;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ApiExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @RestController
    @RequestMapping("/test")
    static class TestController {
        @GetMapping("/app-exception")
        public String throwAppException() {
            throw new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Item not found");
        }

        @GetMapping("/access-denied")
        public String throwAccessDenied() {
            throw new AccessDeniedException("Forbidden action");
        }

        @GetMapping("/internal-error")
        public String throwUnexpected() {
            throw new RuntimeException("unexpected");
        }

        @PostMapping("/validate")
        public String validateBody(@RequestBody @Valid ValidatableDto dto) {
            return "ok";
        }
    }

    record ValidatableDto(@NotBlank String name) {}

    @Test
    void appException_shouldReturnCorrectHttpStatusAndCode() throws Exception {
        mockMvc.perform(get("/test/app-exception"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Item not found"))
                .andExpect(jsonPath("$.correlationId").isString())
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    void accessDeniedException_shouldReturn403() throws Exception {
        mockMvc.perform(get("/test/access-denied"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    void unexpectedException_shouldReturn500() throws Exception {
        mockMvc.perform(get("/test/internal-error"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"));
    }

    @Test
    void validationFailure_shouldReturn422WithFieldErrors() throws Exception {
        mockMvc.perform(post("/test/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.details.name").isString());
    }
}
