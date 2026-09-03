package com.hiacademy.api.dto.request;

import com.hiacademy.api.dto.response.MenuFeatureFlag;
import lombok.Data;

import java.util.Map;

@Data
public class UpdateMenuSettingsRequest {
    private Map<String, MenuFeatureFlag> menus;
}
