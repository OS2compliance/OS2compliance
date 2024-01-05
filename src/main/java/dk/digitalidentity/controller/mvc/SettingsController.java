package dk.digitalidentity.controller.mvc;

import dk.digitalidentity.config.OS2complianceConfiguration;
import dk.digitalidentity.model.dto.SettingsDTO;
import dk.digitalidentity.security.RequireAdminstrator;
import dk.digitalidentity.security.RequireUser;
import dk.digitalidentity.service.KitosService;
import dk.digitalidentity.service.SettingsService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Objects;


@Slf4j
@Controller
@RequestMapping("settings")
@RequireUser
@RequireAdminstrator
@RequiredArgsConstructor
public class SettingsController {
	private final SettingsService settingsService;
    private final HttpServletRequest httpServletRequest;
    private final KitosService kitosService;
    private final OS2complianceConfiguration configuration;

	@Transactional
	@GetMapping("form")
	public String form(final Model model) {
        final SettingsDTO settings = new SettingsDTO();
        settings.addList(settingsService.getByEditable());
		model.addAttribute("Settings", settings);
        model.addAttribute("page", getParentType(httpServletRequest.getHeader("Referer")));
        model.addAttribute("kitosRoles", kitosService.kitosRoles());
        model.addAttribute("kitosEnabled", configuration.getIntegrations().getKitos().isEnabled());

		return "fragments/settings";
	}

	@Transactional
	@PostMapping("update")
	public String update(@ModelAttribute final SettingsDTO settings){

        if(!settings.settingsList.isEmpty()) {
            settings.settingsList.removeIf( x ->  Objects.isNull( x.getSettingValue()) || x.getSettingValue().isEmpty());
            final var res = settingsService.saveAll(settings.settingsList);
        }
		return "redirect:" + httpServletRequest.getHeader("Referer");
	}

    private String getParentType(final String url){
        if(url.contains("standards")) return "standards";
        if(url.contains("registers")) return "registers";
        if(url.contains("assets")) return "assets";
        if(url.contains("suppliers")) return "suppliers";
        if(url.contains("risks")) return "risks";
        if(url.contains("documents")) return "documents";
        if(url.contains("tasks")) return "tasks";
        if(url.contains("reports")) return "tasks";

        return "";
    }
}
