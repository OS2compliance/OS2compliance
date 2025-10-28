package dk.digitalidentity.controller.rest.Admin;

import dk.digitalidentity.model.entity.ChoiceList;
import dk.digitalidentity.model.entity.ChoiceValue;
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

import java.util.Map;
import java.util.Objects;
import java.util.Random;

@Slf4j
@RestController
@RequireAdmin
@RequiredArgsConstructor
@RequestMapping(value = "rest/choicelists/custom", consumes = "application/json", produces = "application/json")
public class CustomChoiceListRestController {

    private final ChoiceService choiceService;
	private final ChoiceValueService choiceValueService;
	private final TaskService taskService;

	public record CreateChoiceListRecord(String caption, String description) {}

	@RequireUpdateAll
	@PostMapping("/{choiceListId}/create")
	public ResponseEntity<Map<String, Object>> createChoiceValue(
			@PathVariable Long choiceListId,
			@RequestBody CreateChoiceListRecord createChoiceListRecord) {

		ChoiceList choiceList = choiceService.findChoiceList(choiceListId).orElse(null);
		if (choiceList == null) {
			return ResponseEntity.badRequest()
					.body(Map.of("success", false, "error", "Could not find choiceList"));
		}

		Random random = new Random();
		int randomNumber = 100000 + random.nextInt(900000);

		ChoiceValue value = new ChoiceValue();
		value.setCaption(createChoiceListRecord.caption());
		String identifier = choiceList.getIdentifier() + "-" + createChoiceListRecord.caption() + "-" + randomNumber;

		if (choiceValueService.findByIdentifier(identifier) != null) {
			return ResponseEntity.badRequest()
					.body(Map.of("success", false, "error", "Duplicate identifier"));
		}

		value.setIdentifier(identifier);
		value.setDescription(createChoiceListRecord.description());
		value.setEditable(true);
		value = choiceValueService.save(value);
		choiceList.getValues().add(value);

		choiceService.save(choiceList);

		return ResponseEntity.ok(Map.of("success", true, "choiceListId", choiceListId));
	}

	@RequireUpdateAll
	@PostMapping("/{choiceListId}/{choiceValueId}/edit")
	public ResponseEntity<Map<String, Object>> editChoiceValue(
			@PathVariable Long choiceListId,
			@PathVariable Long choiceValueId,
			@RequestBody CreateChoiceListRecord updateRecord) {

		ChoiceValue choiceValue = choiceValueService.findById(choiceValueId).orElse(null);
		if (choiceValue == null) {
			return ResponseEntity.badRequest()
					.body(Map.of("success", false, "error", "Could not find choice value"));
		}

		choiceValue.setCaption(updateRecord.caption());
		if (!Objects.equals(choiceValue.getDescription(), updateRecord.description())) {
			taskService.updateDescriptions(choiceValue, updateRecord.description);
		}
		choiceValue.setDescription(updateRecord.description());

		choiceValueService.save(choiceValue);

		return ResponseEntity.ok(Map.of("success", true, "choiceListId", choiceListId));
	}

	@RequireUpdateAll
	@PostMapping("/{choiceListId}/{choiceValueId}/delete")
	public ResponseEntity<Map<String, Object>> deleteChoiceValue(
			@PathVariable Long choiceListId,
			@PathVariable Long choiceValueId) {

		ChoiceList choiceList = choiceService.findChoiceList(choiceListId).orElse(null);
		if (choiceList == null) {
			return ResponseEntity.badRequest()
					.body(Map.of("success", false, "error", "Could not find choiceList"));
		}

		ChoiceValue choiceValue = choiceValueService.findById(choiceValueId).orElse(null);
		if (choiceValue == null) {
			return ResponseEntity.badRequest()
					.body(Map.of("success", false, "error", "Could not find choice value"));
		}

		choiceList.getValues().remove(choiceValue);
		choiceService.save(choiceList);

		choiceValueService.delete(choiceValue);

		return ResponseEntity.ok(Map.of("success", true, "choiceListId", choiceListId));
	}

	@GetMapping(value = "/choiceValue/{choiceValueId}", consumes = "*/*")
	public ResponseEntity<Map<String, Object>> getChoiceValue(@PathVariable Long choiceValueId) {

		ChoiceValue choiceValue = choiceValueService.findById(choiceValueId).orElse(null);
		if (choiceValue == null) {
			return ResponseEntity.badRequest()
					.body(Map.of("success", false, "error", "Could not find choice value"));
		}

		return ResponseEntity.ok(Map.of(
				"success", true,
				"id", choiceValue.getId(),
				"caption", choiceValue.getCaption(),
				"description", choiceValue.getDescription() != null ? choiceValue.getDescription() : "",
				"identifier", choiceValue.getIdentifier()
		));
	}
}
