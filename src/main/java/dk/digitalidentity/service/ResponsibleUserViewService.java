package dk.digitalidentity.service;

import dk.digitalidentity.dao.view.ResponsibleUserViewDao;
import dk.digitalidentity.mapping.RelatableMapper;
import dk.digitalidentity.model.dto.RelatableDTO;
import dk.digitalidentity.model.dto.ResponsibleUserTableDTO;
import dk.digitalidentity.model.entity.Relatable;
import dk.digitalidentity.model.entity.view.ResponsibleUserView;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class ResponsibleUserViewService {
    private final ResponsibleUserViewDao responsibleUserViewDao;
    private final RelatableService relatableService;
    private final RelatableMapper relatableMapper;

    public List<ResponsibleUserTableDTO> findInactiveResponsibleUsers() {
        List<ResponsibleUserTableDTO> result = new ArrayList<>();
        List<ResponsibleUserView> users = responsibleUserViewDao.findByActiveFalse();
        for (ResponsibleUserView user : users) {
            List<Long> ids = user.getResponsibleRelatableIds().stream().map(Long::parseLong).collect(Collectors.toList());
            List<Relatable> relatables = relatableService.findAllById(ids);
            List<RelatableDTO> relatableDTOS = relatableMapper.toDTO(relatables);
            result.add(new ResponsibleUserTableDTO(user.getUuid(), user.getName(), user.getUserId(), relatableDTOS));
        }
        return result;
    }

    public ResponsibleUserView findByUserUuid(String uuid) {
        return responsibleUserViewDao.findByUuid(uuid);
    }

    public List<ResponsibleUserView> findAllIn(Collection<String> uuids) {
        return responsibleUserViewDao.findAllByUuidIn(uuids);
    }

}
