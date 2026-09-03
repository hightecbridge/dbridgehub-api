package com.hiacademy.api.dto.response;
import lombok.Builder; import lombok.Data;
import java.util.Map;
@Data @Builder public class AcademyInfo {
    private Long id;
    private String name, address, desc, phone, logoBase64;
    private Map<String, MenuFeatureFlag> menuSettings;
}
