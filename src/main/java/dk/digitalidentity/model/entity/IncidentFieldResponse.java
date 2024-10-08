package dk.digitalidentity.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import dk.digitalidentity.config.StringListNullSafeConverter;
import dk.digitalidentity.model.entity.enums.IncidentType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "incident_field_responses")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IncidentFieldResponse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(length = 2048)
    private String question;

    @Column
    @Enumerated(EnumType.STRING)
    private IncidentType incidentType;

    @Column
    private long sortKey;

    @Column
    @Convert(converter = StringListNullSafeConverter.class)
    private Set<String> definedList;

    @Column
    private String answerText;

    @Column
    private LocalDate answerDate;

    @Column
    @Convert(converter = StringListNullSafeConverter.class)
    private Set<String> answerElementIds = new HashSet<>();

    @ManyToOne
    @JoinColumn(name = "incident_id")
    @JsonIgnore
    private Incident incident;

}
