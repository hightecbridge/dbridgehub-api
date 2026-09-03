package com.hiacademy.api.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class TeacherUpdateRequest {
    private String handle;
    private String name;
    private String phone;
    private String password;
    private List<Long> classroomIds;
}
