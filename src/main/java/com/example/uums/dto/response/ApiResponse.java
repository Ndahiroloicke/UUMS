package com.example.uums.dto.response;

/**
 * Standard API response wrapper used by all controllers and the global exception handler.
 * Contains success flag, message, and typed data payload with static success/error factories.
 */
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ApiResponse<T> {

    private boolean success;
    private String message;
    private T data;

    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder().success(true).message(message).data(data).build();
    }

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder().success(true).message("Operation successful").data(data).build();
    }

    public static ApiResponse<Void> error(String message) {
        return ApiResponse.<Void>builder().success(false).message(message).build();
    }
}
