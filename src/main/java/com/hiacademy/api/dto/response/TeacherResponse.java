package com.hiacademy.api.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class TeacherResponse {
    private Long id;
    private String handle;
    private String loginId;
    private String name;
    private String phone;
    private List<TeacherClassroomInfo> classrooms;
    private LocalDateTime createdAt;

    @Data
    @Builder
    public static class TeacherClassroomInfo {
        private Long id;
        private String name;
    }
}
