package dk.digitalidentity.controller.rest.Admin;

import dk.digitalidentity.model.entity.ChoiceList;
import dk.digitalidentity.model.entity.ChoiceValue;
import dk.digitalidentity.security.annotations.crud.RequireCreateAll;
import dk.digitalidentity.security.annotations.crud.RequireReadOwnerOnly;
import dk.digitalidentity.security.annotations.crud.RequireUpdateAll;
import dk.digitalidentity.security.annotations.sections.RequireAdmin;
import dk.digitalidentity.service.ChoiceService;
import dk.digitalidentity.service.ChoiceValueService;
import dk.digitalidentity.service.TaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@Slf4j
@RestController
@RequireAdmin
@RequiredArgsConstructor
@RequestMapping(value = "rest/choicelists/custom", consumes = "application/json", produces = "application/json")
public class CustomChoiceListRestController {

    private final ChoiceService choiceService;
	private final ChoiceValueService choiceValueService;
	private final TaskService taskService;

	// Request/Response DTOs
	public record CreateChoiceListRecord(String caption, String description) {}
	public record ChoiceValueResponse(boolean success, Long choiceListId, String error) {
		public static ChoiceValueResponse success(Long choiceListId) {
			return new ChoiceValueResponse(true, choiceListId, null);
		}

		public static ChoiceValueResponse error(String error) {
			return new ChoiceValueResponse(false, null, error);
		}
	}

	public record ChoiceValueDetailResponse(boolean success, Long id, String caption, String description, String identifier, String error) {
		public static ChoiceValueDetailResponse success(ChoiceValue choiceValue) {
			return new ChoiceValueDetailResponse(
					true,
					choiceValue.getId(),
					choiceValue.getCaption(),
					choiceValue.getDescription() != null ? choiceValue.getDescription() : "",
					choiceValue.getIdentifier(),
					null
			);
		}

		public static ChoiceValueDetailResponse error(String error) {
			return new ChoiceValueDetailResponse(false, null, null, null, null, error);
		}
	}

	@RequireCreateAll
	@PostMapping("/{choiceListId}/create")
	public ResponseEntity<ChoiceValueResponse> createChoiceValue(
			@PathVariable Long choiceListId,
			@RequestBody CreateChoiceListRecord createChoiceListRecord) {

		ChoiceList choiceList = choiceService.findChoiceList(choiceListId).orElse(null);
		if (choiceList == null) {
			return ResponseEntity.badRequest().body(ChoiceValueResponse.error("Could not find choiceList"));
		}

		// Generate unique identifier using timestamp
		long timestamp = System.currentTimeMillis();
		String identifier = choiceList.getIdentifier() + "-" + createChoiceListRecord.caption() + "-" + timestamp;

		ChoiceValue value = new ChoiceValue();
		value.setCaption(createChoiceListRecord.caption());
		value.setIdentifier(identifier);
		value.setDescription(createChoiceListRecord.description());
		value.setEditable(true);
		value = choiceValueService.save(value);
		choiceList.getValues().add(value);

		choiceService.save(choiceList);

		return ResponseEntity.ok(ChoiceValueResponse.success(choiceListId));
	}

	@RequireUpdateAll
	@PostMapping("/{choiceListId}/{choiceValueId}/edit")
	public ResponseEntity<ChoiceValueResponse> editChoiceValue(
			@PathVariable Long choiceListId,
			@PathVariable Long choiceValueId,
			@RequestBody CreateChoiceListRecord updateRecord) {

		ChoiceValue choiceValue = choiceValueService.findById(choiceValueId).orElse(null);
		if (choiceValue == null) {
			return ResponseEntity.badRequest().body(ChoiceValueResponse.error("Could not find choice value"));
		}

		choiceValue.setCaption(updateRecord.caption());
		if (!Objects.equals(choiceValue.getDescription(), updateRecord.description())) {
			taskService.updateDescriptions(choiceValue, updateRecord.description());
			choiceValue.setDescription(updateRecord.description());
		}

		choiceValueService.save(choiceValue);

		return ResponseEntity.ok(ChoiceValueResponse.success(choiceListId));
	}

	@RequireUpdateAll
	@PostMapping("/{choiceListId}/{choiceValueId}/delete")
	public ResponseEntity<ChoiceValueResponse> deleteChoiceValue(
			@PathVariable Long choiceListId,
			@PathVariable Long choiceValueId) {

		ChoiceList choiceList = choiceService.findChoiceList(choiceListId).orElse(null);
		if (choiceList == null) {
			return ResponseEntity.badRequest().body(ChoiceValueResponse.error("Could not find choiceList"));
		}


		ChoiceValue choiceValue = choiceList.getValues().stream()
				.filter(cv -> cv.getId().equals(choiceValueId))
				.findFirst()
				.orElse(null);

		if (choiceValue == null) {
			return ResponseEntity.badRequest().body(ChoiceValueResponse.error("Could not find choice value"));
		}

		choiceList.getValues().remove(choiceValue);
		choiceService.save(choiceList);

		choiceValueService.delete(choiceValue);

		return ResponseEntity.ok(ChoiceValueResponse.success(choiceListId));
	}

	@RequireReadOwnerOnly
	@GetMapping(value = "/choiceValue/{choiceValueId}", consumes = "*/*")
	public ResponseEntity<ChoiceValueDetailResponse> getChoiceValue(@PathVariable Long choiceValueId) {

		ChoiceValue choiceValue = choiceValueService.findById(choiceValueId).orElse(null);
		if (choiceValue == null) {
			return ResponseEntity.badRequest().body(ChoiceValueDetailResponse.error("Could not find choice value"));
		}

		return ResponseEntity.ok(ChoiceValueDetailResponse.success(choiceValue));
	}
}
