package dk.digitalidentity.mapping;

import dk.digitalidentity.model.entity.Task;
import dk.digitalidentity.model.entity.enums.TaskRepetition;
import dk.digitalidentity.model.entity.enums.TaskType;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class TaskMapperExportTest {
	private final TaskMapper mapper = Mappers.getMapper(TaskMapper.class);

	@Test
	public void exportShowsTheSameStatusAsTheFilter() {
		assertThat(mapper.toDTOForExport(overdueTask(true), Map.of()).getStatus()).isEqualTo("I gang");
		assertThat(mapper.toDTOForExport(overdueTask(false), Map.of()).getStatus()).isEqualTo("Overskredet");
	}

	private Task overdueTask(final boolean inProgress) {
		final Task task = new Task();
		task.setTaskType(TaskType.TASK);
		task.setRepetition(TaskRepetition.NONE);
		task.setNextDeadline(LocalDate.now().minusDays(1));
		task.setInProgress(inProgress);
		return task;
	}
}
