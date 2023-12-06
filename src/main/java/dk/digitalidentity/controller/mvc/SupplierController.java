package dk.digitalidentity.controller.mvc;

import dk.digitalidentity.dao.AssetOversightDao;
import dk.digitalidentity.dao.ContactDao;
import dk.digitalidentity.dao.RelationDao;
import dk.digitalidentity.dao.SupplierDao;
import dk.digitalidentity.model.entity.Asset;
import dk.digitalidentity.model.entity.AssetOversight;
import dk.digitalidentity.model.entity.Contact;
import dk.digitalidentity.model.entity.Relatable;
import dk.digitalidentity.model.entity.Supplier;
import dk.digitalidentity.model.entity.enums.RelationType;
import dk.digitalidentity.model.entity.enums.SupplierStatus;
import dk.digitalidentity.security.RequireUser;
import dk.digitalidentity.service.AssetService;
import dk.digitalidentity.service.RelationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequestMapping("suppliers")
@RequireUser
public class SupplierController {
	private final SupplierDao supplierDao;
	private final RelationDao relationDao;
	private final ContactDao contactDao;
    private final AssetOversightDao assetOversightDao;

    private final RelationService relationService;
    private final AssetService assetService;

	public SupplierController(final SupplierDao supplierDao, final RelationDao relationDao, final ContactDao contactDao, final AssetOversightDao assetOversightDao, final RelationService relationService, final AssetService assetService) {
		this.supplierDao = supplierDao;
		this.relationDao = relationDao;
		this.contactDao = contactDao;
		this.assetOversightDao = assetOversightDao;
		this.relationService = relationService;
        this.assetService = assetService;
    }

	@GetMapping
	public String suppliersList() {
		return "suppliers/index";
	}

	@GetMapping("{id}")
	public String supplier(final Model model, @PathVariable final String id) {
		final Supplier supplier = supplierDao.findById(Long.valueOf(id))
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
		final List<Contact> contacts = relationDao.findRelatedToWithType(supplier.getId(), RelationType.CONTACT).stream()
				.map(r -> r.getRelationAType() == RelationType.CONTACT ? r.getRelationAId() : r.getRelationBId())
				.map(rid -> contactDao.findById(rid).orElse(null))
				.filter(Objects::nonNull)
				.collect(Collectors.toList());

        final List<Asset> assetsDirect = assetService.findBySupplier(supplier);
        final List<Relatable> assetRelated = relationService.findAllRelatedTo(supplier).stream().filter(r -> r.getRelationType() == RelationType.ASSET).toList();
        final List<Relatable> documents = relationService.findAllRelatedTo(supplier).stream().filter(r -> r.getRelationType() == RelationType.DOCUMENT).toList();
        final List<Relatable> tasks = relationService.findAllRelatedTo(supplier).stream().filter(r -> r.getRelationType() == RelationType.TASK).toList();

        final List<AssetOversight> assetOversights = assetOversightDao.findAll().stream().filter(o -> o.getAsset().getSupplier().equals(supplier)).collect(Collectors.toList());

        model.addAttribute("oversights", assetOversights);
		model.addAttribute("supplier", supplier);
        model.addAttribute("tasks", tasks);
        model.addAttribute("documents", documents);
        model.addAttribute("assetsDirect", assetsDirect);
        model.addAttribute("assetsRelated", assetRelated);
		model.addAttribute("contacts", contacts);
		return "suppliers/view";
	}

	@DeleteMapping("{id}")
	@ResponseStatus(value = HttpStatus.OK)
	@Transactional
	public void supplierDelete(@PathVariable final String id) {
		final Long lid = Long.valueOf(id);
		relationDao.deleteRelatedTo(lid);
		supplierDao.deleteById(lid);
	}

	@GetMapping("form")
	public String form(final Model model, @RequestParam(name = "id", required = false) final Long id) {
		if (id == null) {
			model.addAttribute("supplier", new Supplier());
			model.addAttribute("formId", "createForm");
			model.addAttribute("formTitle", "Ny leverandør");
		} else {
			final Supplier supplier = supplierDao.findById(id)
					.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
			model.addAttribute("supplier", supplier);
			model.addAttribute("formId", "editForm");
			model.addAttribute("formTitle", "Rediger leverandør");
		}
		return "suppliers/form";
	}

	@Transactional
	@PostMapping("form")
	public String formPost(@ModelAttribute final Supplier supplier) {
		if (supplier.getId() != null) {
			final Supplier existingSupplier = supplierDao.findById(supplier.getId())
					.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
			existingSupplier.setStatus(supplier.getStatus());
			existingSupplier.setCvr(supplier.getCvr());
			existingSupplier.setZip(supplier.getZip());
			existingSupplier.setCity(supplier.getCity());
			existingSupplier.setAddress(supplier.getAddress());
			existingSupplier.setContact(supplier.getContact());
			existingSupplier.setPhone(supplier.getPhone());
			existingSupplier.setEmail(supplier.getEmail());
			existingSupplier.setCountry(supplier.getCountry());
			existingSupplier.setPersonalInfo(supplier.isPersonalInfo());
			existingSupplier.setDataProcessor(supplier.isDataProcessor());
			existingSupplier.setDescription(supplier.getDescription());
			supplierDao.save(existingSupplier);
		} else {
			supplierDao.save(supplier);
		}
		return "redirect:/suppliers";
	}

	@Transactional
	@PostMapping(value = "edit", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
	public String descriptionPost(@RequestParam("id") final String id, @RequestParam("description") final String description,
                                                                       @RequestParam("status") final SupplierStatus status,
                                                                       @RequestParam("zip") final String zip,
                                                                       @RequestParam("city") final String city,
                                                                       @RequestParam("address") final String address,
                                                                       @RequestParam("country") final String country,
                                                                       @RequestParam("cvr") final String cvr) {
		final Supplier supplier = supplierDao.findById(Long.valueOf(id))
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
		supplier.setDescription(description);
        supplier.setStatus(status);
        supplier.setCvr(cvr);
        supplier.setZip(zip);
        supplier.setCity(city);
        supplier.setAddress(address);
        supplier.setCountry(country);
		supplierDao.save(supplier);
		return "redirect:/suppliers/" + id;
	}

}
