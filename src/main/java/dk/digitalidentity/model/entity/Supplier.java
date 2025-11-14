package dk.digitalidentity.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import dk.digitalidentity.model.dto.tag.Tagable;
import dk.digitalidentity.model.entity.enums.RelationType;
import dk.digitalidentity.model.entity.enums.SupplierStatus;
import dk.digitalidentity.model.entity.interfaces.HasSingleResponsibleUser;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.ResultCheckStyle;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.Where;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "suppliers")
@Getter
@Setter
@SQLDelete(sql = "UPDATE suppliers SET deleted = true WHERE id=? and version=?", check = ResultCheckStyle.COUNT)
public class Supplier extends Relatable implements HasSingleResponsibleUser, Tagable {

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "responsible_uuid")
	private User responsibleUser;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private SupplierStatus status = SupplierStatus.IN_PROGRESS;

	@Column
	private String cvr;

	@Column
	private String zip;

	@Column
	private String city;

	@Column
	private String address;

	@Column
	private String contact;

	@Column
	private String phone;

	@Column
	private String email;

	@Column
	private String country;

	@Column
	private boolean personalInfo;

	@Column
	private boolean dataProcessor;

	@Column
	private String description;

	@ToString.Exclude
	@EqualsAndHashCode.Exclude
	@OneToMany(mappedBy = "supplier")
    @JsonIgnore
	private List<Asset> assets = new ArrayList<>();

	@ManyToMany(fetch = FetchType.LAZY, cascade = { CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH, CascadeType.DETACH})
	@JoinTable(name = "supplier_tag", joinColumns = { @JoinColumn(name = "supplier_id") }, inverseJoinColumns = { @JoinColumn(name = "tag_id") })
	private Set<Tag> tags = new HashSet<>();

	@Override
	public RelationType getRelationType() {
		return RelationType.SUPPLIER;
	}

    @Override
    public String getLocalizedEnumValues() {
        return status != null ? status.getMessage() : "";
    }
}
