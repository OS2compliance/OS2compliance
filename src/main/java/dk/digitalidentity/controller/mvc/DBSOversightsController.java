package dk.digitalidentity.controller.mvc;

import dk.digitalidentity.security.RequireUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Slf4j
@Controller
@RequireUser
@RequestMapping("dbs/oversight")
@RequiredArgsConstructor
public class DBSOversightsController {

    @GetMapping
    public String assetsList() {
        return "dbs/oversight/index";
    }

}
