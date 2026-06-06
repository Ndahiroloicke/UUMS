package com.example.uums.controller;

/**
 * REST controller for in-app billing notifications at {@code /api/notifications}.
 * Customers list notifications (all or unread), mark single or all as read,
 * and get unread counts. Notifications are created when bills are generated or paid.
 */
import com.example.uums.dto.response.ApiResponse;
import com.example.uums.dto.response.NotificationResponse;
import com.example.uums.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "View and manage billing notifications")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/customer/{customerId}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FINANCE', 'ROLE_CUSTOMER')")
    @Operation(summary = "Get all notifications for a customer")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getNotifications(
            @PathVariable Long customerId) {
        return ResponseEntity.ok(ApiResponse.success(notificationService.getNotificationsByCustomer(customerId)));
    }

    @GetMapping("/customer/{customerId}/unread")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FINANCE', 'ROLE_CUSTOMER')")
    @Operation(summary = "Get unread notifications for a customer")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getUnread(@PathVariable Long customerId) {
        return ResponseEntity.ok(ApiResponse.success(notificationService.getUnreadByCustomer(customerId)));
    }

    @PatchMapping("/{id}/read")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_CUSTOMER')")
    @Operation(summary = "Mark a notification as read")
    public ResponseEntity<ApiResponse<NotificationResponse>> markAsRead(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Notification marked as read",
                notificationService.markAsRead(id)));
    }

    @PatchMapping("/customer/{customerId}/read-all")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_CUSTOMER')")
    @Operation(summary = "Mark all notifications as read for a customer")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(@PathVariable Long customerId) {
        notificationService.markAllAsRead(customerId);
        return ResponseEntity.ok(ApiResponse.success("All notifications marked as read", null));
    }

    @GetMapping("/customer/{customerId}/count")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_CUSTOMER')")
    @Operation(summary = "Count unread notifications for a customer")
    public ResponseEntity<ApiResponse<Long>> countUnread(@PathVariable Long customerId) {
        return ResponseEntity.ok(ApiResponse.success(notificationService.countUnread(customerId)));
    }
}
