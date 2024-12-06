package dk.digitalidentity.controller.mvc.Assets;

import dk.digitalidentity.security.RequireUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Slf4j
@Controller
@RequireUser
@RequestMapping("assets/roles")
@RequiredArgsConstructor
public class RoleController {


}
