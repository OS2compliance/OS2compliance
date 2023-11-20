package dk.digitalidentity.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import dk.digitalidentity.config.StringListNullSafeConverter;
import dk.digitalidentity.model.entity.enums.InformationPassedOn;
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
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "data_processing_categories_registered")
@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class DataProcessingCategoriesRegistered {
    @Id
    @Column
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private String personCategoriesRegisteredIdentifier;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne
    @JoinColumn(name = "data_processing_id")
    @JsonIgnore
    private DataProcessing dataProcessing;

    @Column(name = "person_cat_info_identifiers")
    @Convert(converter = StringListNullSafeConverter.class)
    @Builder.Default
    private Set<String> personCategoriesInformationIdentifiers = new HashSet<>();

    @Column(name = "information_passed_on")
    @Enumerated(EnumType.STRING)
    private InformationPassedOn informationPassedOn;

    @Column(name = "information_receivers")
    @Convert(converter = StringListNullSafeConverter.class)
    @Builder.Default
    private Set<String> informationReceivers = new HashSet<>();

}
