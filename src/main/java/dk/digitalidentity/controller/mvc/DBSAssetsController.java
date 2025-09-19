package dk.digitalidentity.controller.mvc;

import dk.digitalidentity.security.annotations.crud.RequireReadOwnerOnly;
import dk.digitalidentity.security.annotations.sections.RequireAsset;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Slf4j
@Controller
@RequireAsset
@RequestMapping("dbs/assets")
@RequiredArgsConstructor
public class DBSAssetsController {

	@RequireReadOwnerOnly
    @GetMapping
    public String assetsList() {
        return "dbs/assets/index";
    }

}
