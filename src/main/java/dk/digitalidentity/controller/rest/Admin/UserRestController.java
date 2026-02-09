package dk.digitalidentity.controller.rest.Admin;

import dk.digitalidentity.dao.UserDao;
import dk.digitalidentity.mapping.UserMapper;
import dk.digitalidentity.model.dto.PageDTO;
import dk.digitalidentity.model.dto.ResponsibleUserTableDTO;
import dk.digitalidentity.model.dto.UserDTO;
import dk.digitalidentity.model.dto.excel.ExcelExportRequest;
import dk.digitalidentity.model.dto.excel.ExportMetadataDTO;
import dk.digitalidentity.security.annotations.RequireAuthenticated;
import dk.digitalidentity.security.annotations.crud.RequireReadOwnerOnly;
import dk.digitalidentity.service.ExcelExportHelperService;
import dk.digitalidentity.service.ResponsibleUserViewService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("rest/users")
@RequireAuthenticated
@RequiredArgsConstructor
public class UserRestController {
    private final UserDao userDao;
    private final UserMapper userMapper;
	private final ExcelExportHelperService excelExportHelperService;
	private final ResponsibleUserViewService responsibleUserViewService;


	@RequireReadOwnerOnly
    @GetMapping("autocomplete")
    public PageDTO<UserDTO> autocomplete(@RequestParam("search") final String search) {
        final Pageable page = PageRequest.of(0, 25, Sort.by("name").ascending());
        if (StringUtils.length(search) == 0) {
            return userMapper.toDTO(userDao.findAllByActiveTrue(page));
        } else {
            final String replacedString = search.replace(' ', '%');
            return userMapper.toDTO(userDao.searchForUser("%" + replacedString + "%", page));
        }

    }

	@RequireReadOwnerOnly
	@GetMapping("inactive/export-metadata")
	public ExportMetadataDTO getExportMetadata() {
		return excelExportHelperService.getMetadata(ResponsibleUserTableDTO.class);
	}

	@PostMapping("inactive/export-custom")
	@RequireReadOwnerOnly
	public void exportCustom(
			@RequestBody ExcelExportRequest request,
			HttpServletResponse response
	) throws IOException {
		List<ResponsibleUserTableDTO> inactiveResponsibleUsers = responsibleUserViewService.findInactiveResponsibleUsersByIds(request.getSelectedIds());

		if (inactiveResponsibleUsers.isEmpty()) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}

		excelExportHelperService.exportEntities(
				ResponsibleUserTableDTO.class,
				inactiveResponsibleUsers,
				request,
				response
		);
	}

}
