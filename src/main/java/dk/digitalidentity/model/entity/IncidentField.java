package dk.digitalidentity.model.entity;

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
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.Length;

import java.util.Set;

@Entity
@Table(name = "incident_fields")
@Getter
@Setter
public class IncidentField {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @NotNull
    @Column
    @Enumerated(EnumType.STRING)
    private IncidentType incidentType;

    @NotNull
    @Column(length = 2048)
    private String question;

    @Column
    private long sortKey;

    @Column
    @Length(max = 255)
    private String indexColumnName;

    @Column
    @Convert(converter = StringListNullSafeConverter.class)
    private Set<String> definedList;

}
