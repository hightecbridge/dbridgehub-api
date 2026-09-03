package com.hiacademy.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AdminConsultRequest {
    /** 재원생 | 신규 */
    @NotBlank private String kind;
    private Long studentId;
    private Long teacherUserId;
    @NotBlank private String date;
    private String time;
    private String content;
    private String status;
    private String prospectName;
    private String prospectPhone;
    private String prospectGrade;
    private String prospectParentName;
}
