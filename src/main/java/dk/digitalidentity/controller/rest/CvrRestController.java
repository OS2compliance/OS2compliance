package dk.digitalidentity.controller.rest;

import dk.digitalidentity.integration.cvr.CvrService;
import dk.digitalidentity.integration.cvr.dto.CvrSearchResultDTO;
import dk.digitalidentity.security.RequireUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@RestController
@RequestMapping("rest/cvr")
@RequireUser
@RequiredArgsConstructor
public class CvrRestController {
    private final CvrService cvrService;

    @GetMapping
    public CvrSearchResultDTO findCompany(@RequestParam final String cvr) {
        return cvrService.getSearchResultByCvr(cvr)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

}
