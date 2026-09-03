package com.hiacademy.api.service;

import com.hiacademy.api.dto.response.MenuFeatureFlag;
import com.hiacademy.api.entity.Academy;
import com.hiacademy.api.menu.MenuSettings;
import com.hiacademy.api.repository.AcademyRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@Service
@Transactional
public class MenuSettingsService {
    private final AcademyRepository academyRepo;

    public MenuSettingsService(AcademyRepository academyRepo) {
        this.academyRepo = academyRepo;
    }

    @Transactional(readOnly = true)
    public Map<String, MenuFeatureFlag> get(Long academyId) {
        return MenuSettings.merge(academy(academyId).getMenuSettingsJson());
    }

    public Map<String, MenuFeatureFlag> save(Long academyId, Map<String, MenuFeatureFlag> incoming) {
        Academy academy = academy(academyId);
        academy.setMenuSettingsJson(MenuSettings.toJson(incoming));
        academyRepo.save(academy);
        return MenuSettings.merge(academy.getMenuSettingsJson());
    }

    @Transactional(readOnly = true)
    public void requireParentFeature(Long academyId, String key) {
        if (academyId == null) return;
        if (!MenuSettings.parentVisible(get(academyId), key)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "학원에서 학부모에게 제공하지 않는 기능입니다.");
        }
    }

    @Transactional(readOnly = true)
    public void requireAnyParentFeature(Long academyId, String... keys) {
        if (academyId == null || keys == null || keys.length == 0) return;
        Map<String, MenuFeatureFlag> settings = get(academyId);
        for (String key : keys) {
            if (MenuSettings.parentVisible(settings, key)) return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "학원에서 학부모에게 제공하지 않는 기능입니다.");
    }

    @Transactional(readOnly = true)
    public boolean parentVisible(Long academyId, String key) {
        if (academyId == null) return true;
        return MenuSettings.parentVisible(get(academyId), key);
    }

    private Academy academy(Long academyId) {
        return academyRepo.findById(academyId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "학원 정보를 찾을 수 없습니다."));
    }
}
