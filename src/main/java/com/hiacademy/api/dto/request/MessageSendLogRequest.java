package com.hiacademy.api.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MessageSendLogRequest {
    @NotBlank
    @Size(max = 20)
    private String kind; // CLASS | ALL | PAYMENT

    @NotBlank
    @Size(max = 200)
    private String targetLabel;

    @NotBlank
    @Size(max = 200)
    private String title;

    @NotBlank
    @Size(max = 2000)
    private String bodyPreview;

    @NotNull
    @Min(0)
    private Integer recipientCount;
}
