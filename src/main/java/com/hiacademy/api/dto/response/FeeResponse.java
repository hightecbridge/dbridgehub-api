package com.hiacademy.api.dto.response;
import lombok.Builder; import lombok.Data;
@Data @Builder public class FeeResponse {
    private Long id;
    private String label;
    private int amount, yearMonth;
    private boolean paid;
}
