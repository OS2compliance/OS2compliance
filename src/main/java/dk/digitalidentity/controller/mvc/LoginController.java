package dk.digitalidentity.controller.mvc;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping()
class LoginController {

    @GetMapping("login")
    String login() {
        return "login";
    }
}
