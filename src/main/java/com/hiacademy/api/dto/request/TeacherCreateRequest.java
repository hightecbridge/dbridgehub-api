package com.hiacademy.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class TeacherCreateRequest {
    @NotBlank
    private String handle;
    @NotBlank
    private String name;
    private String phone;
    @NotBlank
    @Size(min = 4)
    private String password;
    private List<Long> classroomIds;
}
