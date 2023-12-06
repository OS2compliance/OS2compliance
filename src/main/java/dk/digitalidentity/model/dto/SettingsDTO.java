package dk.digitalidentity.model.dto;

import dk.digitalidentity.model.entity.Setting;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
@Getter
@Setter
@NoArgsConstructor
public class SettingsDTO {
    public List<Setting> settingsList = new ArrayList<>();
    public void addList (final List<Setting> list) {
        settingsList.addAll(list);
    }
}
