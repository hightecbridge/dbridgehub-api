package com.hiacademy.api.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ParentLoginAcademyOption {
    private Long id;
    private String name;
    private String address;
    private int studentCount;
}
