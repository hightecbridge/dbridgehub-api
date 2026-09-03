package com.hiacademy.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class StudentRequest {
    @NotBlank private String name;
    @NotBlank private String grade;
    private LocalDate birthDate;
    private String phone;
    private String status;
    @NotNull private Long classroomId;

    @NotBlank private String parentName;
    @NotBlank private String parentPhone;
    private String loginPhone;
    private String loginPassword;
    private String badgeColor;
    private String badgeTextColor;
    private Boolean kakaoLinked;
}
