package dk.digitalidentity.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class PageDTO<T> {
    private Long totalCount;
    private List<T> content;
}
