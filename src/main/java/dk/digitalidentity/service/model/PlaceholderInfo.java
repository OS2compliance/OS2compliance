package dk.digitalidentity.service.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
@AllArgsConstructor
public class PlaceholderInfo {
    private String typesOfPersonalInformationFreetext;
    private String selectedAccessWhoTitles;
    private String selectedAccessCountTitle;
    private String howLongTitle;
    private Set<String> personalDataTypesTitles;
    private Set<String> categoriesOfRegisteredTitles;
}
