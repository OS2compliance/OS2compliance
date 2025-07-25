package dk.digitalidentity.report.systemowneroverview;

import dk.digitalidentity.model.dto.StatusCombination;
import dk.digitalidentity.model.dto.enums.StatusColor;
import dk.digitalidentity.model.entity.Asset;
import dk.digitalidentity.model.entity.Document;
import dk.digitalidentity.model.entity.OrganisationUnit;
import dk.digitalidentity.model.entity.Register;
import dk.digitalidentity.model.entity.Relatable;
import dk.digitalidentity.model.entity.Tag;
import dk.digitalidentity.model.entity.Task;
import dk.digitalidentity.model.entity.ThreatAssessment;
import dk.digitalidentity.model.entity.enums.AssetStatus;
import dk.digitalidentity.model.entity.enums.DocumentStatus;
import dk.digitalidentity.model.entity.enums.RegisterStatus;
import dk.digitalidentity.model.entity.enums.RiskAssessment;
import dk.digitalidentity.report.systemowneroverview.dto.AssetRow;
import dk.digitalidentity.report.systemowneroverview.dto.DocumentRow;
import dk.digitalidentity.report.systemowneroverview.dto.RegisterRow;
import dk.digitalidentity.report.systemowneroverview.dto.TaskRow;
import dk.digitalidentity.report.systemowneroverview.dto.ThreatAssessmentRow;
import dk.digitalidentity.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class SystemOwnerOverviewService {
	private final TaskService taskService;

	public Map<String, Object> mapToModel(Map<Asset, Set<Relatable>> assetRelations, Set<Task> assetUnrelatedTasks, Set<Register> assetUnrelatedRegisters) {
		final Map<String, Object> model = new HashMap<>();
		Map<Task, Set<String>> assetNamesByTask = new HashMap<>();
		Map<Register, Set<String>> assetNamesByRegisters = new HashMap<>();

		Set<TaskRow> tasks =  new HashSet<>();
		Set<AssetRow> assets =  new HashSet<>();
		Set<RegisterRow> registers =  new HashSet<>();
		Set<DocumentRow> documents =  new HashSet<>();
		Set<ThreatAssessmentRow> threatAssessments = new HashSet<>();
		for (Map.Entry<Asset, Set<Relatable>> entry : assetRelations.entrySet()){
			Asset asset = entry.getKey();
			String assetName = asset.getName();

			for (Relatable assetRelatable : entry.getValue()){
				if (assetRelatable instanceof Task task) {
					if (assetNamesByTask.containsKey(task)) {
						assetNamesByTask.get(task).add(assetName);
					} else {
						assetNamesByTask.put(task, new HashSet<>());
						assetNamesByTask.get(task).add(assetName);
					}

				} else if (assetRelatable instanceof Register register){
					if (assetNamesByRegisters.containsKey(register)) {
						assetNamesByRegisters.get(register).add(assetName);
					} else {
						assetNamesByRegisters.put(register, new HashSet<>());
						assetNamesByRegisters.get(register).add(assetName);
					}
				}else if (assetRelatable instanceof Document document){
					documents.add(mapToRow(document, assetName));
				} else if( assetRelatable instanceof ThreatAssessment threatAssessment){
					threatAssessments.add(mapToRow(threatAssessment, assetName));
				}
			}

			assets.add(mapToRow(asset, threatAssessments));
		}

		// Only add unrelated tasks and registers if they are not already present
		for (Task task : assetUnrelatedTasks) {
			if (!assetNamesByTask.containsKey(task)) {
				assetNamesByTask.put(task, Set.of(""));
			}
		}
		for (Register register : assetUnrelatedRegisters) {
			if (!assetNamesByRegisters.containsKey(register)) {
				assetNamesByRegisters.put(register, Set.of(""));
			}
		}

		// Map tasks and registers
		for (Map.Entry<Task,Set<String>> entry : assetNamesByTask.entrySet()) {
			Task task = entry.getKey();
			for (String assetName : entry.getValue()){
				tasks.add(mapToRow(task, assetName));
			}
		}
		for (Map.Entry<Register,Set<String>> entry : assetNamesByRegisters.entrySet()) {
			Register register = entry.getKey();
			for (String assetName : entry.getValue()){
				registers.add(mapToRow(register, assetName));
			}
		}

		model.put("tasks", tasks);
		model.put("assets", assets);
		model.put("registers", registers);
		model.put("documents", documents);
		model.put("threatAssessments", threatAssessments);
		return model;
	}


	public TaskRow mapToRow(Task task, String assetName) {
		return new TaskRow(
				task.getName(),
				assetName,
				task.getTaskType() != null ? task.getTaskType().getMessage() : "",
				task.getResponsibleOu() != null ? task.getResponsibleOu().getName() : null,
				task.getNextDeadline(),
				task.getRepetition() != null ? task.getRepetition().getMessage() : "",
				taskService.calculateStatus(task),
				task.getTags().stream().map(Tag::getValue).collect(Collectors.joining(","))
		);
	}

	public DocumentRow mapToRow(Document document, String assetName) {
		DocumentStatus status = document.getStatus();
		StatusCombination statusCombination = new StatusCombination("", StatusColor.GREY);
		if (status != null) {
			StatusColor statusColor = switch (status) {
				case READY -> StatusColor.GREEN;
				case IN_PROGRESS -> StatusColor.YELLOW;
				default -> StatusColor.GREY;
			};
			statusCombination = new StatusCombination(status.getMessage(), statusColor);
		}

		return new DocumentRow(
				document.getName(),
				assetName,
				document.getDocumentType() != null ? document.getDocumentType().getMessage() : "",
				document.getNextRevision(),
				statusCombination,
				document.getTags().stream().map(Tag::getValue).collect(Collectors.joining(","))
		);
	}

	public RegisterRow mapToRow(Register register, String assetName) {
		RegisterStatus status = register.getStatus();
		StatusCombination statusCombination = new StatusCombination("", StatusColor.GREY);
		if (status != null) {
			StatusColor statusColor = switch (status) {
				case READY -> StatusColor.GREEN;
				case IN_PROGRESS -> StatusColor.YELLOW;
				default -> StatusColor.GREY;
			};
			statusCombination = new StatusCombination(status.getMessage(), statusColor);
		}

		RiskAssessment assessment = register.getConsequenceAssessment() != null ? register.getConsequenceAssessment().getAssessment() : null;
		StatusCombination assessmentCombination = new StatusCombination("", StatusColor.GREY);
		if (assessment != null) {
			StatusColor statusColor = switch (assessment) {
				case RED -> StatusColor.RED;
				case YELLOW -> StatusColor.YELLOW;
				case GREEN -> StatusColor.GREEN;
				case ORANGE -> StatusColor.ORANGE;
				case LIGHT_GREEN -> StatusColor.LIGHT_GREEN;
				default -> StatusColor.GREY;
			};
			assessmentCombination = new StatusCombination(assessment.getMessage(), statusColor);
		}

		return new RegisterRow(
				register.getName(),
				assetName,
				register.getResponsibleOus().stream().map(OrganisationUnit::getName).collect(Collectors.joining(",")),
				LocalDate.of(register.getUpdatedAt().getYear(), register.getUpdatedAt().getMonth(), register.getUpdatedAt().getDayOfMonth()),
				assessmentCombination,
				statusCombination
		);
	}

	public AssetRow mapToRow(Asset asset, Set<ThreatAssessmentRow> assetThreatAssessments) {
		AssetStatus status = asset.getAssetStatus();
		StatusCombination statusCombination = new StatusCombination("", StatusColor.GREY);
		if (status != null) {
			StatusColor statusColor = switch (status) {
				case READY -> StatusColor.GREEN;
				case ON_GOING -> StatusColor.YELLOW;
				case NOT_STARTED -> StatusColor.GREY;
				default -> StatusColor.GREY;
			};
			statusCombination = new StatusCombination(status.getMessage(), statusColor);
		}

		StatusCombination assessment = assetThreatAssessments.stream()
				.map(ThreatAssessmentRow::riskAssessment)
				.findFirst()
				.orElse(new StatusCombination("", StatusColor.GREY));


		return new AssetRow(
				asset.getName(),
				asset.getSupplier() != null ? asset.getSupplier().getName() : "",
				asset.getAssetType() != null ? asset.getAssetType().getCaption() : "",
				LocalDate.of(asset.getUpdatedAt().getYear(), asset.getUpdatedAt().getMonth(), asset.getUpdatedAt().getDayOfMonth()),
				assessment,
				statusCombination
		);
	}

	public ThreatAssessmentRow mapToRow(ThreatAssessment threatAssessment, String assetName) {
		RiskAssessment assessment = threatAssessment.getAssessment();
		StatusCombination assessmentCombination = new StatusCombination("", StatusColor.GREY);
		if (assessment != null) {
			StatusColor statusColor = switch (assessment) {
				case GREEN -> StatusColor.GREEN;
				case YELLOW -> StatusColor.YELLOW;
				case RED -> StatusColor.RED;
				case ORANGE -> StatusColor.ORANGE;
				case LIGHT_GREEN -> StatusColor.LIGHT_GREEN;
				default -> StatusColor.GREY;
			};
			assessmentCombination = new StatusCombination(assessment.getMessage(), statusColor);
		}


		return new ThreatAssessmentRow(
				threatAssessment.getName(),
				assetName,
				threatAssessment.getThreatAssessmentType() != null ? threatAssessment.getThreatAssessmentType().getMessage() : "",
				threatAssessment.getResponsibleOu() != null ? threatAssessment.getResponsibleOu().getName() : "",
				threatAssessment.getResponsibleUser() != null ? threatAssessment.getResponsibleUser().getName() : "",
				threatAssessment.getUpdatedAt(),
				threatAssessment.getThreatAssessmentReportApprovalStatus().getMessage(),
				assessmentCombination
		);
	}
}
