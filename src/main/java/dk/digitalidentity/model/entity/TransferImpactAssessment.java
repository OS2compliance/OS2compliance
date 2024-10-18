package dk.digitalidentity.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import dk.digitalidentity.config.StringSetConverter;
import dk.digitalidentity.model.entity.enums.AccessType;
import dk.digitalidentity.model.entity.enums.ForwardInformationToOtherSuppliers;
import dk.digitalidentity.model.entity.enums.TiaAssessment;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Set;

@Entity
@Table(name = "tia")
@Getter
@Setter
public class TransferImpactAssessment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToOne
    @JoinColumn(name = "asset_id")
    @JsonIgnore
    private Asset asset;

    @Column(name = "forward_information_other_suppliers")
    @Enumerated(EnumType.STRING)
    private ForwardInformationToOtherSuppliers forwardInformationToOtherSuppliers;

    @Column(name = "forward_information_other_suppliers_detail")
    private String forwardInformationToOtherSuppliersDetail;

    @Column(name = "information_types")
    @Convert(converter = StringSetConverter.class)
    private Set<String> informationTypes;

    @Column(name = "registered_categories")
    @Convert(converter = StringSetConverter.class)
    private Set<String> registeredCategories;

    @Column
    private String expectedTransferDuration;

    @Column
    private String transferCaseDescription;

    @Column
    @Enumerated(EnumType.STRING)
    private AccessType accessType;

    @Column
    private String technicalSecurityMeasures;

    @Column
    private String organizationalSecurityMeasures;

    @Column
    private String contractualSecurityMeasures;

    @Column
    private String conclusion;

    @Column
    @Enumerated(EnumType.STRING)
    private TiaAssessment assessment;


}
