package com.hiacademy.api.dto.response;
import lombok.Builder; import lombok.Data;
@Data @Builder public class AcademyInfo {
    private Long id;
    private String name, address, desc, phone, logoBase64;
}
