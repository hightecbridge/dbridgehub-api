package com.hiacademy.api.dto.request;
import jakarta.validation.constraints.*; import lombok.Data;
import java.util.List;
@Data public class NoticeRequest {
    @NotBlank private String title;
    @NotBlank private String body;
    @NotNull  private List<String> targets;
    private String imageUrl, date;
}
