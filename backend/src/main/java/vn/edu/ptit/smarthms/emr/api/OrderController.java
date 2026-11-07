package vn.edu.ptit.smarthms.emr.api;

import java.util.List;
import java.util.UUID;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.ptit.smarthms.common.security.Actor;
import vn.edu.ptit.smarthms.common.security.ActorResolver;
import vn.edu.ptit.smarthms.emr.api.EmrRequests.OrderRequest;
import vn.edu.ptit.smarthms.emr.api.EmrRequests.ResultRequest;
import vn.edu.ptit.smarthms.emr.api.EmrResponses.OrderResponse;
import vn.edu.ptit.smarthms.emr.api.EmrResponses.ResultResponse;
import vn.edu.ptit.smarthms.emr.service.OrderService;

/** CLS orders and results (FR-EMR-02). */
@RestController
@RequestMapping("/api/v1/emr")
public class OrderController {

    private final OrderService orders;

    public OrderController(OrderService orders) {
        this.orders = orders;
    }

    private Actor actor(Authentication authentication) {
        return ActorResolver.from(authentication);
    }

    @PostMapping("/encounters/{encounterId}/orders")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<OrderResponse> createOrder(Authentication authentication,
            @PathVariable UUID encounterId, @Valid @RequestBody OrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(orders.createOrder(actor(authentication), encounterId, request));
    }

    @GetMapping("/orders/{orderId}")
    @PreAuthorize("hasAnyRole('DOCTOR', 'NURSE', 'CLS_TECHNICIAN', 'RECORDS_MANAGER', 'RECEPTION')")
    public OrderResponse getOrder(Authentication authentication, @PathVariable UUID orderId) {
        return orders.getOrder(actor(authentication), orderId);
    }

    @PostMapping("/orders/{orderId}/submit")
    @PreAuthorize("hasRole('DOCTOR')")
    public OrderResponse submitOrder(Authentication authentication, @PathVariable UUID orderId) {
        return orders.submitOrder(actor(authentication), orderId);
    }

    @PostMapping("/orders/{orderId}/acknowledge")
    @PreAuthorize("hasRole('CLS_TECHNICIAN')")
    public OrderResponse acknowledgeOrder(Authentication authentication, @PathVariable UUID orderId) {
        return orders.acknowledgeOrder(actor(authentication), orderId);
    }

    @PostMapping("/orders/{orderId}/start")
    @PreAuthorize("hasRole('CLS_TECHNICIAN')")
    public OrderResponse startOrder(Authentication authentication, @PathVariable UUID orderId) {
        return orders.startOrder(actor(authentication), orderId);
    }

    @PostMapping("/orders/{orderId}/cancel")
    @PreAuthorize("hasRole('DOCTOR')")
    public OrderResponse cancelOrder(Authentication authentication, @PathVariable UUID orderId) {
        return orders.cancelOrder(actor(authentication), orderId);
    }

    @PostMapping("/orders/{orderId}/results")
    @PreAuthorize("hasRole('CLS_TECHNICIAN')")
    public ResponseEntity<ResultResponse> receiveResult(Authentication authentication,
            @PathVariable UUID orderId, @Valid @RequestBody ResultRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(orders.receiveResult(actor(authentication), orderId, request));
    }

    @GetMapping("/orders/{orderId}/results")
    @PreAuthorize("hasAnyRole('DOCTOR', 'NURSE', 'CLS_TECHNICIAN', 'RECORDS_MANAGER', 'RECEPTION')")
    public List<ResultResponse> listResults(Authentication authentication,
            @PathVariable UUID orderId) {
        return orders.listResults(actor(authentication), orderId);
    }
}
