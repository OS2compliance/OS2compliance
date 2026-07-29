package dk.digitalidentity.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import dk.digitalidentity.config.StringListNullSafeConverter;
import dk.digitalidentity.model.entity.enums.IncidentType;
import dk.digitalidentity.statistic.StatisticLabel;
import dk.digitalidentity.statistic.interfaces.StatisticEnabled;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
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
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "incident_field_responses")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IncidentFieldResponse implements StatisticEnabled {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(length = 2048)
    private String question;

	@StatisticLabel("Hændelsestype")
    @Column
    @Enumerated(EnumType.STRING)
    private IncidentType incidentType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incident_field_id")
    private IncidentField incidentField;

    @Column
    private long sortKey;

    @Column
    @Convert(converter = StringListNullSafeConverter.class)
    private List<String> definedList;

    @Column
    private String answerText;

	@StatisticLabel("Svartidspunkt")
    @Column
    @DateTimeFormat(pattern = "d/M-yyyy")
    private LocalDate answerDate;

    // The name is spelled out so it matches answerElementIdsRaw below: two mappings of one physical
    // column must agree on the logical name, or Hibernate refuses to build its metadata at startup.
    @Column(name = "answer_element_ids")
    @Convert(converter = StringListNullSafeConverter.class)
	@Builder.Default
    private List<String> answerElementIds = new ArrayList<>();

    @Column(name="answer_choice_values")
    @Convert(converter = StringListNullSafeConverter.class)
	@Builder.Default
    private List<String> answerChoiceValues = new ArrayList<>();

    // Read-only views of the two CSV columns above. The converted List<String> mappings cannot be used
    // in Criteria string expressions, so column filtering and search read the raw column through these.
    @JsonIgnore
    @Column(name = "answer_element_ids", insertable = false, updatable = false)
    private String answerElementIdsRaw;

    @JsonIgnore
    @Column(name = "answer_choice_values", insertable = false, updatable = false)
    private String answerChoiceValuesRaw;

    @ManyToOne
    @JoinColumn(name = "incident_id")
    @JsonIgnore
    private Incident incident;

}
