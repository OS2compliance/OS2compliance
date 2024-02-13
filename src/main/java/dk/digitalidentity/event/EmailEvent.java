package dk.digitalidentity.event;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailEvent {
    private String email;
    private String subject;
    private String message;
    private List<String> attachments = new ArrayList<>();
}
