package dk.digitalidentity.model.entity;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "dbs_oversight")
@Getter
@Setter
public class DBSOversight {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private Long dbsId;

    @Column
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dbs_supplier_id")
    private DBSSupplier supplier;

    @Column
    private boolean locked;

    @Column
    private boolean taskCreated;

    @Column
    private LocalDateTime created;

	/**
	 * Auditens publishedDate som platformen senest har vist den. Genudgivelser opdages som et
	 * fremadrettet hop i dette felt; null = endnu ikke set af platform-syncen. Adskilt fra
	 * created, som på adopterede rækker stammer fra den gamle integration og er usammenlignelig.
	 */
	@Column(name = "published_date")
	private LocalDateTime publishedDate;

	@Column(name = "audit_link")
	private String auditLink;

	/**
	 * De DBS-systemer auditens systems[] dækker. Tom mængde = ældre række uden systemdata;
	 * opgavejobbet falder da tilbage til alle leverandørens aktiver.
	 */
	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(name = "dbs_oversight_assets",
			joinColumns = @JoinColumn(name = "dbs_oversight_id"),
			inverseJoinColumns = @JoinColumn(name = "dbs_asset_id"))
	private Set<DBSAsset> assets = new HashSet<>();

}
