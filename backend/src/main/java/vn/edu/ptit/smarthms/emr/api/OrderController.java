package vn.edu.ptit.smarthms.emr.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vn.edu.ptit.smarthms.common.security.ActorResolver;
import vn.edu.ptit.smarthms.emr.domain.MedicalOrder;
import vn.edu.ptit.smarthms.emr.service.OrderService;

@RestController
@RequestMapping("/api/v1/emr/encounters/{encounterId}/orders")
public class OrderController {
    public record CreateRequest(@NotNull MedicalOrder.Type orderType,
                                @Size(max = 160) String externalOrderRef) {}

    public record StatusRequest(@NotNull MedicalOrder.Status status) {}

    public record OrderResponse(UUID orderId, UUID encounterId, UUID orderedBy, String orderType,
                                String orderStatus, String externalOrderRef, OffsetDateTime orderedAt) {
        static OrderResponse from(MedicalOrder o) {
            return new OrderResponse(o.getOrderId(), o.getEncounterId(), o.getOrderedBy(),
                    o.getOrderType().name(), o.getOrderStatus().name(),
                    o.getExternalOrderRef(), o.getOrderedAt());
        }
    }

    private final OrderService service;

    public OrderController(OrderService service) { this.service = service; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public OrderResponse create(Authentication authentication,
                                @PathVariable UUID encounterId,
                                @Valid @RequestBody CreateRequest req) {
        return OrderResponse.from(service.create(ActorResolver.from(authentication), encounterId, req.orderType(), req.externalOrderRef()));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('DOCTOR', 'NURSE', 'CLS_TECHNICIAN', 'RECORDS_MANAGER', 'ADMIN')")
    public List<OrderResponse> list(@PathVariable UUID encounterId) {
        return service.list(encounterId).stream().map(OrderResponse::from).toList();
    }

    @PatchMapping("/{orderId}/status")
    @PreAuthorize("hasAnyRole('DOCTOR', 'CLS_TECHNICIAN', 'ADMIN')")
    public OrderResponse changeStatus(Authentication authentication,
                                      @PathVariable UUID encounterId,
                                      @PathVariable UUID orderId,
                                      @Valid @RequestBody StatusRequest req) {
        return OrderResponse.from(service.changeStatus(ActorResolver.from(authentication), encounterId, orderId, req.status()));
    }
}
