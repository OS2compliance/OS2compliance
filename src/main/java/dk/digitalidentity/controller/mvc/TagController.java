package dk.digitalidentity.controller.mvc;

import dk.digitalidentity.dao.TagDao;
import dk.digitalidentity.model.entity.Tag;
import dk.digitalidentity.security.RequireUser;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@SuppressWarnings("SpringJavaAutowiredFieldsWarningInspection")
@Slf4j
@Controller
@RequestMapping("tags")
@RequireUser
public class TagController {
    @Autowired
    private TagDao tagDao;

    @GetMapping("autocomplete")
    public String autocomplete(final Model model,
                               @RequestParam("search") final String search,
                               @RequestParam("id") final String datalistId) {
        if (StringUtils.length(search) == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        final List<Tag> tags = tagDao.findByValueIsLikeIgnoreCaseOrderByValue("%" + search + "%", Pageable.ofSize(15))
                .getContent();
        model.addAttribute("tags", tags);
        model.addAttribute("dataListId", datalistId);
        return "tags/autocomplete";
    }

    @PostMapping("{value}")
    @ResponseStatus(value = HttpStatus.CREATED)
    @Transactional
    public void create(@PathVariable("value") final String value) {
        final Optional<Tag> optionalTag = tagDao.findByValue(value);
        if (optionalTag.isEmpty()) {
            final var tag = Tag.builder().value(value).build();
            tagDao.save(tag);
        }
    }

}
