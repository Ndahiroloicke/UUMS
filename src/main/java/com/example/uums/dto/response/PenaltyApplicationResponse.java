package com.example.uums.dto.response;

/** Response DTO summarizing the result of a bulk overdue penalty application job. */
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PenaltyApplicationResponse {
    private int billsPenalized;
    private String details;
}
