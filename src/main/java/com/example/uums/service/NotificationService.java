package com.example.uums.service;

/**
 * Handles in-app notifications and optional email delivery for billing events.
 * Persists notifications, manages read/unread state and counts, and sends email
 * via SMTP without failing the main transaction if mail delivery fails.
 */
import com.example.uums.dto.response.NotificationResponse;
import com.example.uums.entity.Customer;
import com.example.uums.entity.Notification;
import com.example.uums.exception.ResourceNotFoundException;
import com.example.uums.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String mailFrom;

    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotificationsByCustomer(Long customerId) {
        return notificationRepository.findByCustomerIdOrderByCreatedAtDesc(customerId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getUnreadByCustomer(Long customerId) {
        return notificationRepository.findByCustomerIdAndIsReadFalse(customerId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    public NotificationResponse markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id: " + notificationId));
        notification.setIsRead(true);
        return mapToResponse(notificationRepository.save(notification));
    }

    @Transactional
    public void markAllAsRead(Long customerId) {
        notificationRepository.findByCustomerIdAndIsReadFalse(customerId)
                .forEach(n -> {
                    n.setIsRead(true);
                    notificationRepository.save(n);
                });
    }

    @Transactional
    public void saveInAppNotification(Customer customer, String message) {
        notificationRepository.save(Notification.builder()
                .customer(customer)
                .message(message)
                .isRead(false)
                .build());
    }

    /** Sends email and logs failures without breaking the main transaction. */
    public void sendEmailSilently(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailFrom);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Email sent successfully to {}", to);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage(), e);
        }
    }

    public long countUnread(Long customerId) {
        return notificationRepository.countByCustomerIdAndIsReadFalse(customerId);
    }

    private NotificationResponse mapToResponse(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .customerId(n.getCustomer().getId())
                .message(n.getMessage())
                .isRead(n.getIsRead())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
