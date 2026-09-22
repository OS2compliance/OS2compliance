package dk.digitalidentity.e2e;

import dk.digitalidentity.model.entity.User;
import dk.digitalidentity.samlmodule.model.SamlGrantedAuthority;
import dk.digitalidentity.samlmodule.model.TokenUser;
import dk.digitalidentity.samlmodule.model.SamlLoginPostProcessor;
import dk.digitalidentity.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Login med brugernavn og kodeord til e2e-drejebogen, så en kørsel ikke kræver en IdP.
 * Ligger under src/test og kræver profilen locallogin — den kan ikke havne i en release.
 */
@Profile("locallogin")
@Controller
@RequiredArgsConstructor
public class LocalLoginController {
    private final UserService userService;
    // Interfacetypen, ikke RolePostProcessor: bønnen er en JDK-proxy og kan ikke castes til klassen.
    private final SamlLoginPostProcessor rolePostProcessor;
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

    @Value("${e2e.password:E2E-test1234}")
    private String forventetKodeord;

    // Egen side frem for produktets: /login er Spring Securitys SAML-valgside, og den kan ikke overtages af en controller.
    @GetMapping("/e2e/login")
    @ResponseBody
    public String loginside(@RequestParam(required = false) final String fejl) {
        return """
            <!DOCTYPE html><html lang="da"><head><meta charset="utf-8"><title>E2E-login</title></head>
            <body style="font-family:sans-serif;max-width:24rem;margin:4rem auto">
            <h1>E2E-login</h1>
            %s
            <form method="post" action="/e2e/login">
              <p><label>Brugernavn<br><input name="username" autofocus></label></p>
              <p><label>Kodeord<br><input name="password" type="password"></label></p>
              <p><button type="submit">Log ind</button></p>
            </form>
            </body></html>
            """.formatted(fejl != null ? "<p style=\"color:#b00\">Forkert brugernavn eller kodeord.</p>" : "");
    }

    @PostMapping("/e2e/login")
    public String login(@RequestParam final String username, @RequestParam final String password,
                        final HttpServletRequest request, final HttpServletResponse response) {
        final Optional<User> user = userService.findByUserId(username);
        if (user.isEmpty() || !forventetKodeord.equals(password)) {
            return "redirect:/e2e/login?fejl";
        }

        // Rollerne fra databasen sendes ind som var de claims fra en IdP, så rettighederne
        // foldes ud af den samme kode som ved SAML-login.
        final TokenUser tokenUser = TokenUser.builder()
                .cvr("123456")
                .username(user.get().getUuid())
                .attributes(new HashMap<>())
                .authorities(user.get().getRoles().stream().map(SamlGrantedAuthority::new).collect(Collectors.toSet()))
                .build();
        rolePostProcessor.process(tokenUser);

        final UsernamePasswordAuthenticationToken token =
                new UsernamePasswordAuthenticationToken(tokenUser.getUsername(), null, tokenUser.getAuthorities());
        token.setDetails(tokenUser);
        final SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(token);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
        return "redirect:/dashboard";
    }

}
