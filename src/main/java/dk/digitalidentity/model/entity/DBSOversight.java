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
	 * Auditens publishedDate som platform-API'et senest har vist den. Adskilt fra created, fordi
	 * created på rækker adopteret fra den gamle integration stammer fra det gamle systems
	 * dokumentdato og ikke kan sammenlignes med platformens publishedDate. Genudgivelser opdages
	 * som et fremadrettet hop i netop dette felt; null betyder at platform-syncen ikke har set
	 * rækken endnu.
	 */
	@Column(name = "published_date")
	private LocalDateTime publishedDate;

	@Column(name = "audit_link")
	private String auditLink;

	/**
	 * De DBS-systemer auditens systems[] dækker. Tom mængde betyder en ældre række uden
	 * systemdata - opgavejobbet falder da tilbage til alle leverandørens aktiver, som var
	 * adfærden før koblingen fandtes.
	 */
	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(name = "dbs_oversight_assets",
			joinColumns = @JoinColumn(name = "dbs_oversight_id"),
			inverseJoinColumns = @JoinColumn(name = "dbs_asset_id"))
	private Set<DBSAsset> assets = new HashSet<>();

}
