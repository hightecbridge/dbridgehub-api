package com.hiacademy.api.dto.request;
import lombok.Data;
@Data public class UpdateProfileRequest {
    private String name, phone, profileImageBase64;
    private String academyName, academyAddress, academyDesc, academyLogoBase64;
}
