package com.hiacademy.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MenuFeatureFlag {
    private Boolean enabled;
    private Boolean parentVisible;
}
