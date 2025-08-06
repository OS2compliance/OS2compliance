package dk.digitalidentity.model.entity;

import dk.digitalidentity.config.StringSetNullSafeConverter;
import dk.digitalidentity.model.entity.enums.DeletionProcedure;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "data_processing")
public class DataProcessing {
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "access_who_identifiers")
    @Convert(converter = StringSetNullSafeConverter.class)
    private Set<String> accessWhoIdentifiers = new HashSet<>();

    @Column(name = "access_count_identifier")
    private String accessCountIdentifier;

    // User information

    @Column(name = "person_count_identifier")
    private String personCountIdentifier;

    @OneToMany(mappedBy = "dataProcessing", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DataProcessingCategoriesRegistered> registeredCategories = new ArrayList<>();

    @Column
    private String typesOfPersonalInformationFreetext;

    // Storage

    @Column(name = "storage_time_identifier")
    private String storageTimeIdentifier;

    @Column
    @Enumerated(EnumType.STRING)
    private DeletionProcedure deletionProcedure;

    @Column
    private String deletionProcedureLink;

    @Column
    private String elaboration;

	@Column
	private boolean deletionAppliesToAll;

}
