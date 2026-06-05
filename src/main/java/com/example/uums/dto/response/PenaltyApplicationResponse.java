package com.example.uums.dto.response;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PenaltyApplicationResponse {
    private int billsPenalized;
    private String details;
}
